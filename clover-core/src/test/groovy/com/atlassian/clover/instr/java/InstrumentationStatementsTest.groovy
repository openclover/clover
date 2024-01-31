package com.atlassian.clover.instr.java

import org.junit.Test

class InstrumentationStatementsTest extends InstrumentationTestBase {

    @Test
    void testDiamondTypeArgs() throws Exception {
        checkInstrumentation([
                [ "class B { private void a(int arg) { List<String> l = new ArrayList<>(); new Foo<>(); }}",
                  "class B {$snifferField private void a(int arg) {RECORDER.inc(0); RECORDER.inc(1);List<String> l = new ArrayList<>(); RECORDER.inc(2);new Foo<>(); }}"]
        ] as String[][])
    }

    @Test
    void testStatements() throws Exception {
        checkInstrumentation([
                [ "class B { private void a(int arg) {hashCode();}}",
                  "class B {$snifferField private void a(int arg) {RECORDER.inc(0);RECORDER.inc(1);hashCode();}}" ]
        ] as String[][])
    }
}
