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
