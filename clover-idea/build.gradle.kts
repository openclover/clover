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
}

// version-filtered generated Java source (PluginVersionInfo.java)
val generateVersionSource by tasks.registering(Copy::class) {
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
}

// version-substitute plugin.xml (<version>idea-${project.version}</version>).
tasks.processResources {
    filesMatching("META-INF/plugin.xml") {
        filter { line -> line.replace("\${project.version}", version.toString()) }
    }
}

// Fat plugin jar: clover-idea classes + clover.jar + jtreemap.jar
// Produced at build/dist/clover-idea-<version>.jar and uploaded by the 'D-release-openclover'
val fatJar by tasks.registering(Jar::class) {
    archiveFileName.set("clover-idea-$version.jar")
    destinationDirectory.set(layout.buildDirectory.dir("dist"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    val composed = tasks.named("composedJar")
    dependsOn(composed)
    // clover-idea's own classes + patched META-INF/plugin.xml + services + PluginVersionInfo
    from({ zipTree(composed.get().outputs.files.singleFile) })
    // clover.jar + jtreemap.jar contents (self-contained; non-transitive)
    from({
        configurations.runtimeClasspath.get().files
            .filter { it.name.startsWith("clover-") || it.name.startsWith("jtreemap-") }
            .map { zipTree(it) }
    })
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

// version-filtered autoupdate descriptor for the openclover.org web server
val generateAutoUpdate by tasks.registering(Copy::class) {
    from("src/main/resources-filtered/autoupdate")
    into(layout.buildDirectory.dir("autoupdate"))
    filter { line -> line.replace("\${project.version}", version.toString()) }
}

// Aggregate the release artifacts (invoked from the Maven wrapper's package/install phases).
val mavenArtifacts by tasks.registering {
    dependsOn(fatJar, generateAutoUpdate)
}

intellijPlatform {
    pluginConfiguration {
        // Keep the existing broad compatibility; do not let the plugin narrow until-build so
        // the same artifact loads across 2024/2025/2026.
        ideaVersion {
            sinceBuild = "139"
            untilBuild = provider { null }
        }
    }
    // These tests are unit/heavy platform tests; no need for the plugin verifier here.
    buildSearchableOptions = false
}

// testproject coverage fixture (build/testproject/clover/coverage.db)
val testProjectTool: Configuration by configurations.creating
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
val instrumentTestProject by tasks.registering(JavaExec::class) {
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
val compileTestProject by tasks.registering(Exec::class) {
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
val buildTestProject by tasks.registering(JavaExec::class) {
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
