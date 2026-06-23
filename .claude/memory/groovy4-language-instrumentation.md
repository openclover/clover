# OC-192: Groovy 4 Language Instrumentation Plan

## Context

Branch: `OC-192-support-groovy-4`

Groovy 4 was already adopted in the test infrastructure (pom.xml uses `org.apache.groovy:groovy:4.0.15`).
The clover-groovy module already compiles and runs tests against Groovy 4.
This ticket is about verifying and extending **Clover instrumentation** to correctly handle the new
language constructs introduced in Groovy 4.

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
- The `switch` variable is saved to a synthetic `__$$sev0` local before the closure.
- Pattern: `{ -> __$$sev0 = x; switch(__$$sev0) { case 1: return "one" ... } }.call()`
- When no `default` is present, Groovyc **injects a synthetic default that returns null**.
  That injected default statement has source coordinates `[-1:-1 .. -1:-1]` — no source position.
- Void switch expression (side-effect cases, e.g. `case 1 -> println("one")`) is valid.
  It compiles to an `ExpressionStatement` wrapping the closure `.call()`, rather than `ReturnStatement`.

**Records** (`record Point(int x, int y) { ... }`)
- Compile to regular `ClassNode` with annotations:
  `@RecordBase`, `@RecordOptions`, `@TupleConstructor`, `@KnownImmutable`, `@POJO`, `@CompileStatic`
- Auto-generated methods (not `synthetic=true`): `toString`, `equals`, `hashCode`, `getAt`,
  `toList`, `toMap`, `size`
- User-defined methods within the record body are also present.
- Because these are not flagged `synthetic`, Clover will attempt to instrument them.
  Their bodies do contain statements (Groovy generates real AST for them), so they will appear in
  coverage reports. This is acceptable — they are real executable code.

**Sealed classes/interfaces** (`sealed interface Shape permits Circle, Square`)
- Compile to `ClassNode` with `@groovy.transform.Sealed` annotation only.
- No special AST nodes. The `permits` clause is encoded as annotation attributes, not as AST expressions
  that would be visited by `InstrumentingCodeVisitor`.
- No instrumentation changes needed; tests verify no crash.

**Enhanced range syntax** (`0<..<3`, `3<..5`, `0<..3`)
- Parses to `RangeExpression` nodes (same class as before, with `exclusiveLeft`/`exclusiveRight` flags).
- No instrumentation changes needed; tests verify correct handling.

**Decimal literals without leading zero** (`.5` equals `0.5`)
- Parses to `ConstantExpression` as before (value `0.5`).
- No instrumentation changes needed; tests verify no crash.

### Features NOT in scope (experimental/incubating in Groovy 4)
- GINQ (Groovy-Integrated Query) — incubating, not enabled by default
- Macro methods (`SV()`, `SVI()`, etc.) — compiler debugging tools, not user-facing syntax

## Backward Compatibility Strategy (Groovy 2 runtime)

Pattern established in OC-121 for `LambdaExpression` and `MethodReferenceExpression`:
- Do NOT `import` Groovy 4-only classes directly
- Use `Class.forName(...)` with `catch (ClassNotFoundException ignored)` in a static initializer
- Store in a `static final Class<?>` field
- Check with `FIELDNAME != null && FIELDNAME.isInstance(expr)` before casting

As of Groovy 4, no new AST expression/statement classes were introduced, so **no new Class.forName
workarounds are needed for Groovy 4 specifically**. This remains a concern if a future version adds new nodes.

## Scope: clover-groovy Module Only

All changes are expected to stay within `clover-groovy/`:
- `src/main/java/org/openclover/groovy/instr/` — instrumentation logic
- `src/test/groovy/org/openclover/groovy/instr/` — integration tests
- No changes expected in clover-core, clover-ant, or other modules.

## Implementation Tasks

### 1. Code Samples for Each Feature

Each snippet is used twice in tests: once plain, once with `@groovy.transform.CompileStatic`
on the class/method under test (see "Test Structure" below).

---

**Switch expression — arrow form with default** (value-returning)
```groovy
class SwitchArrow {
    static String classify(int n) {
        return switch (n) {
            case 1 -> "one"
            case 2 -> "two"
            default -> "other"
        }
    }
    static void main(String[] args) {
        assert classify(1) == "one"
        assert classify(2) == "two"
        assert classify(99) == "other"
    }
}
```
Expected (call sequence: `classify(1)`, `classify(2)`, `classify(99)`):
- closure wrapper: hit=3
- case-1 body: hit=1
- case-2 body: hit=1
- default body: hit=1
- synthetic null-branch (injected default when user writes `default ->`): not present (Groovyc does
  not inject it when `default` is written)

---

**Switch expression — arrow form without default** (value-returning, null fallthrough)
```groovy
class SwitchArrowNoDefault {
    static String classify(int n) {
        return switch (n) {
            case 1 -> "one"
            case 2 -> "two"
        }
    }
    static void main(String[] args) {
        assert classify(1) == "one"
        assert classify(99) == null
    }
}
```
Expected (call sequence: `classify(1)`, `classify(99)`):
- closure wrapper: hit=2
- case-1 body: hit=1
- case-2 body: hit=0
- Groovyc-injected default (returns null): hit=1, but **source coordinates are `[-1:-1..-1:-1]`**.

Instrumentation: The injected default has no source position. **Decision: synthesize a 1-character
region at the closing `}` of the enclosing `SwitchStatement`.**

Concretely, inside `InstrumentingCodeVisitor.visitSwitch(SwitchStatement statement)` we already have
both the parent `SwitchStatement` (with valid coordinates) and the default `Statement` (with -1,-1)
at the same time. When `statement.getDefaultStatement().getLineNumber() == -1`, before calling
`statementInstrumenter.instrumentBlockStatement(...)`, patch or wrap the default statement with a
synthesized source region:

```
synthesizedLine   = statement.getLastLineNumber()
synthesizedCol    = statement.getLastColumnNumber() - 1   // the '}' character (1-indexed)
synthesizedEndCol = statement.getLastColumnNumber()        // exclusive end, one past '}'
```

AST inspection confirms: `SwitchStatement` for a 7-line class has coordinates `[4:16 .. 7:10]`,
so `lastColumnNumber=10` and the `}` sits at column 9. The synthesized region is `[7:9 .. 7:10]`,
a single character.

The `EmptyStatement` singleton (`EmptyStatement$1`) cannot have its coordinates mutated (it's shared).
Instead, pass the synthesized region directly to whatever overload of `StatementInstrumenter` records
the statement — or wrap the empty statement in a new `BlockStatement` with those coordinates before
handing it to `instrumentBlockStatement`.

---

**Switch expression — yield form with default**
```groovy
class SwitchYield {
    static String classify(int n) {
        return switch (n) {
            case 1: yield "one"
            case 2: yield "two"
            default: yield "other"
        }
    }
    static void main(String[] args) {
        assert classify(1) == "one"
        assert classify(99) == "other"
    }
}
```
Expected (call sequence: `classify(1)`, `classify(99)`):
- closure wrapper: hit=2
- case-1 body: hit=1 (case-2: hit=0, default: hit=1)
- Groovyc does NOT inject a synthetic null-branch when `default` is present

---

**Switch expression — yield form without default** (null fallthrough)
```groovy
class SwitchYieldNoDefault {
    static String classify(int n) {
        return switch (n) {
            case 1: yield "one"
            case 2: yield "two"
        }
    }
    static void main(String[] args) {
        assert classify(1) == "one"
        assert classify(99) == null
    }
}
```
Expected: same null-branch consideration as arrow-no-default.

---

**Switch expression — void (side-effect cases)**
```groovy
class SwitchVoid {
    static List<String> log = []
    static void classify(int n) {
        switch (n) {
            case 1 -> log.add("one")
            case 2 -> log.add("two")
        }
    }
    static void main(String[] args) {
        classify(1)
        classify(99)
        assert log == ["one"]
    }
}
```
Note: void switch compiles to `ExpressionStatement` (not `ReturnStatement`) wrapping the closure call.
The instrumentation path differs slightly from the value-returning form; verify both paths are covered.
Expected:
- closure wrapper: hit=2
- case-1 body: hit=1
- case-2 body: hit=0
- injected null-default: hit=1 (same -1,-1 source position issue)

---

**Record — generated and user-defined methods**
```groovy
record Point(int x, int y) {
    static Point origin() { new Point(0, 0) }
    static void main(String[] args) {
        def p = new Point(3, 4)
        assert p.x() == 3
        assert p.y() == 4
        assert p.toString() != null
        def o = origin()
        assert o.x() == 0
    }
}
```
Expected:
- `origin()` method: 1 statement (`new Point(0,0)`), hit=1
- `toString()` (generated): appears in coverage model; body has ≥1 statement; hit=1 because `main` calls `p.toString()`
- `hashCode()` (generated): appears in coverage model; hit=0 (not called in main)
- `equals()` (generated): appears in coverage model; hit=0 (not called in main)
- Constructor (via `TupleConstructor`): may or may not appear depending on how Clover handles
  `@TupleConstructor`-generated constructors; verify no crash

---

**Sealed interface — basic verification**
```groovy
sealed interface Shape permits Circle, Square {}
final class Circle implements Shape {
    final float radius
    Circle(float r) { this.radius = r }
}
final class Square implements Shape {
    final float side
    Square(float s) { this.side = s }
}
class SealedTest {
    static String name(Shape s) {
        if (s instanceof Circle) return "circle"
        if (s instanceof Square) return "square"
        return "unknown"
    }
    static void main(String[] args) {
        assert name(new Circle(1.0f)) == "circle"
        assert name(new Square(1.0f)) == "square"
    }
}
```
No special expected hit-count structure; test primarily verifies no crash during instrumentation and
that regular method/branch counts are recorded correctly.

---

**Enhanced range syntax**
```groovy
class RangeTest {
    static List<Integer> leftOpen(int from, int to) {
        return (from<..to).toList()
    }
    static List<Integer> bothOpen(int from, int to) {
        return (from<..<to).toList()
    }
    static void main(String[] args) {
        assert leftOpen(0, 3) == [1, 2, 3]
        assert bothOpen(0, 3) == [1, 2]
    }
}
```
Expected: methods `leftOpen` and `bothOpen` each have 1 statement, hit=1.

---

**Decimal literal without leading zero**
```groovy
class DecimalLiteralTest {
    static double half() { .5 }
    static double quarter() { .25 }
    static void main(String[] args) {
        assert half() == 0.5
        assert quarter() == 0.25
    }
}
```
Expected: each method has 1 statement, hit=1.

---

### 2. Test Class Structure

```groovy
package org.openclover.groovy.instr

import groovy.transform.CompileStatic
import org.openclover.buildutil.test.junit.GroovyVersionStart
import org.openclover.core.CloverDatabase
import org.openclover.core.CodeType
import org.openclover.core.CoverageDataSpec
import org.openclover.core.api.registry.MethodInfo
import org.openclover.core.api.registry.PackageInfo
import org.openclover.core.api.registry.StatementInfo
import org.openclover.core.registry.entities.FullClassInfo
import org.openclover.core.registry.entities.FullFileInfo

@CompileStatic
class Groovy4CoverageRecordingTest extends TestBase {

    Groovy4CoverageRecordingTest(String testName) { super(testName) }
    Groovy4CoverageRecordingTest(String methodName, String specificName,
                                  File groovyAllJar, List<File> additionalGroovyJars) {
        super(methodName, specificName, groovyAllJar, additionalGroovyJars)
    }

    // Each test method below is exercised twice by the TestSuite:
    //   once with plain Groovy source, once with @CompileStatic added to the tested class.

    @GroovyVersionStart("4.0.0")
    void testSwitchArrowWithDefaultHitCounts() { ... }

    @GroovyVersionStart("4.0.0")
    void testSwitchArrowNoDefaultNullBranch() { ... }

    @GroovyVersionStart("4.0.0")
    void testSwitchYieldWithDefaultHitCounts() { ... }

    @GroovyVersionStart("4.0.0")
    void testSwitchYieldNoDefaultNullBranch() { ... }

    @GroovyVersionStart("4.0.0")
    void testSwitchVoidHitCounts() { ... }

    @GroovyVersionStart("4.0.0")
    void testRecordGeneratedAndUserMethodHitCounts() { ... }

    @GroovyVersionStart("4.0.0")
    void testSealedClassesDoNotCrash() { ... }

    @GroovyVersionStart("4.0.0")
    void testEnhancedRangeHitCounts() { ... }

    @GroovyVersionStart("4.0.0")
    void testDecimalLiteralWithoutLeadingZero() { ... }
}
```

**Running each test with and without `@CompileStatic`**: look at how `Groovy3CoverageRecordingTest`
achieves this via the `TestSuite` and the constructor that accepts `specificName`. The suite likely
calls each test method twice with different `groovyAllJar` / configuration. Replicate that pattern.

### 3. Register in TestSuite

Find `TestSuite.groovy` (or `.java`) in `clover-groovy/src/test/groovy/org/openclover/groovy/instr/`
and add `Groovy4CoverageRecordingTest` the same way `Groovy3CoverageRecordingTest` was added.

### 4. Potential Instrumentation Issues to Investigate

**Issue A: Injected default branch with `[-1:-1..-1:-1]` source position**
Switch expressions without an explicit `default` have a Groovyc-injected `EmptyStatement` singleton
with coordinates `[-1:-1..-1:-1]`. Fix: inside `InstrumentingCodeVisitor.visitSwitch()`, detect
`getDefaultStatement().getLineNumber() == -1` and synthesize a 1-character region at the closing `}`
of the `SwitchStatement` using `(lastLineNumber, lastColumnNumber-1) .. (lastLineNumber, lastColumnNumber)`.
Because `EmptyStatement$1` is a singleton and immutable, create a wrapper (e.g. a new `BlockStatement`
or pass the synthesized coordinates directly to the recording call) rather than mutating it.

**Issue B: Record auto-generated methods with `@CompileStatic`**
Records automatically get `@CompileStatic` on all generated methods. Their bodies run through the
CompileStatic instrumentation path. Check for NPE in `CloverAstTransformerInstructionSelection`
when processing generated method bodies that may have unusual AST shapes.

**Issue C: Switch expression variable scope balance**
The compiler introduces a synthetic `__$$sev0` local. Verify that `pushVariableScope` /
`popVariableScope` calls in `InstrumentingCodeVisitor` remain balanced when visiting the wrapping closure.

**Issue D: Sealed class `@Sealed` annotation on interfaces**
Verify that the `visitAnnotations()` no-op override in `InstrumentingCodeVisitor` correctly suppresses
visiting annotation attribute expressions (the `permits` class list is stored there).

### 5. Key Files to Read Before Starting

```
clover-groovy/src/main/java/org/openclover/groovy/instr/InstrumentingCodeVisitor.java
clover-groovy/src/main/java/org/openclover/groovy/instr/OperatorsInstrumenter.java
clover-groovy/src/main/java/org/openclover/groovy/instr/StatementInstrumenter.java
clover-groovy/src/main/java/org/openclover/groovy/instr/CloverAstTransformerInstructionSelection.java
clover-groovy/src/main/java/org/openclover/groovy/instr/GroovyUtils.java
clover-groovy/src/test/groovy/org/openclover/groovy/instr/Groovy3CoverageRecordingTest.groovy
clover-groovy/src/test/groovy/org/openclover/groovy/instr/TestBase.groovy
```

### 6. Key Design Decisions

- No new AST node classes in Groovy 4 — no new visitor methods needed in `InstrumentingCodeVisitor`.
- Switch expressions desugar to closures wrapping `SwitchStatement`; instrumentation flows through
  existing closure and switch paths.
- Record-generated methods are instrumented like any other method; they appear in coverage reports.
  No exclusion mechanism is planned (impossible to distinguish user-overridden vs generated when both
  share the same name and class annotation).
- The Groovy 2 Class.forName compatibility strategy is **not needed** for Groovy 4 features.
- Focus: write tests, run them, fix whatever breaks.

## Reference Commits

- OC-121 (Groovy 3 support): `3905c046..9600b99d` on master
  - Key: `e913ab23` — Class.forName pattern for LambdaExpression/MethodReferenceExpression
  - Key: `0f98ca2e` — DoWhileStatement instrumentation
  - Key: `130ba153` — default interface method instrumentation
- OC-31 (CompileStatic support): `3905c046` — see full commit message for split-phase strategy
