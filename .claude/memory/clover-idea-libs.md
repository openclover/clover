# clover-idea-libs upgrade plan

## Goal

Restore binary compatibility between OpenClover's IntelliJ IDEA plugin (`clover-idea`) and
modern IDEA versions, starting from the currently working IDEA 14.1.7 up through 2026.1.x.
One IDEA major release per calendar year is targeted.

---

## IDEA versions to add as lib tags

| IDEA version | Download URL filename         | Extracted dir              | Build number     | Tag                                    |
|--------------|-------------------------------|----------------------------|------------------|----------------------------------------|
| 15.0.6       | `ideaIC-15.0.6.tar.gz`        | `idea-IC-143.2370.31`      | `143.2370.31`    | `clover-idea-libs-15.0.143237031`      |
| 2016.3.8     | `ideaIC-2016.3.8.tar.gz`      | `idea-IC-163.15529.8`      | `163.15529.8`    | **already tagged** `clover-idea-libs-2016.3.163155298` |
| 2017.3.7     | `ideaIC-2017.3.7.tar.gz`      | `idea-IC-173.4710.11`      | `173.4710.11`    | `clover-idea-libs-2017.3.173471011`    |
| 2018.3.6     | `ideaIC-2018.3.6.tar.gz`      | `idea-IC-183.6156.11`      | `183.6156.11`    | `clover-idea-libs-2018.3.183615611`    |
| 2019.3.5     | `ideaIC-2019.3.5.tar.gz`      | `idea-IC-193.7288.26`      | `193.7288.26`    | `clover-idea-libs-2019.3.193728826`    |
| 2020.3.4     | `ideaIC-2020.3.4.tar.gz`      | `idea-IC-203.8084.24`      | `203.8084.24`    | `clover-idea-libs-2020.3.203808424`    |
| 2021.3.3     | `ideaIC-2021.3.3.tar.gz`      | `idea-IC-213.7172.25`      | `213.7172.25`    | `clover-idea-libs-2021.3.213717225`    |
| 2022.3.3     | `ideaIC-2022.3.3.tar.gz`      | `idea-IC-223.8836.41`      | `223.8836.41`    | `clover-idea-libs-2022.3.223883641`    |
| 2023.3.8     | `ideaIC-2023.3.8.tar.gz`      | `idea-IC-233.15619.7`      | `233.15619.7`    | `clover-idea-libs-2023.3.233156197`    |
| 2024.3.7.1   | `ideaIC-2024.3.7.1.tar.gz`    | `idea-IC-243.28141.41`     | `243.28141.41`   | `clover-idea-libs-2024.3.2432814141`   |
| 2025.3.6     | `idea-2025.3.6.tar.gz`        | `idea-IU-253.33813.25`     | `253.33813.25`   | `clover-idea-libs-2025.3.2533381325`   |
| 2026.1.3     | `idea-2026.1.3.tar.gz`        | `idea-IU-261.25134.95`     | `261.25134.95`   | `clover-idea-libs-2026.1.2612513495`   |

**Note on filename prefix:** For IDEA 2025+ the archive is `idea-*.tar.gz` (no `IC`); extracted as `idea-IU-*` (Ultimate edition — superset of Community, fine for compilation classpath).
**Note on devkit plugin:** Not present in 2025/2026 IU archives; omitted from those lib tags automatically by the generate scripts.

**Tag version string format:** `<MAJOR>.<MINOR>.<BUILD_NODOTS>` e.g.
`clover-idea-libs-2016.3.163155298` where `163155298` = `163.15529.8` with dots removed.

**pom.xml `<version>` format:** `<build>` e.g. `163.15529.8`
(comment: `<!-- major.minor.build (2016.3.8 build IC-163.15529.8) -->`).

---

## Source archives (for reading IDEA API without decompiling)

Useful when resolving API compatibility problems:

| IDEA version | GitHub source archive                                                          |
|--------------|--------------------------------------------------------------------------------|
| 2017.3.x     | `https://github.com/JetBrains/intellij-community/archive/idea/173.4710.11.zip` |
| 2018.3.x     | `https://github.com/JetBrains/intellij-community/archive/idea/183.6156.11.zip` |
| 2019.3.5     | `https://github.com/JetBrains/intellij-community/archive/idea/2019.3.5.zip`    |
| 2020.3.x     | `https://github.com/JetBrains/intellij-community/archive/idea/203.8084.24.zip` |
| 2021.3.x     | `https://github.com/JetBrains/intellij-community/archive/idea/213.7172.25.zip` |
| 2022.3.x     | `https://github.com/JetBrains/intellij-community/archive/idea/223.8836.41.zip` |
| 2023.3.x     | `https://github.com/JetBrains/intellij-community/archive/idea/233.15619.7.zip` |
| 2024.3.x     | `https://github.com/JetBrains/intellij-community/archive/idea/243.28141.41.zip`|
| 2025.2.x     | `https://github.com/JetBrains/intellij-community/archive/idea/252.28539.54.zip`|

---

## Phase 1 — Create Maven lib tags for each IDEA version

Repeat the steps below for each IDEA version in the table above (skip 2016.3 — already done).

### Steps per version

1. **Update download properties** in `clover-idea-libs/pom.xml` (`<properties>` section):
   ```xml
   <idea.download.site>https://download.jetbrains.com</idea.download.site>
   <idea.download.path>idea</idea.download.path>
   <idea.download.file>ideaIC-X.Y.Z.tar.gz</idea.download.file>   <!-- or idea-X.Y.Z.tar.gz for 2025+ -->
   <idea.download.dir>idea-IC-BUILD</idea.download.dir>
   ```

2. **Download and extract** (triggers via Maven workspace-setup profile):
   ```
   cd clover-idea-libs
   mvn -Pworkspace-setup generate-resources -f pom.xml
   ```
   After extraction inspect `target/extract/` to confirm directory name and build number.

3. **Generate install-file executions** and replace the block between the `<!-- Generated -->` 
   comments in `clover-idea-libs/pom.xml`:
   ```
   cd clover-idea-libs
   bash generate-install-file-calls.sh target/extract/idea-IC-BUILD BUILD > /tmp/install-calls.xml
   ```
   Replace content between `<!-- Generated automatically by ./generate-install-file-calls.sh -->`
   and `<!-- End of generated ... -->` in `clover-idea-libs/pom.xml`.
   Also update `<version>BUILD</version>` in pom.xml header and both comments.

4. **Generate dependency list** and replace the block in `clover-idea-libs/clover-idea-all/pom.xml`:
   ```
   bash generate-idea-dependencies.sh target/extract/idea-IC-BUILD BUILD > /tmp/deps.xml
   ```
   Replace content between `<!-- Generated -->` / `<!-- End of generated -->` markers.
   Also update `<version>BUILD</version>` in pom header and comment.

5. **Install JARs to local Maven repo:**
   ```
   mvn -Pworkspace-setup install -f pom.xml
   ```

6. **Commit:**
   ```
   git add clover-idea-libs/pom.xml clover-idea-libs/clover-idea-all/pom.xml
   git commit -m "OC-68: add IDEA YEAR.MINOR libs (build BUILD)"
   ```

7. **Tag** (use three-digit tag form: `YEAR.MINOR.<BUILD_NODOTS>`):
   ```
   git tag clover-idea-libs-YEAR.MINOR.BUILDNODOTS
   ```
   Example: build `173.4321.50` → tag `clover-idea-libs-2017.3.173432150`

---

## Phase 2 — Bump clover-idea version and fix compilation

After all lib tags exist, bump `clover-idea` one IDEA version at a time.

### Steps per bump

1. **Update `clover-idea/pom.xml` properties:**
   ```xml
   <idea.version>BUILD</idea.version>
   <idea.version.short>YEAR.MINOR.PATCH</idea.version.short>
   ```

2. **Compile:**
   ```
   mvn compile -pl clover-idea -am -Didea.version=BUILD
   ```

3. **Fix compilation errors.** Common categories:
   - Removed/renamed classes (use `javap -c` on the new lib JAR or the source archive)
   - Changed method signatures (add casts, update parameter types)
   - Deprecated API now removed
   - Plugin XML descriptor issues (EP names, extension points renamed)

4. **Run tests:**
   ```
   mvn test -pl clover-idea
   ```
   Fix failing tests; distinguish test-infra failures from real regressions.

5. **Commit per fix** with message `OC-68: fix compatibility with IDEA YEAR.MINOR - <brief description>`

6. **Repeat until all tests pass**, then move to next IDEA version.

---

## Key files

| File                                      | Purpose                                          |
|-------------------------------------------|--------------------------------------------------|
| `clover-idea-libs/pom.xml`                | Downloads IDEA, installs JARs; update per version|
| `clover-idea-libs/clover-idea-all/pom.xml`| BOM-style dep list consumed by clover-idea       |
| `clover-idea-libs/generate-install-file-calls.sh` | Generates install-file executions        |
| `clover-idea-libs/generate-idea-dependencies.sh`  | Generates `<dependency>` list            |
| `clover-idea/pom.xml`                     | `idea.version` property pins which lib set to use|

---

## Tagging convention (from existing tag)

Existing: `clover-idea-libs-2016.3.163155298`
- `2016.3` = IDEA major.minor version
- `163155298` = build `163.15529.8` with all dots removed (concatenated digits only)

So build `173.4321.50` → `clover-idea-libs-2017.3.173432150`
