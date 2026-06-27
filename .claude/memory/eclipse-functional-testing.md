# Eclipse Functional Test Infrastructure — Implementation Plan

## Goal

Automate end-to-end testing of the `clover-eclipse` plugin by installing it into a real Eclipse runtime and running the existing test projects through the full instrumentation/build/coverage cycle. No Tycho migration; stays within the existing plain-Maven build.

---

## `clover-eclipse/workspace/` — DELETE IT

The `workspace/` directory contains two stub project descriptors (`com.cenqua.clover.core`, `com.cenqua.clover.testoptimization`) left over from Atlassian's internal development environment circa 2017. Evidence:
- **Zero Java source files** — just `.project` and `.classpath` files
- **Hardcoded absolute paths** like `/home/sginter/clover8/core/src` in `.classpath`
- **Renamed project names** — `.project` names already updated to `org.openclover.*` but the directory is still `com.cenqua.*`
- **No references** to these from any other file in the repo
- Last meaningful touch: initial commit (2017)

Each test run will use a fresh, empty workspace created by the runner. Delete `clover-eclipse/workspace/` entirely.

---

## New Modules

### `clover-eclipse/org.openclover.eclipse.functest.runner`

An OSGi plugin that implements Eclipse's `IApplication` interface. Installed into the test Eclipse instance as a dropin. When invoked, it:
1. Sets the `CLOVER_RUNTIME` classpath variable
2. Imports test projects into the headless workspace
3. Triggers a full workspace build (activating Clover's pre/post builders)
4. Runs JUnit 4 tests for projects that have them (subprocess)
5. Loads and asserts the Clover coverage database via `CloverDatabase.loadWithCoverage()`
6. Writes per-project results as JUnit XML to `target/surefire-reports/`
7. Exits with 0 (all pass) or 1 (any failure)

### `clover-eclipse/org.openclover.eclipse.functest`

Maven orchestrator module. Active only on Linux via an explicit profile `eclipse-functest`. Responsible for:
1. Downloading and extracting Eclipse tar.gz
2. Installing the clover-eclipse update site via p2 director
3. Dropping `functest.runner` JAR into `eclipse/dropins/`
4. Launching Eclipse headlessly via `xvfb-run`
5. Failing the Maven build if Eclipse exits non-zero

---

## Module Layout

```
clover-eclipse/
  org.openclover.eclipse.functest.runner/
    pom.xml
    src/main/java/org/openclover/eclipse/functest/runner/
      Application.java           ← IApplication entry point
      WorkspaceManager.java      ← import projects, set CLOVER_RUNTIME
      BuildVerifier.java         ← check IMarkers, .class files, .clover/clover.db
      TestRunner.java            ← launch JVM subprocess with JUnit 4 runner
      CoverageVerifier.java      ← load DB via CloverDatabase.loadWithCoverage()
      SurefireReporter.java      ← write JUnit XML to target/surefire-reports/
      TestResult.java            ← value object: project name, pass/fail, messages, timing
    src/main/resources/
      META-INF/MANIFEST.MF
      plugin.xml

  org.openclover.eclipse.functest/
    pom.xml                      ← download + install + run + verify
    src/test/resources/
      projects/                  ← moved from clover-eclipse/tests/projects/
        TestAllUnitTests/
        TestAllUnitTestsExternal/
        TestAntBuild/
        TestAppAndSourceFolders/
        TestCoverageTypes/
        TestDependenciesA/
        TestDependenciesB/
        TestDependenciesC/
        TestDynamicWebProject/
        TestEncodingsProject/
        TestEquinoxProject/
        TestEquinoxTestsProject/
        TestExcludesProject/
        TestJava13Project/
        TestJava14Project/
        TestJava15Project/
        TestMultipleSourceFolders/
        TestRelativeInitstring/
        TestRootSourceFolder/
        TestSeparateInstrOutputFolder/
```

Note: `clover-eclipse/workspace/` is deleted (see above). `clover-eclipse/tests/` becomes the `projects/` resource above; the `tests/` directory itself is removed.

---

## JUnit Version Upgrade: 3 → 4

### Current state

All test source files use `junit.framework.TestCase` (JUnit 3 API) and the `.classpath` files declare `org.eclipse.jdt.junit.JUNIT_CONTAINER/3`. These are old fixtures from 2017 or earlier.

### Why upgrade to JUnit 4

- JUnit 3 (`TestCase` subclassing, method-name-based discovery) is obsolete but still functional
- `JUNIT_CONTAINER/3` resolves to `junit-3.8.2.jar` in Eclipse — a very old JAR
- The subprocess test runner is easier to drive with JUnit 4's `JUnitCore` runner class
- More realistic test projects (reflect actual Java development today)
- JUnit 4 is backward-compatible: existing `extends TestCase` + `testXxx()` method-name tests **continue to work without code changes** under JUnit 4's runner (it detects `TestCase` subclasses)

### Upgrade plan

**In every `.classpath` that has `JUNIT_CONTAINER/3`**, replace:
```xml
<classpathentry kind="con" path="org.eclipse.jdt.junit.JUNIT_CONTAINER/3"/>
```
with:
```xml
<classpathentry kind="con" path="org.eclipse.jdt.junit.JUNIT_CONTAINER/4"/>
```

**Source code**: keep as-is for now. `TestCase` subclasses compile and run under JUnit 4 without modification. Optionally add `@Test` annotations + remove `extends TestCase` in a separate cleanup pass.

**`TestAntBuild`**: uses `lib/junit-3.8.2.jar` directly in classpath rather than a container. Replace the JAR with `junit-4.13.2.jar` (or depend on the JUnit 4 container instead).

**`.launch` files**: `TEST_KIND` attribute value changes from `org.eclipse.jdt.junit.loader.junit3` to `org.eclipse.jdt.junit.loader.junit4` in all `.launch` files.

### Why not JUnit 5

JUnit 5 requires the vintage engine for backward-compat with JUnit 3/4, the Platform launcher, and Gradle/Maven-specific integrations. The Eclipse JUnit 5 container (`JUNIT_CONTAINER/5`) adds complexity without benefit for these fixture projects. JUnit 4 is the right stopping point.

---

## Eclipse Version Parameterization

### Problem
`clover-eclipse-libs` version (`2026.03`) is hardcoded in every plugin's `pom.xml`. The download file/path in `clover-eclipse-libs/pom.xml` is also hardcoded for Windows.

### Solution

**In `clover-eclipse/pom.xml`**, add two properties:
```xml
<!-- Eclipse version in release-label format (dash separator) — used in download URLs -->
<eclipse.version>2026-03</eclipse.version>
<!-- Eclipse libs Maven artifact version (dot separator) — must match clover-eclipse-libs/pom.xml <version> -->
<eclipse.libs.version>2026.03</eclipse.libs.version>
```

**In every `org.openclover.eclipse.*` plugin pom.xml**, change:
```xml
<!-- FROM: -->
<version>2026.03</version>
<!-- TO: -->
<version>${eclipse.libs.version}</version>
```
for the `clover-eclipse-libs` dependency.

**Note:** `clover-eclipse-libs/pom.xml` itself still has a hardcoded `<version>2026.03</version>` — Maven does not allow property substitution in the artifact's own `<version>`. When bumping Eclipse versions, update `eclipse.version`, `eclipse.libs.version`, AND `clover-eclipse-libs/pom.xml <version>` together.

**To switch Eclipse version** for matrix testing:
```
mvn install -Declipse.version=2025-06 -Declipse.libs.version=2025.06
```

---

## Eclipse Download — Platform Logic

### URL pattern

| Component | Format | Example |
|---|---|---|
| Download path | `technology/epp/downloads/release/YYYY-MM/R` | `technology/epp/downloads/release/2026-03/R` |
| Linux file | `eclipse-java-YYYY-MM-R-linux-gtk-x86_64.tar.gz` | `eclipse-java-2026-03-R-linux-gtk-x86_64.tar.gz` |
| Windows file | `eclipse-java-YYYY-MM-R-win32-x86_64.zip` | (workspace-setup only, not functest) |

### Download site by version

| Eclipse version | Download site |
|---|---|
| 2023-06 | `https://archive.eclipse.org` |
| 2024-06 | `https://archive.eclipse.org` |
| 2025-06 | `https://ftp.fau.de/eclipse` |
| 2026-03 | `https://ftp.fau.de/eclipse` |

Mirror availability: versions older than ~2 years are reliably on `archive.eclipse.org`. Current/recent versions are on active mirrors. The CI matrix (see below) overrides `eclipse.download.site` per version.

### Maven properties in `org.openclover.eclipse.functest/pom.xml`
```xml
<properties>
  <eclipse.download.path>technology/epp/downloads/release/${eclipse.version}/R</eclipse.download.path>
  <eclipse.download.file>eclipse-java-${eclipse.version}-R-linux-gtk-x86_64.tar.gz</eclipse.download.file>
  <!-- default site for current version; override per matrix entry -->
  <eclipse.download.site>https://ftp.fau.de/eclipse</eclipse.download.site>
</properties>
```

### Extraction
Use `maven-antrun-plugin` with Ant's `<untar compression="gzip">` task. Extract the full `eclipse/` directory tree (not just JARs). Target: `${project.build.directory}/eclipse-install/`.

---

## `org.openclover.eclipse.functest/pom.xml` — Execution Flow

Bound to Maven's `integration-test` phase, inside the `eclipse-functest` profile:

```
Phase: pre-integration-test
  Step 1: Download eclipse-java-${eclipse.version}-R-linux-gtk-x86_64.tar.gz
          (skip if cached)
  Step 2: Extract to target/eclipse-install/eclipse/
  Step 3: Install clover-eclipse update site via p2 director:
            eclipse -nosplash
                    -application org.eclipse.equinox.p2.director
                    -repository file://${clover-eclipse.updatesite.dir}
                    -installIU org.openclover.eclipse.feature.feature.group
                    -destination target/eclipse-install/eclipse
  Step 4: Copy functest.runner JAR into eclipse/dropins/

Phase: integration-test
  Step 5: xvfb-run eclipse
                 -nosplash
                 -application org.openclover.eclipse.functest.runner.application
                 -data target/functest-workspace
                 -projectsDir ${project.basedir}/src/test/resources/projects
                 -cloverRuntime ${clover-runtime.jar.path}
                 -reportsDir ${project.build.directory}/surefire-reports
          → exit code != 0 fails the Maven build

Phase: post-integration-test
  Step 6: Logs archived:
            target/functest-workspace/.metadata/.log
```

The update site directory is the expanded contents of `org.openclover.eclipse.updatesite/target/classes/` (already contains `features/`, `plugins/`, `site.xml`).

---

## `org.openclover.eclipse.functest.runner` — Plugin Details

### `META-INF/MANIFEST.MF`
```
Bundle-SymbolicName: org.openclover.eclipse.functest.runner; singleton:=true
Bundle-Version: 5.0.0
Require-Bundle: org.eclipse.core.runtime,
 org.eclipse.core.resources,
 org.eclipse.jdt.core,
 org.eclipse.jdt.launching,
 org.openclover.eclipse.core
Eclipse-LazyStart: false
```

No `Bundle-Activator` needed — the application entry point is the `IApplication` registered in `plugin.xml`.

### `plugin.xml`
```xml
<extension point="org.eclipse.core.runtime.applications"
           id="application"
           name="OpenClover Eclipse Functional Test Runner">
  <application>
    <run class="org.openclover.eclipse.functest.runner.Application"/>
  </application>
</extension>
```

### `Application.java` — Key Logic

```java
public class Application implements IApplication {
    public Object start(IApplicationContext context) throws Exception {
        // Parse: -projectsDir, -cloverRuntime, -reportsDir from context.getArguments()
        Map args = context.getArguments();
        File projectsDir = ...;
        String cloverRuntime = ...;
        File reportsDir = ...;
        reportsDir.mkdirs();

        WorkspaceManager wm = new WorkspaceManager(projectsDir, cloverRuntime);
        wm.setCloverRuntimeVariable();
        wm.importProjects();
        wm.buildAll();

        List<TestResult> results = new ArrayList<>();
        for (IProject project : wm.getProjects()) {
            long start = System.currentTimeMillis();
            TestResult r = new TestResult(project.getName());

            BuildVerifier.verify(project, r);
            if (!r.hasBuildErrors() && hasUnitTests(project)) {
                TestRunner.run(project, cloverRuntime, wm.getEclipseInstallDir(), r);
                wm.refresh(project);
                CoverageVerifier.verify(project, r);
            } else if (!r.hasBuildErrors()) {
                CoverageVerifier.verifyDbOnly(project, r);
            }

            r.setDurationMs(System.currentTimeMillis() - start);
            results.add(r);
        }

        SurefireReporter.write(results, reportsDir, eclipseVersion);
        printSummary(results);
        return results.stream().anyMatch(TestResult::hasFailed) ? EXIT_ERROR : EXIT_OK;
    }
}
```

### `WorkspaceManager.java`
- `setCloverRuntimeVariable()`: `JavaCore.setClasspathVariable("CLOVER_RUNTIME", new Path(cloverRuntime), null)` then join the resulting classpath init job before opening projects.
- `importProjects()`: Sorted import to respect dependencies:
  1. `TestAllUnitTestsExternal` before `TestAllUnitTests`
  2. `TestDependenciesA` → `B` → `C`
  3. Others in any order
  
  Per project: `workspace.loadProjectDescription(new Path(dir+"/.project"))` → `desc.setLocation(...)` → `project.create(desc, null)` → `project.open(null)`.
- `buildAll()`: `workspace.build(IncrementalProjectBuilder.FULL_BUILD, null)` then `Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null)` and `join(ResourcesPlugin.FAMILY_MANUAL_BUILD, null)`.

### `BuildVerifier.java`
After build, checks:
1. No `IMarker.SEVERITY_ERROR` markers on the project (`project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE)`)
2. `project.getFolder(".clover").getFile("clover.db").exists()` — Clover database created
3. Output folder (`bin/` by default, or per `.classpath` `kind="output"`) contains at least one `.class` file

### `TestRunner.java`
For projects with `JUNIT_CONTAINER` in `.classpath`:
```java
// Locate JUnit 4 JAR from eclipse/plugins/org.junit_4*.jar
File junitJar = findJUnitJar(eclipseInstallDir);
// Build classpath: output dir + clover-runtime + junit jar
String cp = outputDir + pathSep + cloverRuntime + pathSep + junitJar;
ProcessBuilder pb = new ProcessBuilder(
    javaExe, "-cp", cp,
    "org.junit.runner.JUnitCore",
    discoverTestClasses(project)  // scan bin/ for *Test.class / Test*.class
);
pb.environment().put("CLOVER_INITSTRING", cloverDbPath);
pb.redirectErrorStream(true);
Process proc = pb.start();
// Capture output for failure message
String output = new String(proc.getInputStream().readAllBytes());
int exit = proc.waitFor();
if (exit != 0) r.fail("JUnit runner exited " + exit + ":\n" + output);
```

### `CoverageVerifier.java`
```java
// After build only (no tests run):
String initString = project.getLocation().append(".clover/clover.db").toOSString();
CloverDatabase db = CloverDatabase.loadWithCoverage(initString, new CoverageDataSpec());
r.assertNotNull(db, "CloverDatabase is null");
r.assertFalse(db.getRegistry().getProject()
    .getPackages(new HasMetrics.Everything()).isEmpty(), "No packages in registry");

// Additional check after test run:
r.assertNotNull(db.getCoverage(), "No coverage data");
r.assertTrue(db.getCoverage().getProject()
    .getMetrics().getNumCoveredElements() > 0, "Zero covered elements");
```

### `SurefireReporter.java`

Writes one XML file per project in the standard Maven Surefire format, named `TEST-eclipse-${eclipseVersion}-${projectName}.xml`. The `eclipseVersion` prefix ensures that when a CI matrix runs multiple versions and uploads to the same artifact, files from different Eclipse versions don't collide.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<testsuite name="eclipse-2026-03.TestAllUnitTests"
           tests="1" failures="0" errors="0" skipped="0"
           time="12.345"
           timestamp="2026-06-27T10:00:00">
  <testcase name="TestAllUnitTests"
            classname="org.openclover.eclipse.functest"
            time="12.345"/>
  <!-- on failure: -->
  <testcase ...>
    <failure message="CloverDatabase is null">full stack / detail here</failure>
  </testcase>
</testsuite>
```

Also writes a text summary `target/functest.log` for human readability.

---

## Test Project Triage

### Tier 1 — Build + JUnit run + coverage assertions
- `TestAllUnitTests` + `TestAllUnitTestsExternal` — canonical case
- `TestAppAndSourceFolders` — src/test split
- `TestCoverageTypes` — passing/failing/incidental coverage types
- `TestDependenciesA` + `TestDependenciesB` + `TestDependenciesC` — cross-project deps
- `TestMultipleSourceFolders` — multiple src dirs
- `TestSeparateInstrOutputFolder` — non-default output dir
- `TestRelativeInitstring` — non-default init string path
- `TestRootSourceFolder` — source at project root

### Tier 2 — Build + DB assertion only (no JUnit subprocess)
- `TestExcludesProject` — no tests, verify instrumentation respects excludes
- `TestEncodingsProject` — no test classes; verify build + DB
- `TestJava13Project` / `TestJava14Project` / `TestJava15Project` — verify build at language level

### Tier 3 — Deferred
- `TestAntBuild` — Ant-based, different flow; requires Ant on PATH
- `TestDynamicWebProject` — requires WTP bundles (not in Eclipse for Java package)
- `TestEquinoxProject` / `TestEquinoxTestsProject` — OSGi classpath complications

---

## Test Project Refresh Tasks

1. **All projects**: Add `.settings/org.eclipse.jdt.core.prefs` with:
   ```properties
   eclipse.preferences.version=1
   org.eclipse.jdt.core.compiler.compliance=11
   org.eclipse.jdt.core.compiler.source=11
   org.eclipse.jdt.core.compiler.codegen.targetPlatform=11
   ```
   (Java 11 minimum; prevents JDT from defaulting to project-type-specific or deprecated compliance levels)

2. **All Tier 1/2 projects**: Change `.classpath` from `JUNIT_CONTAINER/3` to `JUNIT_CONTAINER/4`.

3. **All `.launch` files**: Change `TEST_KIND` from `org.eclipse.jdt.junit.loader.junit3` to `org.eclipse.jdt.junit.loader.junit4`.

4. **`TestEquinoxProject` / `TestEquinoxTestsProject` MANIFEST**: Change `Bundle-RequiredExecutionEnvironment: J2SE-1.5` to `JavaSE-11`.

5. **`TestEncodingsProject`**: Verify `.classpath` exists (the file was hidden from `ls` by leading dot; if truly missing, create it with `CLOVER_RUNTIME` + `src` entry + JRE container + `bin` output).

6. **Import ordering**: Hard-coded dependency order in `WorkspaceManager`:
   ```
   TestAllUnitTestsExternal → TestAllUnitTests
   TestDependenciesA → TestDependenciesB → TestDependenciesC
   ```

---

## CI Workflow: `B-eclipse-compatibility-tests.yml`

Full filename: `.github/workflows/B-eclipse-compatibility-tests.yml`

```yaml
name: B Eclipse Compatibility Tests

on:
  push:
    branches: [ master ]
  pull_request:
    branches: [ master ]
  workflow_dispatch:

jobs:
  build-plugins:
    name: Build clover-eclipse plugins
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: temurin
          cache: maven

      - name: Prepare Eclipse libs (current version for compilation)
        run: mvn --batch-mode install -Pworkspace-setup -f clover-eclipse-libs/pom.xml

      - name: Build all modules including clover-eclipse
        run: |
          mvn --batch-mode install -DskipTests=true

      - name: Upload clover-eclipse artifacts
        uses: actions/upload-artifact@v4
        with:
          name: clover-eclipse-build
          path: |
            clover-eclipse/org.openclover.eclipse.updatesite/target/classes/
            clover-eclipse/org.openclover.eclipse.functest.runner/target/*.jar
            clover-runtime/target/clover-runtime-*.jar

  functest:
    name: Eclipse ${{ matrix.eclipse-version }}
    needs: build-plugins
    runs-on: ubuntu-latest

    strategy:
      fail-fast: false
      matrix:
        include:
          - eclipse-version: "2023-06"
            eclipse-libs-version: "2023.06"
            eclipse-download-site: "https://archive.eclipse.org"
          - eclipse-version: "2024-06"
            eclipse-libs-version: "2024.06"
            eclipse-download-site: "https://archive.eclipse.org"
          - eclipse-version: "2025-06"
            eclipse-libs-version: "2025.06"
            eclipse-download-site: "https://ftp.fau.de/eclipse"
          - eclipse-version: "2026-03"
            eclipse-libs-version: "2026.03"
            eclipse-download-site: "https://ftp.fau.de/eclipse"

    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: temurin
          cache: maven

      - name: Install Xvfb
        run: sudo apt-get install -y xvfb

      - name: Cache Eclipse ${{ matrix.eclipse-version }} download
        uses: actions/cache@v4
        with:
          path: clover-eclipse/org.openclover.eclipse.functest/target/download
          key: eclipse-download-${{ matrix.eclipse-version }}

      - name: Download build artifacts
        uses: actions/download-artifact@v4
        with:
          name: clover-eclipse-build
          path: clover-eclipse-build/

      - name: Prepare Eclipse libs for ${{ matrix.eclipse-version }}
        run: |
          mvn --batch-mode install -Pworkspace-setup \
            -Declipse.version=${{ matrix.eclipse-version }} \
            -Declipse.libs.version=${{ matrix.eclipse-libs-version }} \
            -Declipse.download.site=${{ matrix.eclipse-download-site }} \
            -Declipse.download.path=technology/epp/downloads/release/${{ matrix.eclipse-version }}/R \
            -Declipse.download.file=eclipse-java-${{ matrix.eclipse-version }}-R-linux-gtk-x86_64.tar.gz \
            -f clover-eclipse-libs/pom.xml

      - name: Run functional tests against Eclipse ${{ matrix.eclipse-version }}
        run: |
          mvn --batch-mode verify \
            -pl clover-eclipse/org.openclover.eclipse.functest \
            -Peclipse-functest \
            -Declipse.version=${{ matrix.eclipse-version }} \
            -Declipse.libs.version=${{ matrix.eclipse-libs-version }} \
            -Declipse.download.site=${{ matrix.eclipse-download-site }} \
            -Declipse.download.path=technology/epp/downloads/release/${{ matrix.eclipse-version }}/R

      - name: Upload test results for Eclipse ${{ matrix.eclipse-version }}
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-results-eclipse-${{ matrix.eclipse-version }}
          path: |
            clover-eclipse/org.openclover.eclipse.functest/target/surefire-reports/TEST-eclipse-${{ matrix.eclipse-version }}-*.xml
            clover-eclipse/org.openclover.eclipse.functest/target/functest-workspace/.metadata/.log
          if-no-files-found: warn
```

### Notes on artifact naming

Each matrix leg uploads a **separate artifact** named `test-results-eclipse-YYYY-MM` (e.g., `test-results-eclipse-2026-03`). The XML files inside are also prefixed with the Eclipse version (`TEST-eclipse-2026-03-TestAllUnitTests.xml`). This means:
- No overwriting between versions
- Each version's results are independently downloadable
- A downstream job or report step can download all artifacts and merge them if needed

### `eclipse.download.path` override

The `eclipse.download.path` is overridden explicitly in both the workspace-setup step and the functest step rather than computed from `eclipse.version` within the pom. This is intentional: the path structure is stable but overriding it explicitly avoids complex property-composition logic in Maven. The CI matrix owns the full URL components.

---

## Build Wiring

### `clover-eclipse/pom.xml` — add modules
```xml
<modules>
  ...existing modules...
  <module>org.openclover.eclipse.functest.runner</module>
  <module>org.openclover.eclipse.functest</module>
</modules>
```

### `org.openclover.eclipse.functest/pom.xml` — profile
```xml
<profiles>
  <profile>
    <id>eclipse-functest</id>
    <!-- Not auto-activated: must use -Peclipse-functest.
         Linux is the only target; macOS would need Eclipse.app path adjustments. -->
    <build>
      <plugins>
        <!-- maven-antrun-plugin: download (skip if cached) + untar -->
        <!-- antrun or exec-maven-plugin: p2 director install -->
        <!-- exec-maven-plugin: xvfb-run ./eclipse -nosplash -application ... -->
      </plugins>
    </build>
  </profile>
</profiles>
```

Profile is explicitly activated (`-Peclipse-functest`) rather than OS-auto-activated, so that developers can skip it locally without needing to be on a non-Linux machine.

---

## Key Technical Risks

1. **Display requirement**: `org.openclover.eclipse.core` uses SWT/UI. Must use `xvfb-run` on CI. On macOS dev machines, a display is always available.

2. **Job scheduling**: `workspace.build()` is asynchronous. Must join `ResourcesPlugin.FAMILY_AUTO_BUILD` and `ResourcesPlugin.FAMILY_MANUAL_BUILD` before reading results.

3. **OSGi class loading**: The runner has `Require-Bundle: org.openclover.eclipse.core`. Calls to `CloverDatabase.loadWithCoverage()` work because `org.openclover.eclipse.core` exports `org.openclover.core` in its `Export-Package` manifest.

4. **JUnit 4 JAR location**: `org.eclipse.jdt.junit.JUNIT_CONTAINER/4` resolves to `eclipse/plugins/org.junit_4.*.jar`. The subprocess runner must locate this JAR dynamically from `eclipse/plugins/`. Alternatively, declare `junit:junit:4.13.2` as a `provided` dependency in `functest` pom and pass via `-cp`.

5. **p2 director**: Available in any Eclipse distribution. Invoke via `eclipse -application org.eclipse.equinox.p2.director`. Needs network access to the local `file://` update site.

6. **Classpath variable race**: `JavaCore.setClasspathVariable()` is async. Join the resulting job before calling `project.open()`.

7. **`eclipse-libs` version mismatch in matrix**: The `workspace-setup` profile must install the correct version of eclipse-libs that matches the Eclipse being tested. The matrix step does this by passing `-Declipse.libs.version` to the workspace-setup invocation.

---

## Implementation Order

1. **Step 1**: Delete `clover-eclipse/workspace/`. Add `eclipse.version` / `eclipse.libs.version` to `clover-eclipse/pom.xml`; update all 7 plugin poms to use `${eclipse.libs.version}`.

2. **Step 2**: Update test project `.classpath` files to `JUNIT_CONTAINER/4`. Add `.settings/org.eclipse.jdt.core.prefs` with Java 11 compliance to all projects. Update `.launch` files `TEST_KIND`.

3. **Step 3**: Create `org.openclover.eclipse.functest.runner` with skeleton `Application.java` that imports and builds `TestAllUnitTests` only; confirm it launches from command line against a real Eclipse install.

4. **Step 4**: Create `org.openclover.eclipse.functest` Maven module with download + p2 install + `xvfb-run` invocation. Wire to `integration-test` phase under `eclipse-functest` profile.

5. **Step 5**: Implement `BuildVerifier` + `CoverageVerifier` + `SurefireReporter`. Get Tier 1 projects passing with DB assertions only (no JUnit subprocess).

6. **Step 6**: Implement `TestRunner` subprocess (JUnit 4). Get JUnit projects running and coverage assertions passing.

7. **Step 7**: Add Tier 2 projects. Skip Tier 3 with a logged message in the XML (`<skipped/>`).

8. **Step 8**: Add `.github/workflows/B-eclipse-compatibility-tests.yml`. Verify all 4 matrix entries pass on Ubuntu.
