# Eclipse Libs — Maven Central migration (OC-97)

## Current state (OC-97, superseded the installer/tag mechanism)

`clover-eclipse-libs` no longer downloads or locally-installs Eclipse JARs. It is now a thin
**aggregator POM** that declares Eclipse bundles as ordinary Maven Central dependencies. The
`clover-eclipse/*` modules still import it via `<type>pom</type>`, so nothing changed on the
consumer side.

Eclipse publishes its bundles to Maven Central under two groupIds — `org.eclipse.platform` and
`org.eclipse.jdt` — using the bundle symbolic name as the artifactId and a three-segment version
(the `.vYYYYMMDD-HHMM` qualifier is dropped). Two bundles use different coordinates:
- `org.eclipse.jdt.core.compiler.batch` → `org.eclipse.jdt:ecj`
- `org.osgi.service.prefs` → `org.osgi:org.osgi.service.prefs`

**No usable aggregator/BOM exists.** The feature-level artifacts (`org.eclipse.platform`,
`org.eclipse.sdk`, `org.eclipse.jdt`) declare only a handful of, often `optional=true`,
dependencies and carry no JDT API closure — verified `org.eclipse.sdk`'s transitive closure
contains zero JDT bundles. So the aggregator instead lists a **small set of top-level bundles**
and relies on the (correct) transitive deps of the individual bundle POMs to bring the rest:
`org.eclipse.jdt.ui` transitively pulls jdt.core, jface, swt, text, ui.workbench, core.*, etc.
12 direct deps transitively resolve the full ~31-bundle API set the plugins compile against.
`org.eclipse.swt.win32.win32.x86_64` is kept explicit so the compile classpath stays
platform-independent (a bare `org.eclipse.swt` resolves the build host's native fragment).

Verified: all three CI-tested trains (2024-06, 2025-06, 2026-03) have their full bundle sets on
Maven Central; the reduced 12-dep set clean-compiles all five clover-eclipse code modules.

## Bumping to a newer Eclipse release

Edit the 12 `<*.version>` properties in `clover-eclipse-libs/pom.xml` to the three-segment
versions shipped in that "Eclipse for Java Developers" distribution (look them up on
https://central.sonatype.com). Also update `eclipse.version` / `eclipse.libs.version` in
`clover-eclipse/pom.xml` and the `<version>` of `clover-eclipse-libs` itself. No download, no
extract, no `install-file`, no git tag.

## The `clover-eclipse-libs-*` git tags — now inert

Tags `clover-eclipse-libs-{2020.06 … 2026.03, luna-sr2}` captured, per release, the old pom with
its per-JAR `<*.version>` properties. Their **only** consumer was the removed functest step
`git show clover-eclipse-libs-<v>:clover-eclipse-libs/pom.xml | mvn install -Pworkspace-setup`.
Nothing reads them anymore. They are harmless historical snapshots — no regeneration needed, and
no new tag on future Eclipse bumps.

## How Eclipse *runtime* compatibility testing fetches older versions (unchanged, no tags)

The functest job downloads the full "Eclipse for Java Developers" distribution purely from the
matrix `eclipse-version` — `org.openclover.eclipse.functest/pom.xml` derives the URL from
`${eclipse.version}` (path `technology/epp/downloads/release/${eclipse.version}/R`, file
`eclipse-java-${eclipse.version}-R-linux-gtk-x86_64.tar.gz`, site via `eclipse.download.site`
matrix override). p2-installs the Clover feature and runs the runner. This is independent of
`clover-eclipse-libs` and of any git tag; multi-version *runtime* coverage is fully preserved.

## What was removed (the OLD mechanism)

- `clover-eclipse-libs/pom.xml`: `default`/`workspace-setup` profiles, antrun download+unzip,
  30+ `maven-install-plugin` `install-file` executions, hardcoded win32-zip download coordinates.
- `clover-eclipse-libs/get-plugin-versions.sh` (parsed `target/extract/`).
- `B-eclipse-compatibility-tests.yml`: eclipse-libs binary cache + the per-version `git show <tag>`
  install step in the functest job + the unused `eclipse-libs-version` matrix field.
- `A-build-and-test-jdk21.yml`, `A-build-and-test-jdk25.yml`, `D-release-openclover.yml`,
  `README.md`: the `install -Pworkspace-setup -f clover-eclipse-libs/pom.xml` line (+ the eclipse
  binary caches in the A-* workflows).

## Historical: the OLD installer mechanism (pre-OC-97, no longer in the repo)

Each "Eclipse for Java Developers" release was converted into a committed+tagged
`clover-eclipse-libs/pom.xml` whose `workspace-setup` profile downloaded the win32-x86_64 zip,
extracted all JARs flat into `target/extract/`, and installed each under the fake
`org.openclover.eclipse.libs` group. `get-plugin-versions.sh` read `target/extract/` to emit the
`<*.version>` values. This was a workaround for Eclipse JARs not being on Maven — exactly what
OC-97 resolves.

## Plugin compatibility fixes — Eclipse 2020.06 (first version after luna-sr2)

Still-relevant history: upgrading from `luna-sr2` (Eclipse 4.4, 2014) to `2020.06`
(Eclipse 4.16, JDT 3.22) required these source changes in `clover-eclipse`:

| File | Problem | Fix |
|---|---|---|
| `org.openclover.eclipse.{core,runtime,testopt,ant}/pom.xml` | `<version>luna-sr2</version>` | Change to `<version>2020.06</version>` |
| `views/nodes/PackageFragmentAdapter.java` | JDT 3.22 added 4 abstract methods to `IPackageFragment`: `getOrdinaryClassFile(String)`, `getModularClassFile()`, `getAllClassFiles()`, `getOrdinaryClassFiles()` | Implement all 4 as delegates to `target.*` |
| `views/actions/ToggleWorkingSetModeActionDelegate.java` | `InternalPlatform.getDefault().log(Status)` removed; `Messages.preferences_saveProblems` is internal API | Replace with `CloverPlugin.getInstance().getLog().log(status)` and an inline string |
| `org.openclover.eclipse.core/pom.xml` Export-Package | `org.openclover.runtime.api` and `org.openclover.runtime.api.registry` not exported; `testopt` bundle can't load `CloverException` via OSGi | Add both packages to `<Export-Package>` in the manifest entries |
| `projects/builder/BuildCoordinator.java` | `-bootclasspath` rejected by JDT batch compiler at compliance level ≥ 9 (Java 14 source level in use) | Detect source level: use `-classpath` when `!source.startsWith("1.")`, else `-bootclasspath` |
| `org/eclipse/jdt/internal/compiler/batch/CloverCompiler.java` | `Opcodes.ASM5` in `RecorderInnerClassRemover` causes `UnsupportedOperationException: NestMember requires ASM7` on Java 14 class files | Change `Opcodes.ASM5` → `Opcodes.ASM9` (project already bundles asm-9.7.jar) |

**Key insight on the build flow:** The PostJavaCloverBuilder only instruments files that were compiled in the current JDT build cycle (`dirtyFiles` populated by `CloverCompilationParticipant.buildStarting()`). After enabling Clover on a project, a **Project → Clean** is required to trigger a full rebuild; otherwise no files are dirty and instrumentation is silently skipped.
