# clover-idea-libs — current state and refactoring plan

## Current CI matrix (as of 2026-07-01)

Branch: `OC-109-support-idea-2019-to-2022`
Workflow: `.github/workflows/B-idea-compatibility-tests.yml`

Versions older than 2024 were dropped (IDEA 2019–2022 had unfixable test failures due to
`com.intellij.java` plugin not loading in test mode; not worth maintaining).

| IDEA version | Build number   | Tag (historical, pre-refactor) | JDK |
|--------------|----------------|--------------------------------|-----|
| 2024.3.7.1   | 243.28141.41   | clover-idea-libs-2024.3.2432814141 | 21 |
| 2025.3.6     | 253.33813.25   | clover-idea-libs-2025.3.2533381325 | 21 |
| 2026.1.3     | 261.25134.95   | clover-idea-libs-2026.1.2612513495 | 21 |

**Note on archive naming:** 2025+ archives use `idea-*.tar.gz` prefix (no `IC`), extracted as `idea-IU-*`.
**Note on devkit plugin:** Not present in 2025/2026 IU archives; the generate scripts skip it automatically.

---

## Why the current design is painful

1. `clover-idea-libs/pom.xml` contains ~400 hardcoded `install:install-file` executions, one per JAR.
2. `clover-idea-all/pom.xml` contains ~400 hardcoded `<dependency>` entries.
3. Both poms carry the IDEA build number as `<version>`.
4. Every new IDEA version requires: regenerate snippets → manually splice into poms → commit → create git tag.
5. Changing pom structure on an existing tag requires force-pushing that tag.
6. The two `generate-*.sh` scripts produce **snippets only** — not complete poms — requiring manual splicing.
7. CI uses `git show TAG:pom.xml > pom.xml` to restore the correct pom for each IDEA version.

---

## Refactoring plan — Plan C (approved 2026-07-01)

### Goal

`mvn install -Pworkspace-setup -Didea.version=261.25134.95 -Didea.download.file=idea-2026.1.3.tar.gz -Didea.download.dir=idea-IU-261.25134.95`

…generates and installs everything from the current branch. No git tags, no manual splicing,
no per-version pom commits.

### Key decision — how to install ~400 individual JARs

| Option | Mechanism | Decision |
|--------|-----------|----------|
| A | `mvn install:install-file` subprocess per JAR | Too slow — ~400 JVM startups |
| B | Copy directly into `~/.m2` via antrun | Bypasses checksums/metadata — fragile |
| **C** | **Generate helper pom + exec-maven-plugin** | **Chosen — one Maven process, correct semantics** |

Plan C: generate `target/generated/install-helper.pom` with all `install:install-file` executions, then
`exec-maven-plugin` runs `mvn -f target/generated/install-helper.pom install` in one process.

### Scripts → output files

| Script | Current output | New output |
|--------|---------------|------------|
| `generate-install-file-calls.sh` | XML snippet (pasted into pom) | `target/generated/install-helper.pom` — **full standalone Maven pom** |
| `generate-idea-dependencies.sh` | XML snippet (pasted into pom) | `target/generated/clover-idea-all.pom` — **full standalone BOM pom** |

Both scripts keep the same arguments: `<idea-dir> <idea-version>`.

### `clover-idea-libs/pom.xml` changes

- Remove all ~400 hardcoded `install:install-file` executions.
- Remove IDEA build number from `<version>` — use `${project.parent.version}` (5.0.0-SNAPSHOT).
- Add default properties: `idea.version`, `idea.download.file`, `idea.download.dir`,
  `idea.download.site`, `idea.download.path` (all overridable via `-D`).
- `workspace-setup` profile after extraction:
  1. `exec-maven-plugin` (or antrun exec) runs `generate-install-file-calls.sh` → `target/generated/install-helper.pom`
  2. `exec-maven-plugin` (or antrun exec) runs `generate-idea-dependencies.sh` → `target/generated/clover-idea-all.pom`
  3. `exec-maven-plugin` runs `mvn -f target/generated/install-helper.pom install` (installs all JARs)
  4. `exec-maven-plugin` runs `mvn -f target/generated/clover-idea-all.pom install` (installs BOM)
  5. Existing `install-idea-zip` execution stays (installs idea.zip — already static).

### `clover-idea-all/` directory

- `clover-idea-all/pom.xml` **deleted from git** — it is generated ephemerally to `target/generated/clover-idea-all.pom` and installed to `~/.m2`.
- Remove `<module>clover-idea-all</module>` from `clover-idea-libs/pom.xml`.
- `clover-idea/pom.xml` is **unchanged** — it depends on `clover-idea-all` at `${idea.version}` from `~/.m2`.

### CI workflow changes

Remove:
```yaml
- name: Prepare IDEA ${{ matrix.idea-version }} libs
  run: |
    git show clover-idea-libs-${{ matrix.idea-libs-tag }}:clover-idea-libs/pom.xml \
      > clover-idea-libs/pom.xml
    git show clover-idea-libs-${{ matrix.idea-libs-tag }}:clover-idea-libs/clover-idea-all/pom.xml \
      > clover-idea-libs/clover-idea-all/pom.xml
```

Replace with direct `mvn install -Pworkspace-setup` passing download properties via `-D`.
Remove `idea-libs-tag` from the matrix; add `idea-download-file` and `idea-download-dir` columns.
No more tags to create after each version.

### Files to touch

| File | Change |
|------|--------|
| `clover-idea-libs/pom.xml` | Remove install-file executions; add generation + exec-maven-plugin steps |
| `clover-idea-all/pom.xml` | **Delete from git** |
| `generate-install-file-calls.sh` | Rewrite to emit a full standalone pom to `target/generated/install-helper.pom` |
| `generate-idea-dependencies.sh` | Rewrite to emit a full standalone pom to `target/generated/clover-idea-all.pom` |
| `.github/workflows/B-idea-compatibility-tests.yml` | Remove tag restore steps; add download props to matrix |

### What stays unchanged

- `clover-idea/pom.xml` — depends on `clover-idea-all` at `${idea.version}` (from `~/.m2`)
- The `idea.zip` install execution in `clover-idea-libs/pom.xml`
- The set of JARs scanned: `lib/*.jar` + `plugins/java`, `devkit`, `properties`

---

## Source archives (for resolving API compatibility without decompiling)

| IDEA version | GitHub source archive |
|--------------|-----------------------|
| 2024.3.x     | `https://github.com/JetBrains/intellij-community/archive/idea/243.28141.41.zip` |
| 2025.2.x     | `https://github.com/JetBrains/intellij-community/archive/idea/252.28539.54.zip` |

---

## Key files

| File | Purpose |
|------|---------|
| `clover-idea-libs/pom.xml` | Downloads IDEA, drives workspace-setup; will become fully static after refactor |
| `clover-idea-libs/generate-install-file-calls.sh` | Generates `install-helper.pom` (full standalone pom post-refactor) |
| `clover-idea-libs/generate-idea-dependencies.sh` | Generates `clover-idea-all.pom` (full standalone BOM post-refactor) |
| `clover-idea/pom.xml` | `idea.version` property pins which lib set to use |
| `.github/workflows/B-idea-compatibility-tests.yml` | CI matrix, one job per IDEA version |
