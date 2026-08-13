# OC-326: Migrate iText 2.0.1 → Apache PDFBox 3.x

Issue: https://github.com/openclover/clover/issues/326
Branch: `OC-326-migrate-itext-to-pdfbox`

## Context

`com.lowagie:itext:2.0.1` (released 2007) is flagged by Dependabot alert #11 —
CVE-2017-9096 (XXE, high), vulnerable range `<= 4.2.2`, **no patched version exists** for
this GA. iText 5+ is AGPL, so upgrading in place is impossible; the coordinate can only be
retired. OpenPDF (the LGPL/MPL fork) would be a near-zero-effort swap but its Java 8 branch
is EOL (1.3.43, Mar 2024), and it is not Apache-2.0. Decision: move to **Apache PDFBox 3.x**
(Apache-2.0, ASF-maintained, still Java 8) and accept the cost of reimplementing the layout
features iText provided.

## Constraints

- **Java 8.** `pom.xml:352-353` and `clover-core/pom.xml:123-124` pin source/target to 1.8.
  PDFBox 3.0.8 supports Java 8; this is the reason PDFBox is viable and OpenPDF 3.x is not.
- **Shaded jar.** `clover-core` shades its dependencies (`clover-core/pom.xml:336-347`).
  Every new artifact must be added to `<artifactSet>` and given a `<filter>`. Watch jar size.
- **Layout must be preserved.** Minor drift in colours/margins/kerning is acceptable; the
  page structure (tables, columns, bars, charts, footer) must look the same.
- Existing behaviour that must survive: A4 + LETTER page sizes, `--bw` monochrome palette,
  user-defined column sets, historical reports with JFreeChart graphs, movers/added tables,
  clickable title and footer links, "Page X of Y" footer.

## Current code (all in `clover-core`, ~1000 lines)

| File | Role |
|---|---|
| `reporters/pdf/PDFReporter.java` | `Document` lifecycle, page size, `PdfWriter`, report flow |
| `reporters/pdf/RenderingSupport.java` | all `PdfPTable` construction + 2 `PdfPTableEvent` painters |
| `reporters/pdf/PageFooterRenderer.java` | `PdfPageEventHelper` footer, logo, page numbering |
| `reporters/pdf/PDFColours.java` | `java.awt.Color` palettes — **no iText dependency, unchanged** |
| `util/format/PDFFormatter.java` | dead code, no callers — **delete** |

## Table engine: audited scope

PdfPTable is a large API; Clover uses a narrow slice of it. Audited against actual call
sites, the engine must support **only**:

- **relative column widths** — `setWidths(int[])` / `setWidths(float[])`, always relative
  proportions, never absolute points;
- **table width** — either a percentage of the available width (`setWidthPercentage`, 7 call
  sites) or an absolute width (`setTotalWidth`, footer only). Note iText's default is **80%**
  and several tables rely on it implicitly (`createHeaderStats`, `leftTab`, the bar tables) —
  replicate that default or those blocks shift;
- **colspan** (`createMoversTable`, `createAddedTable`, `createCoverageDataHeader`, both
  historical headers);
- **per-edge borders** — `NO_BORDER`, `BOX`, `TOP|BOTTOM`, `TOP|BOTTOM|RIGHT` — plus border colour;
- **cell background colour**, **padding** (uniform and left-only), **minimum row height**
  (one site: the chart cell), **leading** (`setLeading(2, 0.9f)`, 4 sites);
- **alignment** — horizontal left/center/right, vertical middle (one site);
- **nested tables**;
- **page breaking on row boundaries** — the package coverage table does overflow.

Confirmed **not** used, and therefore out of scope: `setHeaderRows` (no repeated headers on
page breaks — a meaningful simplification), `setSpacingBefore/After`, `setKeepTogether`,
`setSplitRows`/`setSplitLate`, `setExtendLastRow`, `setRunDirection`, rowspan, cell rotation,
images as cell content.

## What PDFBox does not provide

PDFBox is a low-level PDF object model. Everything below exists in iText and must be built.

1. **The table engine** above — the largest piece.
2. **Content flow / pagination.** `document.add(...)` auto-measures and breaks across pages.
   PDFBox has no cursor concept — the engine must measure block height and decide breaks.
3. **Rich text runs.** `Phrase`/`Chunk` mix fonts, sizes, styles and colours in one flowing
   paragraph with leading and embedded `\n`, wrapped to the cell width. Needs a small
   run-based text layouter over `PDFont.getStringWidth`.
4. **Link annotations.** `Chunk.setAnchor(url)` becomes `PDAnnotationLink` + `PDActionURI`,
   which requires the resolved on-page rectangle of the text run — so the layouter has to
   report back geometry.
5. **Page events.** No `PdfPageEventHelper` equivalent; replace with explicit
   open/end-page/close hooks invoked by our own document wrapper.
6. **Deferred "Page X of Y".** iText patches a `PdfTemplate` at close. PDFBox: keep the
   `PDDocument` in memory and stamp footers in a post-pass before save, which is simpler.

Maps cleanly, **not** a risk: page sizes (`PDRectangle.A4`/`LETTER`), form XObjects
(`PDFormXObject`), images (`PDImageXObject`), vector drawing (`PDPageContentStream`
rect/fill/stroke), metadata (`PDDocumentInformation`), font metrics.

**JFreeChart embedding** uses `PdfContentByte.createGraphics(w, h, DefaultFontMapper)`.
Replacement: `de.rototor.pdfbox:graphics2d:3.0.3` (Apache-2.0, tracks PDFBox 3.x), which
supplies a `Graphics2D` writing into a `PDFormXObject`. Configure it with
`PdfBoxGraphics2DFontTextForcedDrawer` so chart labels become vector outlines — removes all
font-mapping risk inside charts at the cost of slightly larger output.

## Widgets instead of table events

Today the coverage bars and the chart are painted through `PdfPTableEvent` — a whole-table
callback that receives raw geometry arrays and indexes into them by column
(`PCBarRenderer.column`, the `##HACK` comments). To attach one, `createPCBar` builds a
throwaway single-cell table purely as an event host.

Replace this with **widgets as first-class cell content**: a `PdfWidget` renders itself into
a rectangle the engine has already resolved.

```
interface PdfWidget { float preferredHeight(); void draw(PdfCanvas canvas, PdfRect bounds); }
```

- `CoverageBarWidget` — covered/uncovered/NA bar; absorbs `PCBarRenderer` and lets
  `createPCBar` return a widget instead of a nested table (one nesting level disappears).
- `CoverageDiffBarWidget` — the movers bar, which today abuses a 2-column table whose column
  widths encode the diff magnitude and whose text sits in one cell while the bar is painted
  across both. As a widget it draws bar and label itself against a single rectangle, and the
  `##HACK` argument-order inversion between gainers and losers goes away.
- `ChartWidget` — wraps the `Graphics2D` bridge; replaces `GraphRenderer` and the nested
  `chartTable`.

This removes the `PdfPTableEvent` seam from the API entirely, and drops the widget-hosting
nested tables — so nesting is left with only the genuine cases (`leftTab` inside `titlebar`
in both historical headers).

## Fonts and encoding

Today: Helvetica everywhere, plus `BaseFont.createFont(HELVETICA, BaseFont.CP1252, NOT_EMBEDDED)`
at `PageFooterRenderer.java:62`. CP1252 silently mangles anything outside Latin-1 — package
names, project titles and user-supplied report titles can all contain non-Latin-1 text.

**Embed a TrueType font as `PDType0Font` (Identity-H, full Unicode).** The Standard-14 fonts
are free but locked to WinAnsiEncoding, i.e. they would reproduce the exact CP1252 defect we
are removing. `PDType0Font.load(doc, stream, true)` subsets on save, so the output PDF grows
only by the glyphs actually used.

**Styles needed: 3.** Audited usage across the two surviving classes is `Font.BOLD` (30
sites) and `Font.ITALIC` (11 sites) plus plain — so Regular, Bold, Italic. `BOLDITALIC` and
`UNDERLINE` appear only in `PDFFormatter`, which is being deleted.

**No monospaced font.** Dropped from scope to save jar size; the PDF reporter renders no
source listings, so nothing needs it. The API keeps a font-family enum so a `MONO` family can
be added later without an interface change.

**Font choice — Liberation Sans (SIL OFL 1.1), 3 styles.** It is metric-compatible with
Arial, which is itself metric-compatible with Helvetica, so text widths and line breaks stay
close to today's output — directly serving the "layout must be preserved" constraint.
Estimated ~1.0 MB for three styles; **verify actual file sizes when implementing**. If that
proves too heavy, **Roboto (Apache-2.0)** is roughly half the size and better aligned with
the project licence, at the cost of narrower metrics and therefore visible reflow.

## Abstraction layer (keep)

New package `org.openclover.core.reporters.pdf.api` — deliberately thin, shaped to exactly
what Clover uses, not to PDF in general. Implementation in
`org.openclover.core.reporters.pdf.pdfbox`. Nothing outside the impl package imports PDFBox.

| Type | Purpose |
|---|---|
| `PdfDocumentFactory` | the single seam; `create(File, PageSize, PageDecorator)` |
| `PdfDocument` | `add(PdfBlock)`, `newPage()`, `setTitle/setCreator`, `close()` |
| `PdfBlock` | anything addable; `PdfTable` is the only implementation today |
| `PdfTable` | relative column widths, width as % or absolute, default-cell state, `addCell`, nesting |
| `PdfCell` | colspan, per-edge borders, padding, background, alignment, min height, content |
| `PdfText` | list of `PdfTextRun` (text + `PdfFontSpec` + optional anchor URI) |
| `PdfFontSpec` | family (`SANS`, extensible), size, style (regular/bold/italic), colour |
| `PdfWidget` | self-drawing cell content — coverage bars, chart |
| `PdfCanvas` / `PdfRect` | `rect/fill/stroke/setLineWidth/drawImage/drawGraphics2D` for widgets |
| `PdfPageDecorator` | `onPageStart/onPageEnd/onDocumentEnd` — replaces `PdfPageEventHelper` |

**Pro:** isolates a future migration to one package; lets the reporter classes be rewritten
once against a stable API; makes the layout engine unit-testable without asserting on PDF
bytes. **Con:** extra surface — since PDFBox needs a bespoke layout engine anyway, the
interfaces buy portability, not less work. Decision: keep it.

## Work breakdown

1. **Dependencies.** Drop `com.lowagie:itext` from root `pom.xml:128-135`, `clover-core/pom.xml:77-80`,
   `clover-all/pom.xml:90-93`, `clover-groovy/pom.xml:196-199`, and the shade
   `<include>`/`<filter>` in `clover-core/pom.xml`. Add `org.apache.pdfbox:pdfbox:3.0.8`
   (pulls `fontbox`, `commons-logging`) and `de.rototor.pdfbox:graphics2d:3.0.3` with
   matching shade entries. Remove the stale `itext.ver` property in
   `clover-core-libs/versions.xml:9`.
2. **API package** — the interfaces above, no logic.
3. **PDFBox impl** — font resolver + text-run layouter first, then the table engine, then
   page flow, widgets and the decorator hooks. Largest single piece; build it against small
   throwaway mains before wiring the reporter.
4. **Rewrite the three reporter classes** against the API. `RenderingSupport`'s structure
   (one static factory per report element) carries over almost unchanged — only the types
   differ. `PDFColours` is untouched.
5. **Delete** `util/format/PDFFormatter.java` (unused).
6. **Licensing/attribution:**
   - remove `clover-core/src/main/resources/licenses/ITEXT-2.0.1-LICENSE.TXT`;
   - add `PDFBOX-3.0.8-LICENSE.TXT`, `FONTBOX-3.0.8-LICENSE.TXT`,
     `PDFBOX-GRAPHICS2D-3.0.3-LICENSE.TXT` and `LIBERATION-FONTS-LICENSE.TXT` (SIL OFL 1.1);
   - `clover-third-party-library-bom.html:145-155` — replace the iText row, and **delete the
     red "do not upgrade above 2.1.7" warning**, which no longer applies to anything;
   - `clover-idea/.../AboutDialog.java:106` and
     `clover-eclipse/.../ShowAboutCloverActionDelegate.java:142` — swap the `iText` entries
     for PDFBox (+ the bundled font). The Eclipse plugin resolves licence files from its own
     `licenses/` copy, so the new files must land there too;
   - `PDFReporter.java:108` writes `"... using iText v2.0.1"` into the PDF Creator field —
     update to PDFBox and its version.
7. **`pom.xml:129-131` comment** — delete the iText/AGPL warning block.

## Tests

**New permanent unit test — content assertions.** PDFBox can read its own output, so no
extra dependency: generate a report from a small in-memory registry and run
`PDFTextStripper` over it, asserting on extracted text — "OpenClover Coverage Report", the
report title, the `Packages` column header, `Branch`/`Stmt`/`Method`/`Total`, the rendered
percentage strings for a known fixture, and the footer "Page 1 of 1". Add an equivalent for
the historical report (title, From/To dates, movers headings). This is the coverage the
suite has never had — `PDFReporterTest.groovy` only exercises the *no-data* paths. Also fix
the inverted `assertFalse(... == expectedReturnValue)` at `PDFReporterTest.groovy:36` while
in there.

Also assert Unicode round-trips: a report title with non-Latin-1 and CJK characters must
come back intact through `PDFTextStripper` — this is the regression test for the CP1252 fix.

**Existing functional test** `CloverReportTaskTest.groovy:176-199` only asserts the files
exist; keep it green as a smoke test.

**Visual regression — one-time, not committed.** Work in a scratchpad, discard afterwards.

1. Fixture: `CloverReportTaskTest.testPdfReports` and its Ant targets in
   `tests-functional/.../clover-report.xml:428-496`. It already generates exactly the matrix
   needed — `current.pdf` (bar-graph columns), `current-defaults.pdf` (default columns),
   `current-nonempty.pdf` (`showEmpty=false`), and `historical.pdf` off an existing history
   dir, so charts and movers/added tables are covered. Preferred over the `tutorial` project,
   which has no history points. `CloverReportTaskSanityTest` is a thinner smoke path and is
   not needed here.
2. Determinism: pin the coverage and generation timestamps, and temporarily hardcode
   `DonationMessageGenerator.pickMessage()` to a single message (comment out the rest) in
   **both** builds — it is random and would otherwise make every footer differ.
3. Generate from `master` (iText) and from this branch, adding A4/LETTER and colour/`--bw`
   variants to the four reports above.
4. Rasterise at 150 dpi (`pdftoppm -r 150 -png`) and diff per page
   (`compare -metric AE -fuzz 5%`), then eyeball a side-by-side contact sheet. Judge on
   structure: table/column positions, bar lengths, chart placement, footer geometry, page
   count and page breaks. Colour and sub-point margin drift is acceptable.
5. Manual checks the diff cannot make: click every link (title anchor, footer OpenClover and
   donation links).
6. Put the contact sheets in the PR description, then discard the scripts and revert the
   `DonationMessageGenerator` hack.
