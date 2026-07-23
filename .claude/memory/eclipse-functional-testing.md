# Eclipse Functional Test Infrastructure — Implementation Plan

> **SUPERSEDED IN PART (OC-97):** the compile-time Eclipse libs are now pulled from Maven Central
> — `clover-eclipse-libs` is a thin aggregator POM, there is no `workspace-setup` download/install,
> no `get-plugin-versions.sh`, and no per-version `git show <tag>` step. See
> [[eclipse-libs-installers]]. Everything about the **functest runner itself** (module layout,
> Application.java, verifiers, tiers, download-full-Eclipse-by-`${eclipse.version}`, p2 install,
> xvfb) below is STILL ACCURATE. The obsolete parts are flagged inline with ⚠️ OC-97 notes.

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

> ⚠️ **OC-97:** the compile-time libs now come from Maven Central. `clover-eclipse-libs/pom.xml`
> no longer has any download file/path — it is an aggregator POM. The `eclipse.version` /
> `eclipse.libs.version` properties in `clover-eclipse/pom.xml` still exist (used by functest
> downloads and by the consumers' `${eclipse.libs.version}` dependency version), but there is no
> `-Declipse.libs.version` matrix override anymore. See [[eclipse-libs-installers]].

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

### OSGi manifest

There is no separate `META-INF/MANIFEST.MF` file. The OSGi headers are generated inline by `maven-jar-plugin` `<manifestEntries>` in the runner's `pom.xml`:

```
Bundle-SymbolicName: org.openclover.eclipse.functest.runner; singleton:=true
Bundle-Version: 5.0.0
Require-Bundle: org.eclipse.core.runtime,org.eclipse.core.resources,org.eclipse.jdt.core,
 org.eclipse.jdt.launching,org.openclover.eclipse.core;bundle-version=${project.version}
Eclipse-LazyStart: false
Bundle-Classpath: .
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

`IApplication` does not define `EXIT_ERROR`; the class declares `private static final Integer EXIT_ERROR = 1`.

Tier 3 projects are listed in a static `SKIPPED_PROJECTS` map (`LinkedHashMap<String, String>` of name → reason). They are passed to `importProjects()` so they are never opened in the workspace, and synthetic skipped `TestResult` entries are appended after the main loop.

`hasUnitTests(project)` reads the `.classpath` file as text and checks for `JUNIT_CONTAINER` — Tier 1 projects have it, Tier 2 do not.

`TestRunner.run()` takes a `File ignored` third parameter (the original plan passed `eclipseInstallDir` from WorkspaceManager, but WorkspaceManager has no such method; the runner locates the JUnit jar itself via `Platform.getInstallLocation()`).

`printSummary()` reports `passed / failed / skipped out of N`.

```java
private static final Integer EXIT_ERROR = 1;

private static final Map<String, String> SKIPPED_PROJECTS = new LinkedHashMap<>();
static {
    SKIPPED_PROJECTS.put("TestAntBuild",           "Tier 3: requires Ant on PATH");
    SKIPPED_PROJECTS.put("TestDynamicWebProject",  "Tier 3: requires WTP bundles not present in Eclipse for Java");
    SKIPPED_PROJECTS.put("TestEquinoxProject",     "Tier 3: OSGi classpath complications");
    SKIPPED_PROJECTS.put("TestEquinoxTestsProject","Tier 3: OSGi classpath complications");
}

public Object start(IApplicationContext context) throws Exception {
    // args parsed from context.getArguments().get(IApplicationContext.APPLICATION_ARGS)
    // -projectsDir, -cloverRuntime, -reportsDir, -eclipseVersion

    WorkspaceManager wm = new WorkspaceManager(projectsDir, cloverRuntime);
    wm.setCloverRuntimeVariable();
    wm.importProjects(SKIPPED_PROJECTS.keySet());   // Tier 3 projects are skipped
    wm.buildAll();

    List<TestResult> results = new ArrayList<>();
    for (IProject project : wm.getProjects()) {
        TestResult r = new TestResult(project.getName());
        BuildVerifier.verify(project, r);
        if (!r.hasBuildErrors()) {
            if (hasUnitTests(project)) {              // reads .classpath for JUNIT_CONTAINER
                TestRunner.run(project, cloverRuntime, null, r);
                wm.refresh(project);
                CoverageVerifier.verify(project, r);
            } else {
                CoverageVerifier.verifyDbOnly(project, r);
            }
        }
        r.setDurationMs(...);
        results.add(r);
    }

    // Append synthetic skipped entries for Tier 3
    for (Map.Entry<String, String> e : SKIPPED_PROJECTS.entrySet()) {
        TestResult r = new TestResult(e.getKey());
        r.skip(e.getValue());
        results.add(r);
    }

    SurefireReporter.write(results, reportsDir, eclipseVersion);
    printSummary(results);   // "X passed, Y failed, Z skipped out of N projects"
    return results.stream().anyMatch(TestResult::hasFailed) ? EXIT_ERROR : EXIT_OK;
}
```

### `WorkspaceManager.java`
- `setCloverRuntimeVariable()`: `JavaCore.setClasspathVariable("CLOVER_RUNTIME", new Path(cloverRuntime), null)` then join the resulting classpath init job before opening projects.
- `importProjects(Set<String> skipProjects)`: Projects in the skip set are logged and skipped entirely. Sorted import to respect dependencies:
  1. `TestAllUnitTestsExternal` before `TestAllUnitTests`
  2. `TestDependenciesA` → `B` → `C`
  3. Others alphabetically
  
  Per project: `workspace.loadProjectDescription(new Path(dir+"/.project"))` → `desc.setLocation(...)` → `project.create(desc, null)` → `project.open(null)`.
- `buildAll()`: `workspace.build(IncrementalProjectBuilder.FULL_BUILD, null)` then `Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null)` and `join(ResourcesPlugin.FAMILY_MANUAL_BUILD, null)`.
- No `getEclipseInstallDir()` method — the eclipse install location is resolved inside `TestRunner` via `Platform.getInstallLocation()`.

### `BuildVerifier.java`
After build, checks:
1. No `IMarker.SEVERITY_ERROR` markers on the project (`project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE)`)
2. `CloverProject.getFor(project).getRegistryFile()` exists — resolves the actual DB path (default `.clover/coverage.db`, or per-project custom path); **not** `project.getFolder(".clover").getFile("clover.db")`
3. Output folder (resolved via `JavaCore.create(project).getOutputLocation()`) contains at least one `.class` file (checked only when no build errors)

### `TestRunner.java`
For projects with `JUNIT_CONTAINER` in `.classpath`. Signature: `run(IProject, String cloverRuntime, File ignored, TestResult)` — the third parameter is unused; JUnit JAR is located internally via `Platform.getInstallLocation().getURL()` (no caller-supplied install dir needed).

```java
// Locate JUnit 4 JAR: Platform.getInstallLocation() → eclipse/plugins/org.junit_4*.jar
File junitJar = findJUnitJar();
// initString via CloverProject.getFor(project).getRegistryFile()
String cp = outputDir + pathSep + cloverRuntime + pathSep + junitJar;
ProcessBuilder pb = new ProcessBuilder(
    javaExe, "-cp", cp,
    "org.junit.runner.JUnitCore",
    ...discoverTestClasses(outputDir)  // scan output dir for *Test.class / *Tests.class
);
pb.environment().put("CLOVER_INITSTRING", initString);
pb.redirectErrorStream(true);
Process proc = pb.start();
String output = new String(proc.getInputStream().readAllBytes());
int exit = proc.waitFor();
if (exit != 0) r.fail("JUnit runner exited " + exit + ":\n" + output);
```

### `CoverageVerifier.java`
initString resolved via `CloverProject.getFor(project).getRegistryFile().getAbsolutePath()` (not hardcoded `.clover/clover.db`). Load errors are caught and reported as failures; the method returns null on error (no `assertNotNull` call on the DB object itself).

```java
// verifyDbOnly — after build, no tests run:
CloverDatabase db = loadDb(project, result);   // returns null and records failure on error
if (db == null) return;
result.assertFalse(
    db.getRegistry().getProject().getPackages(HasMetricsFilter.ACCEPT_ALL).isEmpty(),
    "No packages found in Clover registry");

// verify — after test run:
result.assertFalse(db.getCoverageData().isEmpty(),
    "Clover coverage data is empty");
int covered = db.getRegistry().getProject().getMetrics().getNumCoveredElements();
result.assertTrue(covered > 0, "Zero covered elements in Clover registry after test run");
```

Note: `HasMetricsFilter.ACCEPT_ALL` (not `new HasMetrics.Everything()`); `db.getCoverageData().isEmpty()` (not `db.getCoverage()`).

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

Handles skipped projects: writes `<skipped message="..."/>` inside `<testcase>` and sets `skipped="1"` on `<testsuite>`. No separate `target/functest.log` text file — the summary is written to stdout by `Application.printSummary()`.

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
Detection: absence of `JUNIT_CONTAINER` in `.classpath`. Projects in this tier:
- `TestExcludesProject` — no tests, verify instrumentation respects excludes
- `TestEncodingsProject` — no test classes; verify build + DB
- `TestJava8Project` / `TestJava11Project` / `TestJava17Project` / `TestJava21Project` — verify build at language level

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

> ⚠️ **OC-97:** the YAML below is the ORIGINAL plan. The current workflow has dropped: the
> eclipse-libs binary cache, the "Prepare Eclipse libs" `git show <tag>` install step, the
> `eclipse-libs-version` matrix field, and `-Declipse.libs.version`. The functest legs now just
> download the full Eclipse (`${eclipse.version}`) and run — read the live file for the real steps.

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
            clover-eclipse/org.openclover.eclipse.updatesite/target/clover-eclipse-site-*.zip
            clover-eclipse/org.openclover.eclipse.functest.runner/target/org.openclover.eclipse.functest.runner-*.jar
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

      - name: Cache Eclipse libs download (${{ matrix.eclipse-libs-version }})
        uses: actions/cache@v4
        with:
          path: clover-eclipse-libs/target/download
          key: ${{ runner.os }}-eclipse-libs-${{ matrix.eclipse-libs-version }}

      - name: Cache Eclipse functest download (${{ matrix.eclipse-version }})
        uses: actions/cache@v4
        with:
          path: clover-eclipse/org.openclover.eclipse.functest/target/download
          key: ${{ runner.os }}-eclipse-functest-${{ matrix.eclipse-version }}

      - name: Download build artifacts
        uses: actions/download-artifact@v4
        with:
          name: clover-eclipse-build
          # No path specified — files land at their original workspace-relative paths,
          # matching functest pom's default property values exactly.

      - name: Prepare Eclipse libs for ${{ matrix.eclipse-version }}
        run: |
          # Overwrite with the version-tagged pom so Maven installs the correct artifact version
          git show clover-eclipse-libs-${{ matrix.eclipse-libs-version }}:clover-eclipse-libs/pom.xml \
            > clover-eclipse-libs/pom.xml
          mvn --batch-mode install -Pworkspace-setup -f clover-eclipse-libs/pom.xml \
            -Declipse.download.site=${{ matrix.eclipse-download-site }} \
            -Declipse.download.path=technology/epp/downloads/release/${{ matrix.eclipse-version }}/R \
            -Declipse.download.file=eclipse-java-${{ matrix.eclipse-version }}-R-win32-x86_64.zip

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

`eclipse.download.path` and `eclipse.download.file` are overridden in the workspace-setup step (which downloads the win32 zip to extract JARs) but not in the functest step — the functest pom derives both from `${eclipse.version}` for the Linux tar.gz. Only `eclipse.download.site` is overridden in the functest step (to switch between archive.eclipse.org and ftp.fau.de).

### Root POM: `eclipse-jdk21plus` profile

`clover-eclipse` is not in the default `<modules>` list of the root `pom.xml`. It is gated behind a profile `eclipse-jdk21plus` with `<activation><jdk>[21,)</jdk></activation>`. This means the A-build workflows (JDK 8/11/17) build the full reactor without any manual `--projects` exclusions; `clover-eclipse` simply does not appear on those JDK versions.

### Workspace-setup version alignment

> ⚠️ **OC-97: REMOVED.** There is no longer a `workspace-setup` install of `clover-eclipse-libs`,
> no `git show clover-eclipse-libs-<v>:...` overwrite, and no win32-zip extraction. Compile-time
> libs resolve from Maven Central at a single version. The `clover-eclipse-libs-*` tags are inert.
> The old behaviour is described below for historical context only.

`clover-eclipse-libs/pom.xml` always has the current Eclipse version hardcoded (e.g., `<version>2026.03</version>`). To install a different version for a matrix leg, the step first overwrites the file from the git tag:
```bash
git show clover-eclipse-libs-2023.06:clover-eclipse-libs/pom.xml > clover-eclipse-libs/pom.xml
```
This makes `mvn install -Pworkspace-setup` install `clover-eclipse-libs:pom:2023.06` (with 2023.06 JARs) to the local Maven repo, which is what the `clover-eclipse` parent BOM import needs. Workspace-setup downloads the **win32 zip** (not the Linux tar.gz) to extract individual JARs.

---

## `TestResult.java` — Skip Support

In addition to `fail()` / `hasFailed()`, `TestResult` carries:
- `skip(String reason)` — marks the result as skipped with a reason string
- `isSkipped()` / `getSkipReason()` — read back the skip state

Used for Tier 3 projects whose `TestResult` is created by `Application` (not by the main project loop) and passed directly to `SurefireReporter`.

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

7. ~~**`eclipse-libs` version mismatch in matrix**~~: ⚠️ **OC-97: no longer applies.** Compile-time
   libs come from Maven Central at a single version; the functest matrix only varies the *runtime*
   Eclipse it downloads (via `${eclipse.version}`), which never depended on eclipse-libs.

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
