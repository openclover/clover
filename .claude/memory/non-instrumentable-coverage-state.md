---
name: non-instrumentable-coverage-state
description: Idea to introduce a NON_INSTRUMENTABLE statement state in Clover model for code sites where R().inc() cannot be injected without breaking semantics
metadata:
  type: project
---

Idea: add a `NON_INSTRUMENTABLE` state to Clover's coverage model alongside `COVERED` / `NOT_COVERED` / `PARTIALLY_COVERED`.

**Why:** Some valid source code locations cannot have a recorder call (`R().inc()`) injected in-place without breaking language semantics or breaking compilation. Currently, these sites are either silently skipped (not registered in the model at all) or registered but permanently uncovered, both of which are wrong from a reporting perspective.

If this state is introduced, such sites should appear in HTML/XML reports as "non-instrumentable" (e.g. grey, distinct icon) so developers understand why coverage is missing without it counting against their percentage.

---

## Full list of known non-instrumentable / skipped constructs

### Java (clover-core)

**1. `instanceof` pattern matching — branch skipped**
- Construct: `if (o instanceof String s) { ... }` (Java 16+, pattern variables)
- Why: Branch instrumentation wraps the condition as `(((o instanceof String s) && (R.inc(n)!=0|true)) || ...)`. This makes the pattern variable `s` invisible after the condition — compilation error: "symbol not found".
- Code: `ExpressionInfo.isInstrumentable()` returns `false` when `containsInstanceOfWithVariable` is true, detected by `InstanceOfStateDetector` / `InstanceOfState`.
- Applies to: `o instanceof A s`, `o instanceof final A s`, `o instanceof A.B ab`, `o instanceof A.B[] arr`, `o instanceof List<String> ls`
- Does NOT apply to: `o instanceof String` (no variable binding — fully instrumentable)
- Test: `clover-core/src/test/resources/javasyntax16/Java16InstanceOfPatternMatching.java` lines 121–128
- Note: TODO comment in that file says "branch instrumentation of instanceof with casting is unsupported"

**2. Assignment operator inside branch condition — branch skipped**
- Construct: `if ((a = getValue()) != null) { ... }`
- Why: Wrapping the condition would double-evaluate or mis-sequence the assignment. `ExpressionInfo.isInstrumentable()` returns `false` when `containsAssign` is true, detected by `AssignmentDetector`.
- Code: `ExpressionInfo.fromTokens()` in clover-core

**3. Constant expressions in boolean conditions — branch skipped**
- Construct: `if (true)`, `if (false && x)`, `while (true)`, ternary branches where condition is constant
- Why: Branch instrumentation on constant conditions is meaningless (always-taken / never-taken). `constExpr` flag is set to `true` during grammar parse of constant expressions.
- Code: `java.g` around line 3147: `if (!constExpr) instrBoolExpr(...)`. `ExpressionInfo.scanForConstant()` via `ConstantExpressionDetector`.
- Also applies to: switch case labels (`case 5 + 3:`) — `constExpr = true` is set before parsing `constantExpression` in the grammar.

**4. Java try-with-resources — NOT a limitation (solved via AutoCloseable trick)**
- Construct: `try (InputStream in = new FileInputStream(f)) { ... }`
- Status: **Fully instrumented.** A synthetic anonymous `AutoCloseable` class (e.g. `__CLR4_0_0_autocloseable0`) is created whose instance initializer calls `R.inc(n)`. This class is instantiated inside `try(...)` as an additional resource — syntactically valid, semantically correct.
- Code: `ArmInstrEmitter.java`, `AutoCloseableEmitter.java`, called from `instrArmDecl()` in `java.g` around line 2747.
- Conclusion: resource declarations ARE registered as statements and ARE instrumented. No gap here.

---

### Groovy (clover-groovy)

**5. Groovy try-with-resources resources — statement skipped entirely**
- Construct: `try (InputStream in = new ByteArrayInputStream(...)) { }` (Groovy 3+)
- Why: Groovy desugars try-with-resources during the `CANONICALIZATION` compile phase — **before** OpenClover's `INSTRUCTION_SELECTION` phase. By the time the instrumentation transformer runs, the resources have already been moved into the method body as regular variable declarations. The `try(...)` AST node still exposes `getResourceStatements()` returning those resources, but they are the *same* AST nodes already present as statements in the method body (and will be instrumented there). Registering them a second time via the try's resource list would produce duplicate model entries.
- Code: `InstrumentingCodeVisitor.visitTryCatchFinally()` — resources are visited for sub-expression instrumentation (so closures/lambdas inside them get instrumented) but NOT registered via `session.addStatement()`.
- Note: The desugared resource declarations in the method body ARE instrumented normally. Coverage is correct; only the model representation differs from what you'd expect at the source level.
- Reference: `InstrumentingCodeVisitor.java` lines 713–729 with the TODO comment referencing this idea.

**6. Groovy `break` statements — statement skipped**
- Construct: `break` inside `switch` / loop
- Why: In Groovy's AST, `break` appears as a `BreakStatement`. In a `switch` expression context, the last expression in a case block determines the switch's return value. Prepending `R().inc()` before the `break` would change the block's effective return value to `void` / the return of `R().inc()`, breaking switch semantics.
- Code: `StatementInstrumenter.java` lines 95–96: `(statement instanceof BreakStatement) ? null : instrumentStmt(statement)`
- Ticket: CLOV-1341

**7. Groovy traits — class entirely skipped**
- Construct: `trait MyTrait { def method() { ... } }`
- Why: Not yet implemented. Trait helper (`$Trait$Helper`) classes have invalid source regions (`-1:-1`) which causes them to be skipped by the `GroovyUtils.isReportable()` check.
- Code: `InstrumentingCodeVisitor.java` line 314: `// TODO: CLOV-1960 instrument traits`
- Note: Groovy *interface default methods* (handled as `$Trait$Helper`) ARE now instrumented as of OC-121 work (branch `OC-121-support-groovy-3`). The remaining gap is actual `trait` keyword classes.
- Ticket: CLOV-1960

**8. Groovy method references — no body to inject into**
- Construct: `Integer::toString`, `Integer[][]::new`
- Why: A `MethodReferenceExpression` has no code body — it is a reference to a method, not an invocation. `R().inc()` cannot be inserted inside it.
- Status: Already handled — wrapped in an `exprEval(methodRef, stmtIdx)` call in `OperatorsInstrumenter.java` lines 178–196. Listed here for completeness; the wrapper approach is the correct solution.

**9. Groovy classes / methods with invalid source regions**
- Construct: Any synthetic class or method where `getLineNumber()` / `getColumnNumber()` returns `-1`
- Why: Without valid source positions, the coverage model cannot store meaningful source location data.
- Code: `GroovyUtils.isReportable(ClassNode)` and related checks in `CloverAstTransformerBase` / `CloverAstTransformerInstructionSelection`.

---

## Root cause taxonomy

| Category | Examples |
|----------|---------|
| **Variable scope leak** | `instanceof` pattern matching — variable declared inside condition would go out of scope after wrapping |
| **Return-value contract** | Groovy `break` — prepending `inc()` changes the effective return value of the case block |
| **Compile-phase ordering** | Groovy try-with-resources — desugared before instrumentation phase runs |
| **No code body** | Method references — syntactic form has no executable body to inject into |
| **Not yet implemented** | Groovy traits (CLOV-1960) |
| **Constant folding** | Constant boolean/conditional expressions — instrumentation of always-true/false branches is meaningless |
| **Assignment in condition** | `if (a = val)` — wrapping with boolean short-circuit would mis-sequence the assignment |

---

## Related code
- `InstrumentingCodeVisitor.visitTryCatchFinally()` — TODO comment referencing this idea (Groovy)
- `ExpressionInfo.isInstrumentable()` — three-flag gate for Java branch instrumentation
- `ArmInstrEmitter` / `AutoCloseableEmitter` — Java try-with-resources workaround (for reference, as the "solved" case)
- `InstanceOfState` / `InstanceOfStateDetector` — pattern-variable detection state machine
- `AssignmentDetector` — detects `=` in condition expressions
- `StatementInstrumenter` — Groovy break skip (CLOV-1341)

---

## Implementation Plan

### Goal
Register non-instrumentable code constructs in the Clover database and expose them in metrics and reports as a separate, excluded-from-coverage metric. A developer writing code that contains only non-instrumentable un-coverable sites should still be able to reach 100% coverage.

### Design decisions
- **No runtime changes needed.** Non-instrumentable constructs are never injected with `R().inc()`, so the coverage recorder (clover-runtime) does not need to know about them. They will never accumulate hit counts.
- **Database storage**: use `LanguageConstruct` to flag non-instrumentable entries. Add a new `Builtin.NON_INSTRUMENTABLE` enum constant (id `"NI"`). This is backward-compatible: old readers will get an "unknown construct" fallback, new readers recognise it.
- **Metrics**: add `numNonInstrumentable` counter alongside `numStatements`. Exclude non-instrumentable entries from `numStatements` and from coverage percentage calculations. Expose as a separate metric so it can be shown in reports.
- **Applies to**: statements and branches alike (both can be non-instrumentable for different reasons).
- **Scope of reporting**: file / class / method level, same granularity as statements.

### Phase 1 — Data model (clover-core, no reporters yet)

**`clover-core/src/main/java/org/openclover/core/spi/lang/LanguageConstruct.java`**
- Add `NON_INSTRUMENTABLE("NI", "non-instrumentable ...")` to `Builtin` enum; no runtime injection message needed but calcCoverageMsg should return a human-readable explanation.

**`clover-core/src/main/java/org/openclover/core/api/registry/BlockMetrics.java`** (interface)
- Add `int getNumNonInstrumentable()` accessor to the API contract.

**`clover-core/src/main/java/org/openclover/core/registry/metrics/BlockMetrics.java`** (implementation)
- Add `int numNonInstrumentable` field; add `addNumNonInstrumentable(int)`, `getNumNonInstrumentable()`, `setNumNonInstrumentable(int)`.
- Update `add(BlockMetrics)` to also accumulate `numNonInstrumentable`.
- `getNumStatements()` must NOT include non-instrumentable entries (they are tracked separately).
- `getPcCoveredStatements()` denominator stays `numStatements` (excludes non-instrumentable) → developers can reach 100%.

**`clover-core/src/main/java/org/openclover/core/registry/metrics/ClassMetrics.java`**
- Override `getNumNonInstrumentable()` if class-level aggregation needs special handling (likely not — inherited from BlockMetrics suffices).

**`clover-core/src/main/java/org/openclover/core/registry/entities/FullMethodInfo.java`** (`calcMetrics`)
- In the statement loop: detect `statementInfo.getConstruct() == NON_INSTRUMENTABLE`; if so, increment `numNonInstrumentable` and **skip** `numStatements++` and the `covered++` check.
- Same for branches loop if `BranchInfo` can also be non-instrumentable.

**`clover-core/src/main/java/org/openclover/core/registry/entities/FullClassInfo.java`** (`calcMetrics`)
- Same non-instrumentable exclusion logic for class-level statements (those declared outside methods).

**`clover-core/src/main/java/org/openclover/core/registry/entities/FullFileInfo.java`**
- If file-level metrics are rolled up separately, propagate `numNonInstrumentable` there too.

**`clover-core/src/main/java/org/openclover/core/registry/entities/FullStatementInfo.java`**
- No serialisation change needed — `LanguageConstruct.getId()` is already written/read as a UTF string; the new `"NI"` id will be stored and looked up automatically via `Languages.lookupConstruct()`.
- Add convenience `isNonInstrumentable()` helper: `return getConstruct() == LanguageConstruct.Builtin.NON_INSTRUMENTABLE`.

**`clover-core/src/main/java/org/openclover/core/registry/entities/FullBranchInfo.java`**
- Same `isNonInstrumentable()` helper for branch-level non-instrumentable (e.g. instanceof pattern matching).
- The existing `isInstrumented` boolean already partially covers this; consider whether to replace it or keep both.

### Phase 2 — Instrumentation call sites

**`clover-core/src/main/java/org/openclover/core/instr/InstrumentationSessionImpl.java`**
- No change needed; `addStatement(context, region, complexity, LanguageConstruct)` already accepts any construct. Callers just pass `NON_INSTRUMENTABLE`.
- However, for non-instrumentable entries no data slot should be allocated in the coverage array (they will never be hit). Currently `currentOffsetFromFile += stmt.getDataLength()` always allocates 1 slot. Add a check: if construct is `NON_INSTRUMENTABLE`, do NOT increment `currentOffsetFromFile` (data index stays the same as next real statement). Alternatively allocate the slot but never write to it (simpler, wastes 1 int per non-instrumentable entry).

**Java instrumentation — `clover-core/src/main/java/org/openclover/core/instr/java/java.g`**
- For `instanceof` with pattern variable: where `instrBoolExpr` is skipped due to `containsInstanceOfWithVariable`, call a new `instrNonInstrumentableBranch(start, end)` that registers the expression with `NON_INSTRUMENTABLE` construct and emits no code.
- For assignment-in-condition: same approach where `containsAssign` is true.
- For constant expressions: these are an intentional semantic skip (meaningless to track); leave as-is (do NOT register as non-instrumentable — constant branches are not a coverage gap, they are a code-smell smell).

**`clover-core/src/main/java/org/openclover/core/instr/java/StatementInstrEmitter.java`**
- No change for regular statements; they are fully instrumented.

**Groovy instrumentation — `clover-groovy/src/main/java/org/openclover/groovy/instr/InstrumentingCodeVisitor.java`**
- `visitTryCatchFinally()`: currently resources are visited but not registered. Now register each resource as a statement with `NON_INSTRUMENTABLE` construct and do NOT emit `R().inc()` before it. This records the position in the model for reporting without affecting runtime.
- `visitBreakStatement()` (Groovy): register with `NON_INSTRUMENTABLE` instead of skipping entirely (optional — see `StatementInstrumenter.java`).

**`clover-groovy/src/main/java/org/openclover/groovy/instr/StatementInstrumenter.java`**
- Where `BreakStatement` is skipped (line 95–96): register it via `session.addStatement(..., NON_INSTRUMENTABLE)` instead of passing `null` to skip.

### Phase 3 — Metrics API propagation

**`clover-core/src/main/java/org/openclover/core/reporters/Columns.java`**
- Add `TotalNonInstrumentable` column class (extends `TotalColumn`): reads `getNumNonInstrumentable()` from metrics.
- Existing `TotalStatements`, `CoveredStatements`, `UncoveredStatements` columns need no change (they already exclude non-instrumentable because `getNumStatements()` will not include them).

**`clover-core/src/main/java/org/openclover/core/util/MetricsFormatUtils.java`**
- Add a `formatNonInstrumentable(int count)` method if the console/text formatter needs a dedicated helper.

### Phase 4 — XML report

**`clover-core/src/main/java/org/openclover/core/model/XmlNames.java`**
- Add `A_NONINSTRUMENTABLE = "noninstrumentable"` constant.
- Add `V_NI = "ni"` for per-line type value (alongside existing `V_STMT`, `V_COND`, `V_METHOD`).

**`clover-core/src/main/java/org/openclover/core/reporters/xml/XMLReporter.java`**
- In the `<metrics>` element: emit `noninstrumentable="N"` attribute from `metrics.getNumNonInstrumentable()`.
- In the per-line `<line>` elements: where `stmt.isNonInstrumentable()`, emit `type="ni"` with `count="-1"` (or omit count) and no `truecount`/`falsecount`.

### Phase 5 — HTML report (Velocity templates)

**`clover-core/src/main/resources/html_res/adg/header-stats.vm`**
- Add a "Non-instrumentable:" stat box showing `$headerMetrics.numNonInstrumentable` next to the existing Statements box (only shown when `> 0`).

**`clover-core/src/main/resources/html_res/adg/coverage-row.vm`**
- No change needed unless a dedicated column for non-instrumentable is added to the table (can be optional/configurable).

**`clover-core/src/main/resources/html_res/adg/src-file-source-lines.vm`**
- Add a CSS class / visual indicator for lines that are non-instrumentable (e.g. grey background, distinct icon in the gutter). Currently covered = green, uncovered = red, partially = yellow. Non-instrumentable = grey/striped.

**`clover-core/src/main/resources/html_res/adg/src-file-legend.vm`**
- Add legend entry for the non-instrumentable colour/icon.

**`clover-core/src/main/resources/html_res/adg/src-file-classes-method-row.vm`** and **`src-file-classes-class-row.vm`**
- Optionally add a "NI" count column in the method/class table if desired.

**`clover-core/src/main/resources/html_res/adg/api-json.vm`** (used by JSON report)
- Add `"nonInstrumentable": $metrics.numNonInstrumentable` field to the JSON object emitted per class/method/file.

### Phase 6 — Console/text report

**`clover-core/src/main/java/org/openclover/core/reporters/console/ConsoleReporter.java`**
- Add a "Non-instrumentable statements: N" line next to the existing statements/branches/methods summary (only when `numNonInstrumentable > 0`).

### Phase 7 — Historical report (optional)

**`clover-core/src/main/resources/html_res/adg/historical.vm`** and related
- If historical charting of non-instrumentable is desired, add a data series. Low priority; can be deferred.

### Out of scope
- `clover-runtime` — no changes. Non-instrumentable entries never get `R().inc()` injected, so the recorder array, flush policy, and all runtime classes are unaffected.
- Database format version bump — the `"NI"` construct id is stored as a UTF string; old readers will fall back to `LanguageConstruct.Builtin.STATEMENT` (or unknown) via `Languages.lookupConstruct()`. If backward-compat matters, verify what `lookupConstruct` returns for unknown ids and ensure old readers don't crash on `"NI"`.
