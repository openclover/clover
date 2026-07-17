OC-249: Constructors incorrectly instrumented as test methods

Context

GitHub issue #249: when a class in a test source set has a
constructor (reported for a static inner class inside a test class), OpenClover can treat that constructor as a
test method and apply per-test rewriting. The rewrite moves the constructor body into a synthetic private
helper method, so super(...) / this(...) ends up inside the helper rather than the constructor, and the
renamed signature loses its constructor-ness — the instrumented source no longer compiles.

The issue's suggested workaround is to narrow the test patterns (@Test-only, test*-only, top-level .*Test
classes), which confirms the trigger: it is the user-configured test spec, not the built-in heuristic.

Root cause

Constructors are parsed by standardConstructorSignature / compactConstructorSignature in
clover-core/src/main/java/org/openclover/core/instr/java/java.g (~lines 1255, 1288), which build a
MethodSignature with a null returnType — that null is the codebase's existing marker for "not a real
method". constructorBody (java.g ~1923) then calls the same instrEnterMethod helper as ordinary methods, so a
constructor gets a MethodRegistrationNode and a MethodEntryInstrEmitter just like a method does.

DefaultTestDetector.isMethodMatch already guards against this — clover-core/.../instr/tests/DefaultTestDetector.java:53
has && signature.getReturnType() != null with the comment // no ctors.

TestSpec.isMethodMatch has no such guard (clover-core/.../instr/tests/TestSpec.java:103-110). It only
checks name / annotation / return-type / tag patterns, and methodReturnPatternMatches(null) returns true
whenever no return-type pattern is configured. So any user test spec matching by class pattern plus a permissive
method pattern also matches every constructor in that class. The match then reaches
MethodRegistrationNode.init (clover-core/.../instr/java/MethodRegistrationNode.java:62-66), where
addTestRewriteInstr = isInstrEnabled() && cfg.isRecordTestResults() && isTestMethod triggers the broken rewrite
(it emits signature.getRenamedNormalisedSignature(syntheticTestName) and delegates the body).

Approach

Two changes — fix the root cause, plus a guard so no TestDetector implementation (including the Eclipse plugin's
and user-supplied ones) can trigger the broken rewrite again.

1. Add an explicit constructor marker on MethodSignature

clover-core/src/main/java/org/openclover/core/registry/entities/MethodSignature.java

Add a small accessor documenting the existing convention rather than sprinkling getReturnType() != null checks:

/** Constructors and initializers are parsed with a null return type. */
public boolean isConstructorLike() {
    return returnType == null;
}

Note: instance/static initializers (java.g:1869, 1880) also carry a null returnType; they are already
non-renameable (normSeqPrefix == null), so the shared name is accurate. Keep the check field-based — do not
change how the grammar builds signatures, and do not touch write/read, which are not exercised for ctors.

2. Exclude constructors from test matching in TestSpec

clover-core/src/main/java/org/openclover/core/instr/tests/TestSpec.java — in isMethodMatch, mirror the
DefaultTestDetector guard:

final MethodSignature signature = methodContext.getSignature();
return !signature.isConstructorLike()      // no ctors — see OC-249
        && methodMatches(signature.getName())
        && ...;

Also switch DefaultTestDetector.java:53 to the new accessor so the convention lives in one place.

3. Guard the Java instrumenter (defence in depth)

Both Java call sites compute isTestMethod independently and must agree:

- MethodRegistrationNode.init (line 62) — state.isDetectTests() && !signature.isConstructorLike() && state.getTestDetector().isMethodMatch(...)
- MethodEntryInstrEmitter.init (line 29-32) — same additional condition

This makes the broken rewrite unreachable for constructors regardless of the detector in use. The Groovy path
(InstrumentingCodeVisitor:308) visits ConstructorNode separately and is out of scope.

Files to change

- clover-core/src/main/java/org/openclover/core/registry/entities/MethodSignature.java
- clover-core/src/main/java/org/openclover/core/instr/tests/TestSpec.java
- clover-core/src/main/java/org/openclover/core/instr/tests/DefaultTestDetector.java
- clover-core/src/main/java/org/openclover/core/instr/java/MethodRegistrationNode.java
- clover-core/src/main/java/org/openclover/core/instr/java/MethodEntryInstrEmitter.java
- CHANGELOG.md — add under ### Fixed: Issue #249: Constructors in test classes were instrumented as test methods, producing uncompilable code.

Tests

- clover-core/src/test/groovy/org/openclover/core/instr/tests/TestSpecTest.groovy — add a case: a TestSpec
with a permissive method pattern (e.g. .*) and no return-type pattern must not match a constructor
signature (new MethodSignature(..., "MyInnerClass", null, null, params, null)), but must still match a normal
method. Follow the existing spec.isMethodMatch(null, JavaMethodContext.createFor(sig)) style.
- clover-core/src/test/groovy/org/openclover/core/instr/java/InstrumentationTestMethodsTest.groovy — add a
regression case reproducing the issue: source with a test class containing a static inner class whose
constructor calls super(), instrumented with recordTestResults enabled and a TestSpec-based detector.
Assert the constructor keeps its own body (plain RECORDER.R.inc(n); after the super() call, per the
constructorBody skip-token behaviour) and that no synthetic renamed method is emitted. Use the
checkInstrumentation(String[][], JavaInstrumentationConfig) overload from InstrumentationTestBase.groovy:81
so the custom detector can be configured.

Verification

1. mvn -pl clover-core test -Dtest='TestSpecTest,DefaultTestDetectorTest,InstrumentationTestMethodsTest,InstrumentationDetectionTest'
— new cases pass, existing ones unchanged.
2. Full mvn -pl clover-core test to catch golden-string regressions in the other Instrumentation*Test classes.
3. End-to-end: take the issue's reproducer (test class with a static inner class whose ctor calls super()), run
CloverInstr over it with a permissive -tsm/test-spec configuration and recordTestResults on, then
javac the instrumented output — it must compile. Before the fix it fails; after, it compiles.
