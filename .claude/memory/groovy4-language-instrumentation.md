# OC-192: Groovy 4 Language Instrumentation — Implemented

## Context

Branch: `OC-192-support-groovy-4`

Groovy 4 was already adopted in the test infrastructure (pom.xml uses `org.apache.groovy:groovy:4.0.15`).
The clover-groovy module already compiled and ran tests against Groovy 4.
This ticket verified and extended **Clover instrumentation** to correctly handle new Groovy 4 constructs.

---

## What Changed in Groovy 4 vs Groovy 3

### Maven Group ID
| Groovy 2/3 | Groovy 4 |
|------------|---------|
| `org.codehaus.groovy:groovy` | `org.apache.groovy:groovy` |

Java package names (`org.codehaus.groovy.*`) are **unchanged** in Groovy 4 — backward compatible.

### AST Node Inventory (key finding)
Groovy 4 (4.0.15) has **exactly the same `org.codehaus.groovy.ast.expr.*` and `org.codehaus.groovy.ast.stmt.*` classes** as Groovy 3:
- No new `SwitchExpression` class
- No new expression/statement nodes for records or sealed classes

New AST tooling classes are in `org.apache.groovy.ast.tools.*` but they are not expression/statement nodes.

### How New Groovy 4 Syntax Compiles to AST

**Switch expressions** (`switch(x) { case 1 -> "one" ... }`)
- Compiled to a `ClosureExpression` wrapping a `SwitchStatement`, immediately `.call()`ed.
- The `switch` variable is saved to a synthetic `__$$sev0` local inside the closure.
- Pattern: `{ -> __$$sev0 = x; switch(__$$sev0) { case 1: return "one" ... } }.call()`
- When no `default` is present, Groovyc **injects `EmptyStatement.INSTANCE` as the default**.
  That singleton has source coordinates `[-1:-1 .. -1:-1]` — no source position.
- Void switch expression (side-effect cases, e.g. `case 1 -> println("one")`) compiles to an
  `ExpressionStatement` wrapping the closure `.call()`, rather than a `ReturnStatement`.

**Records** (`record Point(int x, int y) { ... }`)
- Compile to regular `ClassNode` with annotations:
  `@RecordBase`, `@RecordOptions`, `@TupleConstructor`, `@KnownImmutable`, `@POJO`, `@CompileStatic`
- Auto-generated methods (NOT flagged `synthetic=true`): `toString`, `equals`, `hashCode`, `getAt`,
  `toList`, `toMap`, `size`
- Because these are not flagged `synthetic`, Clover instruments them normally.
  They appear in coverage reports, which is acceptable — they are real executable code.

**Sealed classes/interfaces** (`sealed interface Shape permits Circle, Square`)
- Compile to `ClassNode` with `@groovy.transform.Sealed` annotation only.
- No special AST nodes. The `permits` clause is annotation attributes, not visited expressions.
- No instrumentation changes needed.

**Enhanced range syntax** (`0<..<3`, `3<..5`, `0<..3`)
- Parses to `RangeExpression` nodes (same class as before, with `exclusiveLeft`/`exclusiveRight` flags).
- No instrumentation changes needed.

**Decimal literals without leading zero** (`.5` equals `0.5`)
- Parses to `ConstantExpression` as before (value `0.5`).
- No instrumentation changes needed.

### Features NOT in scope (experimental/incubating in Groovy 4)
- GINQ (Groovy-Integrated Query) — incubating, not enabled by default
- Macro methods (`SV()`, `SVI()`, etc.) — compiler debugging tools, not user-facing syntax

---

## Instrumentation Fix Implemented

### The Problem
Switch expressions (and ALL switches) without an explicit `default` use `EmptyStatement.INSTANCE`
as the default branch. This singleton has coordinates `[-1:-1..-1:-1]`. The old `visitSwitch`
called `instrumentBlockStatement(EmptyStatement.INSTANCE)`, which fell into the else-branch
("don't know how to handle it") and returned unchanged — the fallthrough branch was never
registered in the coverage model.

### The Solution: `MutableEmptyStatement` + `== EmptyStatement.INSTANCE` check

- **New `MutableEmptyStatement` class** (extends `EmptyStatement`) — a type-marker subclass
  instantiated with synthesized coordinates. Extends rather than replaces `EmptyStatement` so
  Groovy's own `instanceof EmptyStatement` checks in code generation continue to work. `ASTNode`
  setters don't throw; no override needed.

- **`InstrumentingCodeVisitor.visitSwitch`** — detects the injected sentinel with
  `defaultStatement == EmptyStatement.INSTANCE` (reference equality, not `getLineNumber() == -1`).
  The reference check is precise: `getLineNumber() == -1` would also fire for any future synthetic
  statement without source info, incorrectly replacing its body with just `R.inc()`. `INSTANCE` is
  `public static final`, stable across Groovy 2/3/4, and the same object identity in the same JVM
  as Groovyc. When matched, creates a `MutableEmptyStatement` at synthesized coordinates
  `(lastLine, lastCol-1, lastLine, lastCol)` — a 1-char span at the closing `}` — and passes it
  to `instrumentBlockStatement`.

- **`StatementInstrumenter.instrumentBlockStatement`** — new `MutableEmptyStatement` branch
  (before `TryCatchStatement`): calls `instrumentStmt` to register it in the model, then returns
  only the `R.inc()` call. No `BlockStatement` wrapper needed — the null-return for switch
  expressions comes from the enclosing closure falling off the end, not from the default statement.

### Scope: broader than Groovy 4 only
This fix applies to ALL Groovy versions — traditional `switch` statements without explicit
`default` also use `EmptyStatement.INSTANCE`. The fix correctly adds coverage tracking for
those too. `GroovyCoverageTest.testImplicitReturnsArePreserved` was updated:
- `m.statements.size()` changed from 3 → 4 for both `implicitReturns` and `explicitReturns`
- Added `assertStatement(m, at(11, 29, 11, 30), hits(0))` and `assertStatement(m, at(20, 29, 20, 30), hits(0))`

---

## Files Changed

```
clover-groovy/src/main/java/org/openclover/groovy/instr/
  MutableEmptyStatement.java          ← NEW
  InstrumentingCodeVisitor.java       ← visitSwitch: == EmptyStatement.INSTANCE check
  StatementInstrumenter.java          ← instrumentBlockStatement: MutableEmptyStatement branch

clover-groovy/src/test/groovy/org/openclover/groovy/instr/
  Groovy4CoverageRecordingTest.groovy ← NEW — 9 test methods (@GroovyVersionStart("4.0.0"))
  TestSuite.groovy                    ← +Groovy4CoverageRecordingTest in TEST_CLASSES_AND_SELECTORS
  GroovyCoverageTest.groovy           ← testImplicitReturnsArePreserved: updated statement counts
```

---

## Test Coverage

`Groovy4CoverageRecordingTest` (9 tests, all passing with Groovy 4.0.15):

| Test method | What it verifies |
|---|---|
| `testSwitchArrowWithDefaultHitCounts` | Arrow switch with explicit default; classify method hit=3 |
| `testSwitchArrowNoDefaultNullBranch` | Arrow switch, no default; synthesized default at `at(6,9,6,10)` hit=1 |
| `testSwitchYieldWithDefaultHitCounts` | Yield switch with explicit default; method hit counts |
| `testSwitchYieldNoDefaultNullBranch` | Yield switch, no default; synthesized default hit=1 |
| `testSwitchVoidHitCounts` | Void/side-effect switch; synthesized default at `at(7,9,7,10)` hit=1 |
| `testRecordGeneratedAndUserMethodHitCounts` | Record: origin() hit=1, no crash on generated methods |
| `testSealedClassesDoNotCrash` | Sealed interface/class; no crash during instrumentation |
| `testEnhancedRangeHitCounts` | `<..` and `<..<` ranges; method hit counts |
| `testDecimalLiteralWithoutLeadingZero` | `.5` and `.25` literals; method hit counts |

Run command:
```
mvn test -pl clover-groovy -Dtest="Groovy4CoverageRecordingTest" -Dclover.test.groovyversion.includes=4.0
```

---

## Backward Compatibility Strategy (Groovy 2 runtime)

Pattern established in OC-121 for `LambdaExpression` and `MethodReferenceExpression`:
- Do NOT `import` Groovy 4-only classes directly
- Use `Class.forName(...)` with `catch (ClassNotFoundException ignored)` in a static initializer

**Not needed for Groovy 4** — no new AST expression/statement classes were introduced.
`MutableEmptyStatement` extends a class present since Groovy 1.x and is safe on all versions.

---

## Reference Commits

- OC-121 (Groovy 3 support): `3905c046..9600b99d` on master
  - Key: `e913ab23` — Class.forName pattern for LambdaExpression/MethodReferenceExpression
  - Key: `0f98ca2e` — DoWhileStatement instrumentation
  - Key: `130ba153` — default interface method instrumentation
- OC-31 (CompileStatic support): `3905c046` — see full commit message for split-phase strategy
