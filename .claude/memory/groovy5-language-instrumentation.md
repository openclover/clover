# OC-282: Groovy 5 Language Instrumentation — Specification

## Context

Branch: `OC-282-support-groovy-5`
Reference issue: https://github.com/openclover/clover/issues/282
Based on analysis against Groovy 5.0.4 (latest stable as of research date).

Previous: OC-192 (Groovy 4, branch `OC-192-support-groovy-4`)

---

## How Groovy 5 Differs from Groovy 4 (AST Inventory)

### Maven Coordinates
| Groovy 4 | Groovy 5 |
|----------|---------|
| `org.apache.groovy:groovy:4.x` | `org.apache.groovy:groovy:5.x` |

Java package names (`org.codehaus.groovy.*`) are **unchanged** in Groovy 5 — backward compatible.

### Java Version Requirement
Groovy 5 requires Java **11 minimum** (Groovy 4 supports Java 8+).

### AST Node Inventory (key finding)
Groovy 5 has **the same `org.codehaus.groovy.ast.expr.*` and `org.codehaus.groovy.ast.stmt.*` classes** as Groovy 4.
No new `Expression` or `Statement` subclasses were added.

However, **two existing classes changed significantly**:
- `ForStatement` — new `indexVariable` field (new for-loop syntax)
- `ClassCodeExpressionTransformer` — added `visitClosureExpression` method

---

## New Groovy 5 Language Features & AST Analysis

### 1. Logical Implication Operator (`==>`)

**Syntax:**
```groovy
assert false ==> true     // true: "if false, anything holds"
assert true ==> true      // true
assert !(true ==> false)  // false: "if true, false doesn't hold"
```

**AST:**
`BinaryExpression(left=VariableExpr, op="==>", right=VariableExpr)` — same as any other binary operator (token type 99).

**Instrumentation:** No changes needed. `transform()` handles it via `super.transform()` → `BinaryExpression.transformExpression()`. No special branch coverage tracking is done for `==>` (same policy as `&&`, `||`).

---

### 2. Pattern Matching for `instanceof` (JEP 394)

**Syntax:**
```groovy
if (obj instanceof String s) {
    println s.toUpperCase()    // 's' is bound here
}
```

**AST (after SEMANTIC_ANALYSIS phase):**
```
IfStatement {
  booleanExpression: BooleanExpression {
    expression: BinaryExpression {
      left:  VariableExpression(obj)
      op:    instanceof (token type 544)
      right: DeclarationExpression(VariableExpression(s), =, ...)
    }
  }
}
```
The RHS of `instanceof` is a `DeclarationExpression` instead of the usual `ClassExpression`.

**Impact on Clover's branch instrumentation:**
`BranchInstrumenter.transformBranch()` wraps the entire `BooleanExpression` in additional `&&`/`||` for branch recording:
```groovy
// Original: obj instanceof String s
// Becomes:  (obj instanceof String s && R.iget(0) != 0 || true) || (R.iget(1) == 0 && false)
```

**Verified** (Groovy 5.0.4 runtime test): The pattern binding variable `s` IS accessible in the `if` block even when the condition is wrapped in `&&`/`||`. Groovy's semantic analysis establishes the binding scope before Clover instruments.

The `DeclarationExpression` on the RHS passes through `InstrumentingCodeVisitor.transform()` safely — it doesn't match any of the special cases and falls through to `super.transform()`.

**Instrumentation:** No changes needed for compilation or basic coverage tracking. Branch coverage of the if-else is correctly recorded.

**Limitation:** The pattern variable `s` is never directly tracked as a statement. This is acceptable — same behavior as any local variable declaration inside an expression.

---

### 3. Index Variable in For Loops

**Syntax:**
```groovy
for (int idx, var item in items) {
    println "$idx: $item"
}
```

**AST change — `ForStatement` in Groovy 5:**
```java
// Groovy 4: ForStatement had one variable
ForStatement(Parameter variable, Expression collection, Statement loopBlock)

// Groovy 5: ForStatement has TWO variables  
ForStatement(Parameter indexVariable, Parameter valueVariable, Expression collection, Statement loopBlock)
```

New fields:
- `indexVariable` — the `int idx` part (Parameter)
- `valueVariable` — the `var item` part (Parameter)
- `getVariable()` — still works, returns `valueVariable` (backward compat)
- `getIndexVariable()` — new method returning the index variable

**Impact on `InstrumentingCodeVisitor.visitForLoop()`:**
Our override uses:
- `forLoop.getVariableScope()` ✓ unchanged
- `forLoop.getCollectionExpression()` ✓ unchanged
- `forLoop.setCollectionExpression()` ✓ unchanged
- `forLoop.getLoopBlock()` ✓ unchanged
- `forLoop.setLoopBlock()` ✓ unchanged

We do NOT use `getVariable()` directly. The `indexVariable` is a `Parameter` (type declaration), not an expression — no transformation needed.

**Instrumentation:** No code changes needed. Existing `visitForLoop` instruments correctly. The index variable is tracked as part of the loop scope automatically. **Write a test to verify.**

---

### 4. Multi-Assignment with `var`

**Syntax:**
```groovy
var (x, y) = [1, 2]
```

**AST:**
`DeclarationExpression(TupleExpression([VariableExpression(x), VariableExpression(y)]), =, ListExpression)` — identical to `def (x, y) = [1, 2]`.

**Instrumentation:** No changes needed. Existing handling of `DeclarationExpression` covers this.

---

### 5. Underscore Placeholder (`_`)

**Syntax:**
```groovy
var (_, second) = [1, 2]            // multi-assign — ignore first element
def c = (_, _, a, b) -> a + b       // lambda — ignore first two params
def f = { a, _, _, b -> a + b }     // closure — ignore unused params
```

**AST:** `_` is a regular `VariableExpression` named `"_"` in `TupleExpression` or closure parameters. No new node type.

**Instrumentation:** No changes needed.

---

### 6. Multidimensional Array Literals

**Syntax:**
```groovy
int[][] numsB = [[1,2,3],[4,5,6]]       // uses ListExpression on RHS
def numsD = new int[][] {{1,2,3},{4,5,6}}  // uses ArrayExpression
```

**AST:**
- `[[1,2,3],[4,5,6]]` → `DeclarationExpression(var, =, ListExpression)` — same as nested list
- `new int[][] {{1,2,3},{4,5,6}}` → `ConstructorCallExpression` / `ArrayExpression(elementType=int[], [ArrayExpression(int, [1,2,3]), ArrayExpression(int, [4,5,6])])`

**Instrumentation:** No changes needed. `ArrayExpression` already existed.

---

### 7. Interface Native Default, Private, and Static Methods

**Syntax:**
```groovy
interface MyInterface {
    default void myMethod() { println "default" }
    private void helper() { println "private" }
    static void utility() { println "static" }
}
```

In Groovy 4 and earlier, interface default methods went through the `$Trait$Helper` companion class mechanism. In Groovy 5, they are **native JVM interface methods** (JDK 8+ feature).

**AST:** The class is still an `interface` (`classNode.isInterface() == true`). Methods with bodies are NOT abstract. The `isSynthetic()` flag should be false.

**Instrumentation:** Already handled from Groovy 3 (OC-121). `isInstrumentable(ClassNode)` returns `true` for interfaces. `isInstrumentable(MethodNode)` returns `true` for non-abstract, non-synthetic methods. **Write a test to verify native interface methods in Groovy 5 are instrumented.**

**Risk:** If Groovy 5 changes how it handles the `$Trait$Helper` path or if static interface methods are flagged differently, instrumentation could break. **Needs verification.**

---

### 8. Instance Main / Run Methods (JEP 512)

**Syntax (in a Groovy script):**
```groovy
// Instead of static main(String[] args)
def main() {
    println 'Hello!'
}
// Or as a run-style method
def run() {
    println 'Hello!'
}
```

**AST:** Scripts compile to a class. The `main()` or `run()` become instance `MethodNode`s (not static, not synthetic, not abstract). The script class `run()` method already has special handling in `visitConstructorOrMethod`:
```java
boolean isScriptRun = GroovyUtils.isScriptClass(method.getDeclaringClass()) && "run".equals(method.getName());
```
For instance `main()`, there's no existing special case. It would be instrumented as a regular method, which is correct.

**Instrumentation:** No changes needed. Instance `main()` is instrumented normally. Script `run()` uses class-level source region (existing behavior). **Write a test.**

---

### 9. Named Capturing Groups in Regex (`=~` operator)

**Syntax:**
```groovy
def m = "Groovy 5.0.0" =~ /(\w+) (\d+\.\d+\.\d+)/
println m[0]['project']   // named group access
```

**AST:** `BinaryExpression(StringExpr, "=~", StringExpr)` as before. Named groups are a runtime matcher feature, not an AST change.

**Instrumentation:** No changes needed.

---

### 10. `@OperatorRename` AST Transform

**Syntax:**
```groovy
@OperatorRename(plus='add')
class MyNum { ... }
```

**Impact:** Pure annotation processing, no new AST node types. Clover skips annotations (`visitAnnotations` is overridden to do nothing).

**Instrumentation:** No changes needed.

---

## `ClassCodeExpressionTransformer` Changes in Groovy 5

In Groovy 4, `ClassCodeExpressionTransformer` (base class of `InstrumentingCodeVisitor`) did NOT have `visitClosureExpression`.

In Groovy 5, it now has:
```java
// Groovy 5 ClassCodeExpressionTransformer
public void visitClosureExpression(ClosureExpression expression) {
    if (expression.isParameterSpecified()) {
        for (Parameter p : expression.getParameters()) {
            Expression init = p.getInitialExpression();
            if (init != null) {
                p.setInitialExpression(transform(init));  // transforms default param exprs
            }
        }
    }
    super.visitClosureExpression(expression); // ClassCodeVisitorSupport — visits code
}
```

Our `InstrumentingCodeVisitor.visitClosureExpression` overrides this completely and does NOT call super. This means:
- We correctly instrument the closure body (our code handles this)
- We do NOT transform closure parameter default expressions (e.g., `def f = { String s = someElvis ?: "x" -> ... }`)
- This is a **pre-existing limitation** (same gap in Groovy 4, since the base class didn't have the method)

**Decision:** No change needed for initial Groovy 5 support. Document as known limitation.

---

## Summary: What Needs to Change

### Infrastructure Changes (required)

#### 1. `pom.xml` in `clover-groovy`
```xml
<!-- In compatibility profile, copy-groovy-jars execution, add: -->
<artifactItem>
    <groupId>org.apache.groovy</groupId>
    <artifactId>groovy</artifactId>
    <version>5.0.4</version>   <!-- or latest 5.x -->
</artifactItem>

<!-- Default build version should eventually switch to 5.x.
     For now, add 5.x to the compatibility test matrix. -->
<!-- In <properties>: -->
<clover-groovy.test.groovy.ver>4.0.28</clover-groovy.test.groovy.ver>
<!-- (keep on 4.x for compilation, add 5.x as cross-version test) -->
```

#### 2. `JavaVersionMixin.groovy`
```groovy
Map<String, Pair<String, String>> GROOVY_TO_JAVA_VERSIONS = [
    "2.": Pair.of(JAVA_8, JAVA_8),
    "3.": Pair.of(JAVA_8, JAVA_11),
    "4.": Pair.of(JAVA_8, JAVA_17),
    "5.": Pair.of(JAVA_11, JAVA_17)   // ← ADD THIS
    // JAVA_17 detection tops out at 17 regardless of actual JVM; effectively "Java 11+"
]
```

#### 3. `TestSuite.groovy`
```groovy
(Groovy5CoverageRecordingTest): DefaultTestSelector.instance.closure,
```

### New Test File (required)

#### `Groovy5CoverageRecordingTest.groovy`

New class analogous to `Groovy4CoverageRecordingTest.groovy`. All methods annotated `@GroovyVersionStart("5.0.0")`.

Test methods to write:

| Test | What to verify |
|---|---|
| `testLogicalImplicationHitCounts` | `a ==> b` operator; verify method hit counts, no crash |
| `testPatternMatchingInstanceofHitCounts` | `if (obj instanceof String s)` branch coverage; verify `s.method()` is tracked in if-block |
| `testPatternMatchingInstanceofNoMatch` | Same pattern, object doesn't match; verify false-branch coverage |
| `testIndexedForLoopHitCounts` | `for (int idx, var item in items)` loop; verify loop body hit count = list size |
| `testMultiAssignWithVarHitCounts` | `var (x, y) = [1, 2]`; verify statement hit, x+y computed correctly |
| `testUnderscorePlaceholderHitCounts` | `var (_, second) = [1, 2]`; verify second is accessible |
| `testInterfaceNativeDefaultMethodHitCounts` | Interface with `default void foo()` body; verify method is instrumented |
| `testInterfacePrivateMethodHitCounts` | Interface with `private void helper()` body; verify method is instrumented |
| `testInterfaceStaticMethodHitCounts` | Interface with `static void util()` body; verify method is instrumented |
| `testInstanceMainMethodHitCounts` | Script with `def main()` (instance); verify it gets instrumented and hit=1 |
| `testMultiDimArrayLiteralHitCounts` | `new int[][] {{1,2,3}}` and `int[][] x = [[1,2,3]]`; no crash |

### Code Changes to `InstrumentingCodeVisitor.java`

**None required** based on analysis. All Groovy 5 features use existing AST node types.

The `ForStatement.indexVariable` is a `Parameter` (no expression to transform); existing `visitForLoop` already handles the collection expression and loop block correctly. The parent class `ClassCodeExpressionTransformer.visitForLoop` calls `visitAnnotations(forLoop.getVariable())` but we override `visitForLoop` entirely and don't call super, so no issue.

### Potential Risk: Groovy 5 Compilation Mode

Clover runs as an `ASTTransformation` registered in a `.groovy` meta-inf services file. Verify that the transformation registration still works with Groovy 5's compiler pipeline. The `org.codehaus.groovy.transform.ASTTransformation` interface is unchanged.

---

## Files to Change

```
clover-groovy/pom.xml
  └─ +artifactItem: groovy:5.0.4 in compatibility profile
  └─ (optionally update default version in properties)

clover-groovy/src/test/groovy/org/openclover/groovy/test/junit/JavaVersionMixin.groovy
  └─ +entry "5.": Pair.of(JAVA_11, JAVA_17) in GROOVY_TO_JAVA_VERSIONS map

clover-groovy/src/test/groovy/org/openclover/groovy/instr/TestSuite.groovy
  └─ +Groovy5CoverageRecordingTest in TEST_CLASSES_AND_SELECTORS

clover-groovy/src/test/groovy/org/openclover/groovy/instr/Groovy5CoverageRecordingTest.groovy
  └─ NEW — test class with @GroovyVersionStart("5.0.0") tests
```

No changes in `src/main/java/` are expected (no new AST node handling required).

---

## Features NOT in scope (experimental in Groovy 5 as of research date)

- GINQ v2 improvements — still incubating
- Any `@Incubating`-annotated features

---

## Reference Commits (prior work)

- OC-121 (Groovy 3): commits `cbc3e511`, `e913ab23`, `130ba153`, `0f98ca2e`
- OC-192 (Groovy 4): commits `d63eef2d`, `b682af98`

## Run Command (once implemented)

```bash
mvn test -pl clover-groovy -Dtest="Groovy5CoverageRecordingTest" \
    -Dclover.test.groovyversion.includes=5.0
```

For cross-version matrix (compatibility profile):
```bash
mvn test -pl clover-groovy -P compatibility \
    -Dclover.test.groovyversion.includes=5.0
```
