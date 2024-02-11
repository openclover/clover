package org.openclover.core.instr.java

import org.junit.Test

class InstrumentationMethodEntriesTest extends InstrumentationTestBase {
    @Test
    void testMethodEntries()
            throws Exception {
        checkInstrumentation([
                ["class B { B(int arg) {}}",
                 "class B {$snifferField B(int arg) {RECORDER.inc(0);}}"],
                ["class B { public B(int arg) {}}",
                 "class B {$snifferField public B(int arg) {RECORDER.inc(0);}}"],
                ["class B { protected B(int arg) {}}",
                 "class B {$snifferField protected B(int arg) {RECORDER.inc(0);}}"],
                ["class B { private B(int arg) {}}",
                 "class B {$snifferField private B(int arg) {RECORDER.inc(0);}}"],
                ["class B { void a(int arg) { }}",
                 "class B {$snifferField void a(int arg) {RECORDER.inc(0); }}"],
                ["class B { public void a(int arg) { }}",
                 "class B {$snifferField public void a(int arg) {RECORDER.inc(0); }}"],
                ["class B { protected void a(int arg) { }}",
                 "class B {$snifferField protected void a(int arg) {RECORDER.inc(0); }}"],
                ["class B { private void a(int arg) { }}",
                 "class B {$snifferField private void a(int arg) {RECORDER.inc(0); }}"],
                ["class B { private void a(int arg) {}\nprivate void b() {}\nprivate void c() {}}",
                 "class B {$snifferField private void a(int arg) {RECORDER.inc(0);}\nprivate" +
                         " void b() {RECORDER.inc(1);}\nprivate void c() {RECORDER.inc(2);}}"]
        ] as String[][])
    }
}
