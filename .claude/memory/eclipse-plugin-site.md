---
name: eclipse-plugin-site
description: Plan to migrate the Eclipse update site from legacy site.xml to a p2 repository (OC-134)
metadata:
  type: project
---

# Migrate Eclipse update site: legacy `site.xml` → p2 repository (OC-134)

Issue: https://github.com/openclover/clover/issues/134 — milestone 5.1.0.
Requests: (1) publish a proper **p2 repository** (`content.xml`/`artifacts.xml` +
optional `p2.index`, `compositeContent.xml`) generated instead of the old
Update-Manager `site.xml`; (2) serve it over **HTTPS** at `https://openclover.org/update/`.
Motivation: Eclipse mirroring bug 564767 breaks corporate mirroring of the legacy site.

## Current state (as of branch OC-134-eclipse-p2-plugin-site)

Module: `clover-eclipse/org.openclover.eclipse.updatesite` (`packaging: pom`).

Key facts about how the Eclipse build works today — **this is NOT a Tycho build**:
- Plugins (`org.openclover.eclipse.core`, `.ant`, `.branding`, `.runtime`,
  `.testopt`) are built as **plain Maven JAR artifacts** with **hand-written OSGi
  headers** injected via `maven-jar-plugin` `<manifestEntries>` (see
  `org.openclover.eclipse.core/pom.xml` ~line 111: `Require-Bundle`, etc.).
- Features (`org.openclover.eclipse.feature`, `.ant.feature`) are JAR artifacts
  built by regular Maven with a filtered `feature.xml` in resources.
- The update site is assembled by `maven-assembly-plugin` using
  `src/assembly/clover-eclipse-site.xml`: copies plugin JARs → `plugins/`,
  feature JARs → `features/`, filtered `site.xml` + `README.html` + `LICENSE.txt`
  + `licenses/` into the root, zips it as `clover-eclipse-site-<version>.zip`.
- `src/main/resources/site.xml` is the legacy Update-Manager descriptor
  (`<site><feature .../><category-def/></site>`), version-filtered via
  `${project.version}`.
- `maven-deploy-plugin` and `maven-jarsigner-plugin` are skipped here (zip is
  uploaded to website, not Maven Central).
- Version pattern: `5.0.2.v20260720000000` (OSGi-style `major.minor.micro.qualifier`).

A p2 repo needs `content.xml`/`artifacts.xml` (usually jar-compressed:
`content.jar`/`artifacts.jar`) describing installable units for the bundles and
features. The legacy `site.xml` alone does not provide this; modern Eclipse
(Oiomon/2020-06+) increasingly assumes p2.

## Chosen approach: `org.reficio:p2-maven-plugin` (DECIDED)

Do **not** rewrite the Eclipse build into Tycho — plugins already produce correct
OSGi JARs. Use `org.reficio:p2-maven-plugin` (goal `p2:site`, latest `3.0.0`):
pure Maven-3 plugin, no external Equinox runtime to bootstrap, generates the full
p2 repo (`content.jar`, `artifacts.jar`, `p2.index`, `plugins/`, `features/`)
**including categories in one goal** into `target/repository/`. Our JARs are
already valid OSGi bundles, so the bnd wrapping step is a no-op (use
`transitive=false`, list the reactor artifacts by GAV).

`FeaturesAndBundlesPublisher` (Eclipse native) was considered and rejected: it
needs an Equinox launcher jar bootstrapped into this non-Tycho build and a
separate `CategoryPublisher` pass — more friction for no benefit here.

### Decisions locked in
- **Drop `site.xml` entirely** — go p2-only. Oldest supported Eclipse is the 2024
  releases, which have full p2 support; no legacy Update-Manager client to keep.
- **Sign the JARs** — yes. jarsigner is already configured (see below).
- **Publishing** — keep BOTH the zip download AND the unpacked directory tree
  served at `https://openclover.org/update/`, exactly as now.
- **Composite repo** — yes, add `compositeContent.xml` + `compositeArtifacts.xml`.
  `https://openclover.org/update/` already hosts multiple versions (4.2.0 → 5.0.1);
  the composite lets one URL aggregate all per-version child repos.

## JAR signing — how it works today (relevant, since we enable it)

Signing is already wired in `clover-eclipse/pom.xml` and inherited by every
Eclipse module (`maven-jarsigner-plugin` `sign` goal, bound to `package`):
- Off by default: `clover.jarsigner.skip=true`; releases run with
  `-Dclover.jarsigner.skip=false`.
- Keystore props: `clover.keystore.path` (`~/.openclover/openclover.jks`),
  `.alias`, `.storepass`, `.keypass`.
- Version `3.0.0` is pinned in `clover-base-pom` pluginManagement (`pom.xml`:327).

So the 5 plugin + 2 feature JARs are **already signed at their own `package`
phase** when releasing. Key ordering requirement: p2-maven-plugin records SHA/size
of each artifact into `artifacts.jar`, so it MUST consume the **signed** JARs.
Because `updatesite` is a downstream reactor module that resolves the already-built
(and thus already-signed) artifacts, this holds — but verify the resolved JARs
carry `META-INF/*.RSA`/`.SF` before publishing. The `updatesite` module itself
keeps jarsigner skipped (its output is a zip, not a bundle jar).

## Concrete task list

1. **Delete `src/main/resources/site.xml`.** Add a p2 `category.xml` instead (p2
   category descriptor: `<site><category-def name=... label=...>` p2 form, or the
   `p2-maven-plugin` `<featureDefinitions>`/`<categories>` config). Version-filter
   label via `${project.version}` → `maven-resources-plugin` (filtering already on).
2. Add `p2-maven-plugin` to `updatesite/pom.xml`, goal `p2:site` bound to a phase
   before `package`. Configure `<artifacts>` listing the 5 bundles + 2 features by
   GAV (`org.openclover:org.openclover.eclipse.core:${project.version}`, etc.),
   `transitive=false`, and the category grouping "OpenClover ${project.version}".
   Output lands in `target/repository/`.
3. Ensure signed JARs feed p2: run the reactor with
   `-Dclover.jarsigner.skip=false` for releases; confirm the artifacts p2 resolves
   are the signed ones (check `META-INF` for signature files).
4. Rework packaging: replace the current hand-rolled assembly layout so the ZIP is
   produced FROM `target/repository/` (the real p2 repo dir), keeping
   `README.html`, `LICENSE.txt`, `licenses/`. Update
   `src/assembly/clover-eclipse-site.xml` to zip `target/repository/` +
   the license/readme files. Result: `clover-eclipse-site-<version>.zip` is now a
   valid p2 repo archive (an "archived update site").
5. **Composite repo**: generate/maintain `compositeContent.xml` +
   `compositeArtifacts.xml` at the root of `https://openclover.org/update/` listing
   each per-version child dir (4.2.0 … 5.0.1 … new version). Decide whether to
   generate these in the build (e.g. a small template committed to the module and
   updated per release) or manage them as a website-side artifact. Each release
   uploads its p2 repo to `update/<version>/` and appends a child entry.
6. **Publishing / HTTPS**: unpack the new p2 zip into `update/<version>/` on the
   website (served as a directory tree — users add the composite URL in Eclipse
   "Install New Software"), AND keep offering the zip as a download, as today.
7. Test: point a clean Eclipse (2024+) at (a) the local `file:target/repository`,
   (b) the staged `https://…/update/<version>/`, and (c) the composite
   `https://…/update/`. Install OpenClover + Ant Support features, verify category
   grouping, signed-jar trust prompt, and upgrade path. Verify mirroring works
   (the original bug 564767 scenario).

Related: [[eclipse-functional-testing]], [[eclipse-libs-installers]].
