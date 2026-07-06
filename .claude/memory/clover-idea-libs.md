# clover-idea build — migrated to Gradle (2026-07-06)

## TL;DR

`clover-idea-libs` is **deleted**. `clover-idea` is no longer built by Maven Surefire against
hand-installed IDEA JARs — it's built, tested, and packaged by **Gradle**, using the official
**IntelliJ Platform Gradle Plugin**, which downloads the target IDEA distribution itself as a
normal dependency. `clover-idea/pom.xml` survives only as a thin wrapper so `mvn install` still
builds the whole reactor in one command.

Plans A/B/C described below (Maven-only fixes to `clover-idea-libs`) are **historical** — kept
for context on what was tried, but superseded once the actual root cause was found.

## Root cause (why Maven-only fixes couldn't work)

Maven Surefire cannot launch IDEA 2025+ such that CORE content-module services
(`SettingsController`) register — this fails for **every** app-based test, regardless of which
JARs are on the classpath or how they're installed. Gradle, via the IntelliJ Platform Gradle
Plugin, launches the platform the way JetBrains actually supports; that's the only way that
correctly initializes CORE services on IDEA 2025+. This is why the fix wasn't "install libs
better" but "stop using Surefire for this module."

## Final design

- **`clover-idea/build.gradle.kts`** — the real build. Uses
  `intellijPlatform { create(ideaType, ideaVersion) }` to download the target IDE (IU by default),
  `bundledPlugin("com.intellij.java")`, and `testFramework(Platform + Plugin.Java)`.
  Depends on `org.openclover:clover` and `net.sf.jtreemap:jtreemap` with `isTransitive = false`
  (clover is a self-contained shaded jar; pulling transitives reintroduces old fastutil/jdom that
  binary-conflict with IDEA's bundled copies — `VerifyError`/`NoClassDefFoundError`).
  Produces a fat plugin jar at `build/dist/clover-idea-<version>.jar` (`fatJar` task) and an
  autoupdate descriptor at `build/autoupdate/latestStableVersion.xml`, matching the old Maven
  assembly's output exactly (verified: 4650 vs 4658 entries, diff is just source evolution, no
  bloat).
- **`clover-idea/settings.gradle.kts`**, **`gradle.properties`** (`ideaType`, `ideaVersion`,
  `cloverVersion`), **`gradlew`** (staged with the executable bit via
  `git update-index --chmod=+x` — exFAT working copies drop it).
- **`clover-idea/pom.xml`** — `<packaging>pom</packaging>` wrapper. Uses `exec-maven-plugin` to
  invoke `./gradlew` at the `compile`/`test`/`package` phases, and `maven-install-plugin
  install-file` at `install` to put the fat jar into `~/.m2` as `org.openclover:clover-idea`.
  `-Didea.version.short` / `-Didea.gradle.type` map to Gradle's `-PideaVersion`/`-PideaType`.
  `maven.deploy.skip=true` — clover-idea is distributed via scp to openclover.org only, **never**
  pushed to OSSRH/Maven Central (matches the old pom, which had `maven-deploy-plugin` skip=true).
- **`clover-idea-libs` module: deleted** (was ~400 hardcoded `install:install-file` calls, one per
  IDEA JAR, keyed by git tag per version — see "Historical" below for why it existed and why it
  was painful). Removed from root `pom.xml`'s `workspace-setup` profile.
- Gradle caches downloaded IDE distributions itself (`~/.gradle/caches`, via
  `intellijPlatform { defaultRepositories() }` in settings.gradle.kts) — CI just caches that
  directory instead of a project-specific `target/download`.

## CI workflow changes

- **`B-idea-compatibility-tests.yml`** — rewritten. No more "Prepare IDEA libs" step (was
  `mvn install -Pworkspace-setup -f clover-idea-libs/pom.xml -Didea.version=... -Didea.download.file=... -Didea.download.dir=...`).
  Matrix now only needs `idea-version` (short, e.g. `2025.3.6`) and `idea-build` (for the cache
  key) — no more download-file/download-dir columns. Caches `~/.gradle/caches` +
  `~/.gradle/wrapper` keyed on `idea-build` instead of caching `clover-idea-libs/target/download`.
- **`A-build-and-test-jdk{8,11,17}.yml`** — dropped `Cache IDEA binaries` step and
  `mvn install -Pworkspace-setup -f clover-idea-libs/pom.xml` (clover-idea isn't in the reactor on
  JDK <21 anyway — `idea-jdk21plus` profile gates it).
- **`A-build-and-test-jdk{21,25}.yml`** — same removal, but added a `Cache Gradle IntelliJ
  Platform downloads` step (caches `~/.gradle/caches`+`~/.gradle/wrapper`) since these DO build
  clover-idea through Gradle as part of `mvn install -DskipTests=true`.
- **`C-prepare-release.yml`** — `versions:set` doesn't touch `clover-idea/gradle.properties`
  (it's not a pom), so both the release and next-iteration blocks now also
  `sed -i ... "s@^cloverVersion=.*@cloverVersion=<version>@" clover-idea/gradle.properties`.
- **`D-release-openclover.yml`** — removed the `clover-idea-libs` workspace-setup line; scp source
  paths changed from `clover-idea/target/...` to `clover-idea/build/dist/...` and
  `clover-idea/build/autoupdate/...` (Gradle output layout, not Maven's).
- **`README.md`** — removed the `clover-idea-libs` workspace-setup line from the quick-setup
  instructions; added a note that Gradle downloads the IDE itself.

## Validation done (local + CI)

- Local (macOS, 2025.3.6): 119/125 tests pass; the 6 failures are macOS-specific
  (encoding/flaky) and pass on Linux CI.
- CI: 2024 and 2025 fully green; 2026 has 9 failures — genuine IDEA behavior changes (stricter
  PSI project-model checks, encoding defaults, a `SmartModeScheduler` `TestLogger` error), not
  infrastructure problems. Deferred — infra was the priority, failing tests are the last step.
- Packaging: fat jar entry count matches the old Maven assembly (4650 vs 4658; diff is source
  evolution — anonymous classes now lambdas/named + 2 new icons — not bloat).
- `mvn install -DskipTests` end-to-end validated locally: wrapper compile/test/package/install
  phases all delegate to Gradle correctly, fat jar lands in `~/.m2`.

## Failing-test fixes (2026-07-06, all 3 versions now green locally)

After the infra landed, 2024 & 2025 were green on CI but 2026 had 9 failures on CI, and 2025 had
6 failures locally on macOS (green on Linux CI). All fixed; every version now passes all 125 tests
locally (2024 IU, 2025.3.6 IU, 2026.1.3 IU). Two root causes, both in test code / build config —
no production change:

1. **`createFile(name,text)` + `VirtualFile.move()` foreign-file pattern** (in
   `ExclusionUtilIdeaTest` and `InclusionDetectorIdeaTest` `setUp`). Creating a PSI file in IDEA's
   shared default project and then moving its VirtualFile into the test's own project tree fires a
   global PSI move listener that, on 2025.3+, **non-deterministically** trips a hard project-model
   integrity assertion: "Trying to get PSI for a file that is not included in the project model of
   this project." Non-determinism is why it varied by env/version (green CI-2025, red local-2025,
   red CI-2026 for InclusionDetector but green CI-2026 for ExclusionUtil despite identical setUp).
   - Fix: create the foreign file straight through the VFS in the (deliberately non-source-root)
     `subDir` and get its PSI via `getPsiManager().findFile(vf)`:
     `subDir.createChildData(this, name)` + `VfsUtil.saveText(vf, text)` + `findFile`.
   - Do NOT use the `createFile(module, dir, ...)` overload as a shortcut — it calls
     `addSourceContentToRoots(module, dir)`, which pulls `subDir` into the module and breaks the
     tests' premise (foreignRoot/subDir must stay outside the project model). Verified by decompile.
   - This single fix also cured the 3 **CharsetUtilIdeaTest** failures (`testProjectEncoding...`,
     `testGetFileEncodingFallsBackToSystem...`, `testGetFileEncodingFallsProject...`), which were a
     **cascade**: the failing heavy tests leaked open projects, so `EncodingManager.getInstance()`'s
     internal `guessProject(LightVirtualFile)` (which only resolves when `getOpenProjects().length
     == 1`) returned null → per-file `setEncoding/getEncoding` silently no-op'd → fell back to the
     project default. With the leaks gone, exactly one project is open and encoding get/set works.

2. **Mockito can't mock anything on IDEA 2026 (JBR 25).** 2026's bundled JetBrains Runtime is Java
   25; mockito-core 5.11.0's Byte Buddy officially supports only up to Java 22 and throws
   "Java 25 (69) is not supported… set net.bytebuddy.experimental as a VM property" for every
   `mock()` (hit EventListenerInstallatorTest, IdeaTestFilterIdeaTest×2, LibrarySupportIdeaTest.
   testReorder, AggregatingFilterTest×4). Fix in `build.gradle.kts`: bump `mockito-core` to
   `5.18.0` **and** set `systemProperty("net.bytebuddy.experimental", "true")` on the test task
   (belt-and-suspenders: the flag also covers future JBRs newer than Mockito's declared support).

Also in `build.gradle.kts`: `testLogging.exceptionFormat = FULL` + `showCauses`/`showStackTraces`
kept on purpose — the original terse CI output ("MockitoException at line 39", no cause) is what
made 2026 hard to diagnose from logs.

Tests are run locally with `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/...`; the
Gradle IntelliJ plugin then launches the target IDE on that IDE's own bundled JBR. Concurrent
Gradle test invocations against the same sandbox corrupt its index (`PersistentEnumerator storage
corrupted …/system-test/index`); run serially or `rm -rf .intellijPlatform/sandbox/.../system-test`
between overlapping runs.

## Known local caveat (exFAT)

exFAT (this machine's working-copy filesystem) has no POSIX file locking, which breaks Gradle's
cache. Run Gradle with `--project-cache-dir` pointed at an APFS path (e.g.
`$HOME/.gradle-clover-idea/cache`) when testing locally; CI (Linux) doesn't need this.
`clover-idea/gradlew` must be re-marked executable after any fresh checkout on exFAT
(`git update-index --chmod=+x clover-idea/gradlew`), since exFAT drops the exec bit.

---

## Historical: why `clover-idea-libs` existed and Plans A/B/C to fix it (abandoned)

Before the Gradle migration, `clover-idea-libs/pom.xml` contained ~400 hardcoded
`install:install-file` executions (one per IDEA JAR) and `clover-idea-all/pom.xml` mirrored that
as ~400 hardcoded `<dependency>` entries — both keyed by IDEA build number and by git tag
(`clover-idea-libs-<version>`), requiring a manual regenerate-splice-commit-tag cycle for every
new IDEA version, and force-pushing tags to change pom structure retroactively.

Plan C (approved 2026-07-01, **never implemented** — superseded by the Gradle migration once the
SettingsController root cause was understood) was to generate an `install-helper.pom` and
`clover-idea-all.pom` on the fly via `exec-maven-plugin`, eliminating the tag dance while staying
on Maven/Surefire. This would have fixed the packaging pain but **not** the actual test-launch
blocker (SettingsController), which is why the project moved to Gradle instead.

| IDEA version | Build number   | JDK |
|--------------|----------------|-----|
| 2024.3.7.1   | 243.28141.41   | 21  |
| 2025.3.6     | 253.33813.25   | 21  |
| 2026.1.3     | 261.25134.95   | 21  |
