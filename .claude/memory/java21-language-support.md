# Java 18–21 Language Syntax Support — Report & Implementation Plan (OC-229)

## Context

OpenClover instruments Java source by parsing it with an ANTLR 2.x grammar
(`java.g` / `java-lexer.g`) and rewriting the token stream via `*Emitter` classes.
PR #233 (OC-229) already added **JDK 21 CI build plans** (`A-build-and-test-jdk21.yml`)
and README badges, so Clover *runs* on JDK 21. However, the **instrumenter's language
ceiling is still Java 17**: `SourceLevel` only defines `JAVA_8 … JAVA_17`, there are no
`javasyntax18..21` test resources, and the grammar's switch `case` labels accept only
constant expressions. Java 21 (LTS) finalized two new **syntax** features that current
Clover cannot parse/instrument correctly, so users on JDK 21 with modern syntax hit
parse failures or mis-instrumented branches.

**Goal:** extend the instrumenter to recognize and correctly instrument the *finalized*
Java 21 language syntax, add `SourceLevel` entries for Java 18–21, and add the test
scaffolding (compilation tests + resource fixtures) mirroring the Java 14–17 work.
Preview features are documented for reference but **not implemented** in this iteration.

---

## Part A — Language Reference: Java 18–21 syntax changes

Only **language/syntax** JEPs are listed. Runtime/API JEPs (Virtual Threads, Sequenced
Collections, ZGC, FFM, Scoped Values, Vector, Structured Concurrency) are **out of scope** —
they do not affect the parser.

### Java 18 (GA 2022-03) — no new/finalized language syntax
| JEP | Feature | Status | Parser impact |
|-----|---------|--------|---------------|
| 420 | Pattern Matching for `switch` | **2nd Preview** | none permanent; precursor to JEP 441 |

Java 18 introduced **no permanent** language changes. JEP 420 only iterated the preview
of switch pattern matching (guarded patterns via `&&` at this stage, later `when`).

### Java 19 (GA 2022-09) — no finalized language syntax
| JEP | Feature | Status | Parser impact |
|-----|---------|--------|---------------|
| 405 | Record Patterns | **Preview** | precursor to JEP 440 |
| 427 | Pattern Matching for `switch` | **3rd Preview** | replaces guard `&&` with `when`; precursor to JEP 441 |

### Java 20 (GA 2023-03) — no finalized language syntax
| JEP | Feature | Status | Parser impact |
|-----|---------|--------|---------------|
| 432 | Record Patterns | **2nd Preview** | precursor to JEP 440 |
| 433 | Pattern Matching for `switch` | **4th Preview** | precursor to JEP 441 |

### Java 21 (LTS, GA 2023-09) — TWO finalized language features + three preview

**FINALIZED (mainline, no flag) — must implement:**

- **JEP 441 — Pattern Matching for `switch`.** `case` labels may now be **type patterns**,
  may carry a **guard** (`when`), and `switch` may have a `case null`. Exhaustiveness is
  enforced by javac.
  ```java
  String f(Object obj) {
      return switch (obj) {
          case null        -> "null";
          case Integer i when i > 10 -> "big int " + i;
          case Integer i   -> "int " + i;
          case String s    -> "str " + s;
          default          -> "other";
      };
  }
  ```
- **JEP 440 — Record Patterns.** Deconstruct records in `instanceof` and `switch`,
  arbitrarily nested; supports `var` components.
  ```java
  record Point(int x, int y) {}
  record Line(Point from, Point to) {}

  if (obj instanceof Point(int x, int y)) { ... }

  switch (shape) {
      case Line(Point(var x1, var y1), Point(var x2, var y2)) -> ...;
      case Point p -> ...;
      default -> ...;
  }
  ```

**PREVIEW (need `--enable-preview`) — documented only, NOT implemented this iteration:**

- **JEP 430 — String Templates (Preview).** `STR."...\{expr}..."` template expressions
  (new token/lexer work; `STR`, `FMT`, `RAW` are template processors). *Withdrawn in later
  JDKs — a strong reason to defer.*
  ```java
  String s = STR."Hello \{name}, 1+1=\{1+1}";
  ```
- **JEP 443 — Unnamed Patterns and Variables (Preview).** `_` as an unnamed binding /
  variable / pattern component.
  ```java
  if (obj instanceof Point(int x, _)) { ... }
  for (var _ : list) { count++; }
  try { ... } catch (Exception _) { ... }
  ```
- **JEP 445 — Unnamed Classes and Instance Main Methods (Preview).** Implicit class +
  instance `void main()`. Affects the compilation-unit rule.
  ```java
  void main() { System.out.println("Hi"); }
  ```

**Bottom line for implementation:** the only permanent syntax to teach the parser is
**switch type/guarded/null patterns (JEP 441)** and **record deconstruction patterns
(JEP 440)**. Everything else 18–21 is either preview or non-language.

---

## Part B — Current state in the codebase (what exists / what's missing)

Grammar is **version-agnostic**: modern pseudo-keywords (`record`, `sealed`, `permits`,
`yield`) are lexed as `IDENT` and disambiguated by `isCurrentKeyword(...)` /
`isNextKeyword(...)` predicates — *not* gated by `SourceLevel`. `SourceLevel`/`LanguageFeature`
only influence recorder generation (`RecorderInstrEmitter.java:94`, LAMBDA check) and logging.

Key existing pieces (all under `clover-core/src/main/java/org/openclover/core/`):
- `instr/java/java.g` — parser. `patternMatch` rule (line ~3660) currently:
  `constantExpression (COMMA constantExpression)*` — **constants only, the core gap.**
  Arrow switch = `lambdaSwitchExpression` / `lambdaCase`; colon switch = `colonSwitchExpression`.
  `instanceof` patterns handled in `relationalExpression` (lines ~3279-3285).
- `instr/java/InstanceOfState.java` + `InstanceOfStateDetector.java` — state machine deciding
  when an `instanceof` introduces a pattern binding var (so it is *not* branch-instrumented);
  consumed by `ExpressionInfo.java:22`.
- `instr/java/CaseExpressionEntryEmitter.java` / `CaseExpressionExitEmitter.java` — arrow-switch
  case expression instrumentation (`{ R.inc(n); yield ... }`).
- `cfg/instr/java/SourceLevel.java` — enum `JAVA_8..JAVA_17` + `fromString()` cascade + `autoDetect()`.
- `cfg/instr/java/LanguageFeature.java` — `{LAMBDA, MODULES, TEXT_BLOCKS, RECORDS, SWITCH_EXPRESSIONS}`.
- `util/JavaEnvUtils.java` — **already** knows `JAVA_8..JAVA_25` for runtime detection (no change needed for detection).

Tests mirror this (under `clover-core/src/test/`):
- `groovy/org/openclover/core/JavaSyntaxNNCompilationTest.groovy` (3,4,5,7,8,9,10,11,14,15,16,17)
  extend `JavaSyntaxCompilationTestBase.groovy`; gate with
  `assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_NN))`; use
  `instrumentAndCompileSourceFile(...)`, `assertFileMatches(...)`, `executeMainClasses(...)`.
- `resources/javasyntaxNN/...` — one `.java` per feature (+ negative `...Failed/Invalid.java`).
- `groovy/org/openclover/core/instr/java/` — unit-level: `InstrumentationSwitchStatementsTest`,
  `InstanceOfStateTest`, `InstrumentationLambdasTest`, etc.

---

## Part C — Implementation plan (finalized features only)

### C1. Source-level plumbing — `cfg/instr/java/SourceLevel.java`
- Add enum constants `JAVA_18, JAVA_19, JAVA_20, JAVA_21` following the `JAVA_17` pattern
  (`newHashSet("NN")`, feature set).
- Attach new features **only to `JAVA_21`** (18–20 keep the Java-17 feature set, acting as
  version pass-through per decision).
- Extend `fromString()` cascade with `JAVA_21..JAVA_18` branches (highest first), above `JAVA_17`.
- No change needed to `autoDetect()` (delegates to `JavaEnvUtils`) or `isUnsupported()`.

### C2. `LanguageFeature.java` — add only if instrumentation must be gated
The grammar parses patterns regardless of level, so a new flag is **not required for parsing**.
Add a `PATTERN_MATCHING` (and/or `RECORD_PATTERNS`) constant **only if** C3/C4 introduce
instrumentation behavior that must differ by source level (e.g. suppressing branch
instrumentation on pattern bindings must be conditional). Default recommendation: **add
`PATTERN_MATCHING` to `JAVA_21`'s feature set** and reference it from the pattern-aware
instrumentation guard, keeping the change minimal and consistent with how `RECORDS`/`SWITCH_EXPRESSIONS`
are used. Skip if the pattern handling is unconditional and safe for all levels.

### C3. Grammar — `instr/java/java.g` (the core work)

Extend `case` label parsing to accept patterns (JEP 441 + 440). New rules needed:
- A shared, **recursive** pattern hierarchy reused by both switch labels and `instanceof`:
  - `pattern : recordPattern | typePattern | ...` where
    `typePattern : (annotation)* (FINAL)? typeSpec IDENT` (incl. `var`), and
    `recordPattern : typeSpec LPAREN (pattern (COMMA pattern)*)? RPAREN (IDENT)?` (nested).
- Extend the case-label rule (`patternMatch`) so a label element can be: existing
  `constantExpression`, **or** `null`, **or** `typePattern`, **or** `recordPattern`, each
  optionally followed by a guard `{ isNextKeyword("when") }? IDENT expression`.
- Extend `relationalExpression` `INSTANCEOF` (java.g:3279-3285) with a record-pattern
  alternative `(INSTANCEOF typeSpec LPAREN) => recordPattern`, reusing the shared rule.
- `when` stays a pseudo-keyword via `isNextKeyword("when")` (mirrors `yield`/`record`/
  `sealed`) — **no new lexer token**, identifiers named `when` keep working.

#### Refactoring findings (investigated in java.g)

**(a) The colon-vs-arrow switch selector is duplicated 4× with unbounded lookahead.**
The syntactic predicate `( SWITCH LPAREN expression RPAREN LCURLY (CASE ... | DEFAULT)
COLON/LAMBDA ) =>` appears at java.g:2541 & 2549 (`statement`) and 3527 & 3531
(expression path). Each must scan the **entire first case label** to reach the trailing
`COLON` vs `LAMBDA`. Today the label is a bounded `constantExpression`; after JEP 441 it
may be a guarded record pattern (`Foo(Bar(var x)) when x.test() && y > 0`), i.e. an
arbitrary expression — so every `switch` triggers backtracking over an unbounded region,
four times. This is the top performance + maintenance concern.
→ **Refactor (recommended, highest value):** collapse the four selectors into one entry
rule that matches the common prefix `SWITCH ( expr ) {` once, then commits to colon vs
arrow on the first label via a **bounded semantic predicate** (a `hasArrowSwitchLabels()`
helper that scans tokens up to the first `COLON`/`LAMBDA` at brace depth 0, no backtracking).
Removes 3 duplicated predicates and the unbounded backtracking. Riskiest change — gate with
the full `JavaSyntax14/16/17` + `InstrumentationSwitchStatementsTest` suite before/after.

**(b) Correctness gap, not just cleanup:** the **colon** selectors (2541/3531) hardcode
`CASE constantExpression`, but JEP 441 allows patterns in colon-form switches too
(`case Integer i:`). As written, a colon pattern-switch matches *neither* selector.
`constantExpression` must be replaced by the shared `patternMatch` rule in the colon
selectors **and** in `colonCase`/`colonCasesGroup` (java.g:2634-2668) — independent of any
refactor. `lambdaCase` (java.g:3616) already routes through `patternMatch`, so it only needs
the widened `patternMatch` body.

**(c) `instanceof` is the reuse template.** java.g:3279-3285 already disambiguates
pattern-binding vs plain type with cheap bounded predicates
(`(INSTANCEOF FINAL type IDENT) =>`, `(INSTANCEOF type IDENT) =>`, fallback `INSTANCEOF type`).
The new record-pattern rule should be shared between instanceof and switch rather than
duplicated; the `FINAL`/annotation handling folds into the shared `typePattern`.

**(d) Semantic > syntactic predicates for the new ambiguities (performance).** The grammar
already avoids ANTLR2 backtracking in hot spots via constant-lookahead helpers
(`isNextKeyword`, `isCurrentKeyword` used as `{...}?`). Follow that idiom for the new
decisions instead of `(...)=>`:
  - type pattern vs constant (`case Integer i` vs `case FOO`): `looksLikeTypePattern()`
    (peek type tokens + trailing binding IDENT before `,`/`:`/`->`/`when`);
  - record pattern (`case Foo(`): `looksLikeRecordPattern()` — the `LPAREN` after a type is a
    cheap, bounded discriminator;
  - `case null`: `NULL_LITERAL` is a distinct token — no predicate; only widen the comma-list
    to permit `null, default`.
  These keep decisions O(k) constant-lookahead rather than per-label backtracking, which is
  what makes the added grammar affordable on pattern-heavy switches.

**Performance bottom line:** naively adding `(...)=>` predicates for every new label form on
top of the existing 4 unbounded selectors would compound backtracking on every switch. Doing
(a) once + expressing (c)/(d) as bounded semantic predicates yields a net **neutral-to-faster**
parser versus today, while covering the new syntax.

### C4. Instrumentation / complexity semantics
- **Branch instrumentation:** pattern bindings and guards must not be treated as ordinary
  boolean branch expressions. Reuse / extend the `InstanceOfState` approach
  (`InstanceOfState.java`, `InstanceOfStateDetector.java`, `ExpressionInfo.java`) so that a
  pattern binding variable and nested record-pattern bindings are recognized and **not**
  branch-instrumented. A `when` guard *is* a real boolean expression → it **should** be
  branch-instrumented (adds cyclomatic complexity), consistent with OC-229 `50e33fb4`
  (switch-arg complexity fix).
- **Case coverage:** each `case` (including pattern cases) must still emit a statement/case
  recorder increment via the existing `CaseExpressionEntryEmitter`/`ExitEmitter` and colon/arrow
  case handling — verify these fire for pattern cases, extend if a pattern case is currently skipped.
- No new `*Emitter` class is anticipated; if guard instrumentation needs wrapping, reuse
  `StartBoolInstrEmitter`/`EndBoolInstrEmitter`/`FlagDeclEmitter`.

### C5. CLI/help surface (ceiling bump)
- `api/command/HelpBuilder.java` and `cmdline/CloverInstrArgProcessors.java` reference the
  supported `--source` range (per OC-219 `e37990d9`). Update any hardcoded "up to 17" text /
  validation to 21. (`SourceLevelArg` at `CloverInstrArgProcessors.java:~242` uses
  `SourceLevel.fromString`, so it works automatically once C1 lands — check only for hardcoded help text.)

### C5b. Eclipse & IDEA plugin source-level mappers (IDE → SourceLevel)

Both IDE plugins translate the IDE's own language-level setting into Clover's `SourceLevel`.
They must be extended, and they differ:

**Eclipse — string passthrough, works for free once C1 lands.**
`clover-eclipse/org.openclover.eclipse.core/.../projects/CloverProject.java:303`:
```java
config.setSourceLevel(SourceLevel.fromString(getJavaProject().getOption(JavaCore.COMPILER_SOURCE, true)));
```
`JavaCore.COMPILER_SOURCE` yields `"18"…"21"`, so `SourceLevel.fromString` resolves them
automatically after C1. `BuildCoordinator.java` (lines 311/436) likewise passes the raw
compliance string to javac. **No plugin code change required** — only add/verify a functest
fixture: `TestJava21Project/.settings/org.eclipse.jdt.core.prefs` (compliance=21) already
exists; add analogous 18/19/20 fixtures if 18–20 coverage is wanted.

**IDEA — explicit enum map, MUST be extended (this is the mapper to update).**
`clover-idea/src/jps/java/org/openclover/idea/build/jps/CloverJavaSourceTransformer.java:168-196`
has a `LANGUAGE_LEVEL_TO_SOURCE_LEVEL` map that stops at `JDK_17 → JAVA_17` and a hardcoded
fallback `return sourceLevel == null ? SourceLevel.JAVA_17 : sourceLevel;`. Changes:
```java
put(LanguageLevel.JDK_18, SourceLevel.JAVA_18);
put(LanguageLevel.JDK_19, SourceLevel.JAVA_19);
put(LanguageLevel.JDK_20, SourceLevel.JAVA_20);
put(LanguageLevel.JDK_21, SourceLevel.JAVA_21);
// raise the fallback ceiling:
return sourceLevel == null ? SourceLevel.JAVA_21 : sourceLevel;
```
- **Preview levels:** IntelliJ also exposes `JDK_NN_PREVIEW` and `JDK_X` (experimental).
  Map any `*_PREVIEW`/`JDK_X` to the highest known `SourceLevel` (JAVA_21) via the fallback,
  or add explicit entries — but only if the bundled IntelliJ SDK the plugin builds against
  actually defines those enum constants (verify; older SDKs may lack `JDK_21`).
- **Update the unit test** `clover-idea/src/test/.../jps/CloverJavaSourceTransformerTest.java:41-43`,
  which currently asserts `JDK_18`, `JDK_21`, `JDK_X` all map to `JAVA_17`. Change to
  `JDK_18→JAVA_18`, `JDK_19→JAVA_19`, `JDK_20→JAVA_20`, `JDK_21→JAVA_21`, and `JDK_X→JAVA_21`.
- No IDEA UI dropdown of levels needs editing — the level is auto-detected from the project
  via `collectLanguageLevels(...)`/prefix tree; `CloverPluginConfig.DEFAULT_LANGUAGE_LEVEL`
  ("9") is only a fallback default and can stay.

Note (from OC-285 `50e3f8f4` / OC-109): the Eclipse and IDEA modules build only under JDK21+
in CI, so these edits are exercised on the `A-build-and-test-jdk21.yml` plan.

Files: `CloverJavaSourceTransformer.java` (map + fallback), `CloverJavaSourceTransformerTest.java`
(assertions); Eclipse needs no code change (optional functest fixtures only).

### C6. Docs
- README already has JDK21 badge (PR #233). Add a note that instrumentation supports Java 21
  language level if any "supported source levels" doc/text exists.

### Files to touch (summary)
| File | Change |
|------|--------|
| `cfg/instr/java/SourceLevel.java` | add `JAVA_18..JAVA_21` + `fromString` branches |
| `cfg/instr/java/LanguageFeature.java` | add `PATTERN_MATCHING` (only if gating needed) |
| `instr/java/java.g` | patternMatch/recordPattern/guard rules; instanceof record patterns |
| `instr/java/InstanceOfState.java`, `InstanceOfStateDetector.java`, `ExpressionInfo.java` | recognize pattern bindings in switch + nested record patterns |
| `api/command/HelpBuilder.java`, `cmdline/CloverInstrArgProcessors.java` | help/validation text ceiling → 21 |
| tests + resources | see Part D |

---

## Part D — Test plan (what to write)

Follow the Java 16/17 pattern exactly.

### D1. Compilation tests (file-level, end-to-end)
New `clover-core/src/test/groovy/org/openclover/core/JavaSyntax21CompilationTest.groovy`
extending `JavaSyntaxCompilationTestBase`, gated with
`assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_21))`, `srcDir = javasyntax21`.
Test methods:
- `testSwitchTypePatterns` — `case Integer i` / `case String s` instruments each case, compiles,
  runs, and coverage model records each case hit (`assertStatementCoverage` / `executeMainClasses`
  + `assertExecOutputContains`).
- `testSwitchGuardedPattern` — `case Integer i when i > 10`: assert the guard adds a branch
  (`assertFileMatches` for the branch `iget`/`R.inc`), guard true/false both covered.
- `testSwitchNullCase` — `case null` and `case null, default` parse, instrument, run.
- `testSwitchExhaustiveNoDefault` — pattern switch with no `default` (exhaustive) compiles/instruments.
- `testRecordPatternInstanceof` — `if (obj instanceof Point(int x, int y))`: binding vars
  `x`,`y` are **not** branch-instrumented (assert negate on branch regex), statement covered.
- `testNestedRecordPattern` — `case Line(Point(var x1,var y1), Point(var x2,var y2))`.
- `testRecordPatternWithVar` — `case Point(var x, var y)`.
- `testWhenIsNotReservedKeyword` — a variable/method named `when` still compiles (regression,
  run under `JAVA_8` like `testRecordIsNotReservedKeyword`).
- Negative: `Java21SwitchPatternFailed.java` style invalid sample asserting graceful handling.

**Required — thin per-version wiring tests.** Add `JavaSyntax18CompilationTest.groovy`,
`JavaSyntax19CompilationTest.groovy`, `JavaSyntax20CompilationTest.groovy`, each extending
`JavaSyntaxCompilationTestBase`, gated with
`assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_18/19/20))`, `srcDir =
javasyntax18/19/20`. Purpose: prove the C1 wiring end-to-end — that `--source 18/19/20` is
accepted (`SourceLevel.fromString` returns `JAVA_18/19/20`) and a simple file instruments,
compiles, and runs. One method each, e.g. `testSourceLevelAcceptedAndInstruments`, using a
minimal `JavaNNSimple.java` fixture (a plain class with a method + statement) and asserting
`assertFileMatches(R_INC)` + `executeMainClasses`/`assertExecOutputContains`. Also add a
`SourceLevel.fromString` unit assertion (in an existing `cfg/instr/java` test, or a new
`SourceLevelTest`) that `"18"/"19"/"20"/"21"` each resolve to the matching enum constant.

### D2. Resource fixtures
New dirs `clover-core/src/test/resources/javasyntax18/`, `javasyntax19/`, `javasyntax20/`,
each with a minimal `Java18Simple.java` / `Java19Simple.java` / `Java20Simple.java` (plain
class, one `main` printing observable output) backing the thin wiring tests above.

New dir `clover-core/src/test/resources/javasyntax21/` (mirror `javasyntax16/17`), one file per case:
- `Java21SwitchTypePatterns.java`, `Java21SwitchGuardedPattern.java`, `Java21SwitchNullCase.java`,
  `Java21RecordPatternInstanceOf.java`, `Java21NestedRecordPattern.java`, `Java21WhenKeyword.java`,
  and a negative `Java21SwitchPatternFailed.java`.
- Each `main` should print observable output so `assertExecOutputContains` can verify runtime
  coverage, matching the Java 16 style.

### D3. Unit-level instrumentation tests (`instr/java/`)
- Extend `InstanceOfStateTest.groovy` (or add `SwitchPatternStateTest`) for the state machine
  handling of switch type patterns, nested record patterns, and bindings.
- Extend `InstrumentationSwitchStatementsTest.groovy` with pattern `case` snippets: assert the
  emitted recorder increments, and that guard expressions add complexity while binding vars do not.

### D4. Regression guard
- Confirm existing `JavaSyntax14/16/17` tests still pass (constant switch, records, sealed,
  `instanceof` binding) — the `patternMatch` rule change must be strictly additive.

---

## Part E — Cross-module impact & report generation

### E1. `clover-ant` — thin passthrough, no code change
Ant integration feeds the source level straight through `SourceLevel.fromString`:
- `ant/tasks/AbstractInstrTask.java:33-37` (`setSource`, also calls `SourceLevel.isUnsupported`),
- `ant/taskdefs/CloverCompilerAdapter.java:171-174` (`<clover-setup>`/compiler adapter),
- report tasks in `CloverReportTask.java` render from the DB, not source level.
→ **No `clover-ant` code change** once C1 lands. The Ant **functional tests** compile real
Java projects and run under the CI JDK; they are only impacted if a fixture actually uses new
syntax. Optional: add a Java 21 ant functest fixture to exercise `--source 21` end-to-end.
CI already builds/runs `clover-ant` on `A-build-and-test-jdk21.yml`.

### E2. `clover-groovy` — NOT impacted
`clover-groovy` instruments **Groovy** via Groovy's own compiler AST (`InstrumentingCodeVisitor`,
`StatementInstrumenter`, etc.) — it does **not** use `java.g`/`JavaLexer`. Java 21 (a
Java-grammar change) is orthogonal; no `clover-groovy` change and no test impact. (Groovy's
own record/switch support is separate and already covered, e.g. `Groovy4CoverageRecordingTest`.)
No action beyond confirming its suite still passes on JDK 21 (it already runs in CI).

### E3. Report STRUCTURE (XML & HTML) — unaffected, no schema change
Coverage is modelled per line as statement / branch / method — never per syntactic construct.
The XML `<line>` element (`clover-core/src/main/resources/clover.xsd:110-138`) has a generic
`type ∈ {method, stmt, cond}`:
- a pattern `case` (arrow or colon) is emitted as an ordinary **stmt** line;
- a `when` guard is an ordinary **cond** line (`truecount`/`falsecount`), consistent with C4;
- record-pattern bindings add no coverage nodes (they are not branches, per C4).
So no new XML elements/attributes and **no `clover.xsd` bump**; `XMLReporter.java` needs no
change. HTML report structure (metrics, line tables) is likewise driven by the same model.

### E4. HTML syntax COLORING — cosmetic only, low priority
The HTML source view runs an **independent** highlighting pass, separate from parsing/coverage:
`reporters/html/source/java/JavaTokenTraverser.java` spins up its own `JavaLexer` and emits
listener events; `src-file-source-lines.vm` renders them. Keyword coloring is decided by
`JavaKeywords.java` — a fixed set of **real** token types. `SWITCH`, `INSTANCEOF`, `CASE`,
`DEFAULT`, `NULL` are real tokens and are **already** colored correctly for pattern switches.
The new contextual keywords `record`/`sealed`/`permits`/`yield`/`when`/`var` are lexed as
`IDENT`, so they render as identifiers, **not** keyword-highlighted — exactly as `record`/
`sealed` already behave today. `non-sealed` (`NON_SEALED`, a real token) is likewise not in
`JavaKeywords`.
- **Robustness (important):** the finalized features (JEP 440/441) introduce **no new tokens** —
  patterns reuse types, `IDENT`, parens, commas, `->`, `:`. So `JavaTokenTraverser`'s lexer
  already tolerates Java 21 finalized syntax; no breakage risk. (Only preview String Templates
  would need lexer/highlighter work — out of scope.)
- **Optional polish:** if consistent highlighting of `when`/`record`/`sealed`/`var` is wanted,
  it must be **context-sensitive** (they are contextual keywords; blanket coloring would
  miscolor identifiers named `var`/`when`). Given the risk and low value, recommend **leaving
  coloring as-is** (matches current record/sealed behavior). If pursued, extend
  `JavaTokenTraverser` (contextual detection) + `JavaTokenTraverserTest.groovy`, not `JavaKeywords`.

### E5. Report-side test touch points (only if E4 polish is done)
`clover-core/src/test/groovy/org/openclover/core/reporters/html/source/java/JavaTokenTraverserTest.groovy`
— add cases for a pattern switch / guarded case to assert the traverser emits the expected
chunks and does not choke. Otherwise no report-side test changes are required.

## Verification

1. Build with JDK 21: run the **clover-core** module tests, focusing on the new/edited Groovy
   tests: `JavaSyntax21CompilationTest`, the thin `JavaSyntax18/19/20CompilationTest`,
   `InstanceOfStateTest`, `InstrumentationSwitchStatementsTest`, and (if E4 polish is done)
   `JavaTokenTraverserTest`, plus the regression `JavaSyntax14/16/17CompilationTest`.
2. The compilation tests already do the real end-to-end check: instrument a `.java` fixture via
   `CloverInstr.mainImpl`, compile it with a forked `Javac` at `--source 21`, run the main class,
   and assert both the instrumented source (`assertFileMatches`) and the coverage DB
   (`assertStatementCoverage`/`assertMethodCoverage`) — so a passing `JavaSyntax21CompilationTest`
   proves parse + instrument + compile + run + coverage.
3. **clover-idea:** run `CloverJavaSourceTransformerTest` (updated per C5b) to confirm the
   `LanguageLevel → SourceLevel` map now yields `JAVA_18..21`.
4. **clover-ant & clover-groovy:** run both module suites on JDK 21 to confirm no regression
   (both already run in CI). No new syntax reaches them unless a fixture is added (E1/E2);
   optionally add a Java 21 ant functest fixture to prove `--source 21` through the Ant task.
5. **Report generation:** generate an HTML + XML report over a project containing a guarded
   record-pattern switch; confirm the XML validates against `clover.xsd` unchanged (pattern
   `case` → `stmt`, guard → `cond`), and the HTML source view renders without lexer errors
   (coloring of `when`/`record` as plain identifiers is expected — see E4).
6. Manual smoke: `--source 21` on a small file using a guarded record-pattern switch; confirm
   no parse error and that guard branches show in the HTML/console report.
7. CI: the `A-build-and-test-jdk21.yml` plan runs the full suite (core + ant + groovy + idea +
   eclipse) on temurin-21 (already present).

## Out of scope (documented, not implemented)
String Templates (JEP 430), Unnamed Patterns/Variables `_` (JEP 443), Unnamed Classes &
Instance Main (JEP 445) — all preview, require `--enable-preview`; JEP 430 later withdrawn.
Revisit when they finalize.
