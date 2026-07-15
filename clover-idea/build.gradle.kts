import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    java
    id("org.jetbrains.intellij.platform")
}

group = "org.openclover.idea"
version = providers.gradleProperty("cloverVersion").getOrElse("5.0.0-SNAPSHOT")

val ideaType = providers.gradleProperty("ideaType").getOrElse("IU")
val ideaVersion = providers.gradleProperty("ideaVersion").getOrElse("2025.3.6")

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

// version-filtered generated Java source (PluginVersionInfo.java)
val generateVersionSource = tasks.register<Copy>("generateVersionSource") {
    group = "build"
    description = "Generates PluginVersionInfo.java with the current plugin version substituted in."
    from("src/main/resources-filtered") { include("**/*.java") }
    into(layout.buildDirectory.dir("generated/java"))
    filter { line -> line.replace("\${project.version}", version.toString()) }
}

sourceSets {
    main {
        java.srcDir(generateVersionSource)
        // plugin.xml lives under etc/META-INF
        resources.srcDir("etc")
    }
    // Classes loaded directly into IntelliJ's external JPS build process (CloverSerializerExtension,
    // CloverJavaBuilder, and the config/util classes they need), plus their transitive JPS-safe
    // dependencies. Kept in a separate source set — compiled to Java 11 bytecode with an isolated
    // classpath — because JPS runs the *project being built*'s own build process, on whatever JDK
    // that project needs (observed as low as Java 11 in practice), NOT the JDK IntelliJ itself runs
    // on. This mirrors how IntelliJ's own JPS extension modules (plugins/maven/jps,
    // plugins/gradle/jps-plugin, etc.) are built: LANGUAGE_LEVEL="JDK_11", no dependency on the full
    // Platform SDK (which is bytecode 65 / Java 21 and unreadable by an older JPS host JVM).
    create("jps") {
        java.srcDir("src/jps/java")
    }
}

dependencies {
    intellijPlatform {
        create(ideaType, ideaVersion)
        // The Java plugin is required both at compile time (StdFileTypes.JAVA, java PSI in
        // main code) and at test time (JavaPsiTestCase). Loaded as a real bundled plugin via
        // the platform's plugin classloader — not flat on the app classpath.
        bundledPlugin("com.intellij.java")
        // Base platform test framework (HeavyPlatformTestCase, LightPlatformTestCase,
        // LightIdeaTestCase, UsefulTestCase, PsiTestUtil).
        testFramework(TestFrameworkType.Platform)
        // Java test framework (JavaPsiTestCase) — layered on top of Platform.
        testFramework(TestFrameworkType.Plugin.Java)
    }

    // clover core + jtreemap are the ONLY libraries bundled into the plugin jar (matching the old
    // Maven assembly: it packed exactly org.openclover:clover + net.sf.jtreemap:jtreemap).
    // isTransitive=false is deliberate: org.openclover:clover is a self-contained shaded jar (it
    // bundles antlr, cajo, fastutil, etc.), so pulling transitives would both
    // bloat the plugin (clover-ant/clover-groovy/asm/ant/...) and re-introduce the old fastutil 4.4.3
    // and jdom 1.0 that conflict with IDEA's bundled copies
    implementation("org.openclover:clover:$version") { isTransitive = false }
    implementation("net.sf.jtreemap:jtreemap:1.1.3") { isTransitive = false }

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.18.0")

    // util-8.jar (bytecode 52, also bundles org.jdom.* used by the JPS model API), jps-model.jar,
    // intellij.platform.jps.build.jar (JPS builder/incremental-compiler API), annotations.jar,
    // intellij.libraries.commons.lang3.jar, and java-impl.jar (only used for its bytecode-52
    // org.jetbrains.jps.builders.java.JavaSourceTransformer) are the only pieces of the platform
    // the 'jps' source set is allowed to see.
    // Sourced from 'compileClasspath' before main's own compileClasspath is extended with the
    // jps source set's output below, to avoid a jps -> main -> jps classpath cycle.
    "jpsCompileOnly"(files(provider {
        configurations["compileClasspath"].filter { jar ->
            jar.name.matches(Regex("(util-8|jps-model|intellij\\.platform\\.jps\\.build|annotations|intellij\\.libraries\\.commons\\.lang3|java-impl)(-.*)?\\.jar"))
        }
    }))
    "jpsImplementation"("org.openclover:clover:$version") { isTransitive = false }
}

// main needs the JPS-loadable serializer/config classes too (e.g. UI panels read CloverModuleConfig).
// Wired directly onto the sourceSet's compile/runtime classpath (not via the 'implementation'
// configuration) so 'configurations["compileClasspath"]', used above to build the jps source set's
// own isolated classpath, doesn't end up including the jps source set's output — that would be a
// circular classpath dependency.
sourceSets.main {
    compileClasspath += sourceSets["jps"].output
    runtimeClasspath += sourceSets["jps"].output
}

// 'test' doesn't inherit compileClasspath/runtimeClasspath additions made directly on the sourceSet
// object above (only main.output, not main.compileClasspath, flows into it by default), so it needs
// the same jps output added explicitly.
sourceSets.test {
    compileClasspath += sourceSets["jps"].output
    runtimeClasspath += sourceSets["jps"].output
}

// The JPS host JVM can be as old as Java 8/11 (see the 'jps' source set comment above), so its
// classes are cross-compiled to Java 11 bytecode regardless of the project's Java 21 toolchain.
tasks.named<JavaCompile>("compileJpsJava") {
    options.release.set(11)
}

// version-substitute plugin.xml (<version>idea-${project.version}</version>).
tasks.processResources {
    filesMatching("META-INF/plugin.xml") {
        filter { line -> line.replace("\${project.version}", version.toString()) }
    }
}

// Fat plugin jar: clover-idea classes + clover.jar + jtreemap.jar
// Produced at build/dist/clover-idea-<version>.jar and uploaded by the 'D-release-openclover'
val fatJar = tasks.register<Jar>("fatJar") {
    group = "build"
    description = "Assembles the self-contained clover-idea plugin jar (plugin classes + clover.jar + jtreemap.jar)."
    archiveFileName.set("clover-idea-$version.jar")
    destinationDirectory.set(layout.buildDirectory.dir("dist"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    val composed = tasks.named("composedJar")
    dependsOn(composed)
    // clover-idea's own classes + patched META-INF/plugin.xml + services + PluginVersionInfo
    from({ zipTree(composed.get().outputs.files.singleFile) })
    // JPS-loadable classes (see the 'jps' source set above) — compiled separately, so composedJar
    // doesn't already contain them; must be merged into the same jar since the JPS build process
    // loads this exact plugin jar off its classpath.
    from({ sourceSets["jps"].output })
    // clover.jar + jtreemap.jar contents (self-contained; non-transitive)
    from({
        configurations.runtimeClasspath.get().files
            .filter { it.name.startsWith("clover-") || it.name.startsWith("jtreemap-") }
            .map { zipTree(it) }
    })
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

// version-filtered autoupdate descriptor for the openclover.org web server
val generateAutoUpdate = tasks.register<Copy>("generateAutoUpdate") {
    group = "build"
    description = "Generates the version-filtered autoupdate descriptor for the openclover.org web server."
    from("src/main/resources-filtered/autoupdate")
    into(layout.buildDirectory.dir("autoupdate"))
    filter { line -> line.replace("\${project.version}", version.toString()) }
}

// Aggregate the release artifacts (invoked from the Maven wrapper's package/install phases).
val mavenArtifacts = tasks.register("mavenArtifacts") {
    group = "build"
    description = "Aggregates the release artifacts (fat plugin jar + autoupdate descriptor) for the Maven package/install phases."
    dependsOn(fatJar, generateAutoUpdate)
}

intellijPlatform {
    pluginConfiguration {
        // Minimum supported IDEA is 2024.2 (build 242, first release on Java 21); do not narrow
        // until-build so the same artifact loads across 2024/2025/2026.
        ideaVersion {
            sinceBuild = "242"
            untilBuild = provider { null }
        }
    }
    // These tests are unit/heavy platform tests; no need for the plugin verifier here.
    buildSearchableOptions = false
}

// testproject coverage fixture (build/testproject/clover/coverage.db)
val testProjectTool: Configuration = configurations.create("testProjectTool")
dependencies {
    testProjectTool("org.openclover:clover:$version") {
        exclude(group = "it.unimi.dsi", module = "fastutil")
    }
    testProjectTool("junit:junit:4.13.2")
}

val testProjectBase = layout.projectDirectory.dir("src/test/resources/testproject")
val testProjectOut = layout.buildDirectory.dir("testproject")
val testProjectInstr = layout.buildDirectory.dir("testproject/instr")
val testProjectClasses = layout.buildDirectory.dir("testproject/classes")
val testProjectDb = layout.buildDirectory.file("testproject/clover/coverage.db")

// 1) instrument the sample sources with clover
val instrumentTestProject = tasks.register<JavaExec>("instrumentTestProject") {
    group = "verification"
    description = "Instruments the sample test project sources with OpenClover (test coverage fixture)."
    inputs.dir(testProjectBase)
    outputs.dir(testProjectInstr)
    outputs.file(testProjectDb)
    classpath = testProjectTool
    mainClass.set("org.openclover.core.CloverInstr")
    doFirst {
        delete(testProjectInstr, testProjectOut.get().dir("clover"))
        testProjectDb.get().asFile.parentFile.mkdirs()
        args(
            "-s", testProjectBase.asFile.absolutePath,
            "-d", testProjectInstr.get().asFile.absolutePath,
            "-i", testProjectDb.get().asFile.absolutePath,
        )
    }
}

// 2) compile the instrumented sources
val compileTestProject = tasks.register<Exec>("compileTestProject") {
    group = "verification"
    description = "Compiles the Clover-instrumented sample test project sources (test coverage fixture)."
    dependsOn(instrumentTestProject)
    inputs.dir(testProjectInstr)
    outputs.dir(testProjectClasses)
    doFirst {
        val classes = testProjectClasses.get().asFile
        delete(classes); classes.mkdirs()
        val javac = File(System.getProperty("java.home"), "bin/javac").absolutePath
        val srcs = fileTree(testProjectInstr) { include("**/*.java") }.files.map { it.absolutePath }
        commandLine(listOf(javac, "-d", classes.absolutePath, "-cp", testProjectTool.asPath) + srcs)
    }
}

// 3) run the sample tests to record coverage into coverage.db (one test is intentionally failing)
val buildTestProject = tasks.register<JavaExec>("buildTestProject") {
    group = "verification"
    description = "Runs the sample test project to record coverage into coverage.db (test fixture)."
    dependsOn(compileTestProject)
    outputs.dir(testProjectOut)
    isIgnoreExitValue = true
    mainClass.set("org.junit.runner.JUnitCore")
    doFirst {
        val classes = testProjectClasses.get().asFile
        classpath = files(classes) + testProjectTool
        val names = fileTree(classes) { include("**/*Test.class"); exclude("**/*\$*.class") }
            .files.map { it.relativeTo(classes).path.removeSuffix(".class").replace(File.separatorChar, '.') }
        args(names)
    }
}

sourceSets.test {
    // expose build/testproject as a classpath root so tests can load /clover/coverage.db
    runtimeClasspath += files(layout.buildDirectory.dir("testproject"))
}

tasks.test {
    dependsOn(buildTestProject)
    // The IntelliJ Platform Gradle Plugin builds its own runtime classpath for this task rather than
    // using sourceSets.test.runtimeClasspath directly, so the jps source set's output (added to that
    // sourceSet property above) doesn't reach the test JVM unless added here too.
    classpath += files(sourceSets["jps"].output)
    // JetBrains platform tests need a large heap.
    maxHeapSize = "1g"
    // IdeaVersionVerificationIdeaTest compares ApplicationInfo major.minor against this.
    systemProperty("cij.idea.expected.version", ideaVersion)
    // Byte Buddy flag for Java 25 (used by IDEA 2026 runtime)
    systemProperty("net.bytebuddy.experimental", "true")
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = false
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showCauses = true
        showStackTraces = true
    }
}
