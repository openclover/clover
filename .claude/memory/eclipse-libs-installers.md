# Eclipse Libs Installer Plan

## Purpose

Convert each major "Eclipse for Java Developers" release into a committed+tagged `clover-eclipse-libs` pom.xml with JARs installed locally in `.m2`. These snapshots form a compatibility matrix for building the Clover Eclipse plugin against different Eclipse versions.

## How it works

`clover-eclipse-libs/pom.xml` has a `workspace-setup` profile that:
1. Downloads the Eclipse win32-x86_64 zip from `${eclipse.download.site}/${eclipse.download.path}/${eclipse.download.file}`
2. Extracts all JARs flat into `target/extract/`
3. Installs each JAR as a Maven artifact under `org.openclover.eclipse.libs` group

Steps for each version:
1. Update `pom.xml`: set `<version>`, `eclipse.download.site`, `eclipse.download.path`, `eclipse.download.file`
2. Run `mvn generate-resources -P workspace-setup` to download and extract JARs
3. Run `./get-plugin-versions.sh` to get updated `<*.version>` property values, paste into pom.xml
4. Run `mvn install -P workspace-setup` to install JARs into local `.m2`
5. Commit: `git commit -m "eclipse-libs: install Eclipse {version}"`
6. Tag: `git tag clover-eclipse-libs-{version}`

## URL patterns

**Date-based versions** (2020 onwards):
```
site:  https://archive.eclipse.org  (recent: use a live mirror, e.g. https://ftp.fau.de)
path:  eclipse/technology/epp/downloads/release/{YYYY-MM}/{R|1|2}
file:  eclipse-java-{YYYY-MM}-{R|1|2}-win32-x86_64.zip
```
Use the highest-numbered service release available for each cycle (check archive.eclipse.org directory listing). Most releases only have `R`.

## Naming conventions

Dotted `YYYY.MM` for pom `<version>` and git tag, consistent with the existing `clover-eclipse-libs-2021.12` tag.

| pom `<version>` | git tag |
|---|---|
| `2021.12` | `clover-eclipse-libs-2021.12` |
| `2024.06` | `clover-eclipse-libs-2024.06` |

## Version list — one annual June SimRel per year, from 2020

Eclipse has no formal LTS. The June SimRel is the "blessed" annual release — most tested and most widely referenced.

| # | Release | pom version | Download subpath | Status |
|---|---|---|---|---|
| 1 | 2020-06 | `2020.06` | `2020-06/R` | **DONE** (tag `clover-eclipse-libs-2020.06`) |
| 2 | 2021-06 | `2021.06` | `2021-06/R` | **DONE** (tag `clover-eclipse-libs-2021.06`) |
| 3 | 2021-12 | `2021.12` | `2021-12/R` | **DONE** (tag `clover-eclipse-libs-2021.12`) — kept as bonus; superseded by 2021-06 for the annual slot |
| 4 | 2022-06 | `2022.06` | `2022-06/R` | **DONE** (tag `clover-eclipse-libs-2022.06`) |
| 5 | 2023-06 | `2023.06` | `2023-06/R` | **DONE** (tag `clover-eclipse-libs-2023.06`) |
| 6 | 2024-06 | `2024.06` | `2024-06/R` | **DONE** (tag `clover-eclipse-libs-2024.06`) |
| 7 | 2025-06 | `2025.06` | `2025-06/R` | **DONE** (tag `clover-eclipse-libs-2025.06`) |
| 8 | 2026-03 | `2026.03` | `2026-03/R` | **DONE** (tag `clover-eclipse-libs-2026.03`) — used fau.de mirror (not yet on archive.eclipse.org) |

> The existing `luna-sr2` tag and the `photon-R` / `2024-03-R` commits are retained in git history but are not part of the active compatibility matrix.

## Notes

- The `get-plugin-versions.sh` script reads from `target/extract/` — run it after the unzip step but before `mvn install`.
- Recent releases (2023+) may no longer be on `archive.eclipse.org` — check a live mirror (e.g. `https://ftp.fau.de/eclipse/technology/epp/downloads/release/`) or `download.eclipse.org` first.
- The `clover-eclipse-libs` pom `<version>` in HEAD is `luna-sr2`. The tagged commits are the deliverables; HEAD version is not meaningful.
- The SWT artifact name (`org.eclipse.swt.win32.win32.x86_64`) is hardcoded; if a newer Eclipse renames it, `get-plugin-versions.sh` may need updating.
