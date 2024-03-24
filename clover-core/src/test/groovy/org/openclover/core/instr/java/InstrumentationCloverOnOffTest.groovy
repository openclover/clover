package org.openclover.core.instr.java

import org.junit.Test

class InstrumentationCloverOnOffTest extends InstrumentationTestBase {

    @Test
    void testCloverOnOff() throws Exception {
        checkInstrumentation([
                ["/*///CLOVER:OFF*/ package A; class B { public B() {int i = 0;}}",
                 "/*///CLOVER:OFF*/ package A; class B { public B() {int i = 0;}}"],
                ["package A; /*///CLOVER:OFF*/ class B { public B() {int i = 0;}}",
                 "package A; /*///CLOVER:OFF*/ class B { public B() {int i = 0;}}"],
                ["/*CLOVER:OFF*/ class B { public B() {int i = 0;}}",
                 "/*CLOVER:OFF*/ class B { public B() {int i = 0;}}"],
                // test second directive prefix, added for eclipse auto-format support
                ["/*// /CLOVER:OFF*/ package A; class B { public B() {int i = 0;}}",
                 "/*// /CLOVER:OFF*/ package A; class B { public B() {int i = 0;}}"],
                ["package A; /*// /CLOVER:OFF*/ class B { public B() {int i = 0;}}",
                 "package A; /*// /CLOVER:OFF*/ class B { public B() {int i = 0;}}"],
                ["/*// /CLOVER:OFF*/ class B { public B() {int i = 0;}}",
                 "/*// /CLOVER:OFF*/ class B { public B() {int i = 0;}}"],

                ["class B { ///CLOVER:OFF\nprivate B() {int i = 0;}}",
                 "class B { ///CLOVER:OFF\nprivate B() {int i = 0;}}"],
                ["class B { ///CLOVER:OFF\nprivate B() {}}",
                 "class B { ///CLOVER:OFF\nprivate B() {}}"],
                ["class B { ///CLOVER:OFF\nprivate B() {}\n///CLOVER:ON\n}",
                 "class B { ///CLOVER:OFF\nprivate B() {}\n///CLOVER:ON\n}"],
                ["class B { private B() {///CLOVER:OFF\n}\n///CLOVER:ON\n}",
                 "class B {$classField$snifferField private B() {RECORDER.R.inc(0);///CLOVER:OFF\n}\n///CLOVER:ON\n}"],
                ["class B { private B() {///CLOVER:OFF\nint i = 0;///CLOVER:ON\n}}",
                 "class B {$classField$snifferField private B() {RECORDER.R.inc(0);///CLOVER:OFF\nint i = 0;///CLOVER:ON\n}}"],
                ["class B { private B() {///CLOVER:OFF\nhashCode();///CLOVER:ON\n}}",
                 "class B {$classField$snifferField private B() {RECORDER.R.inc(0);///CLOVER:OFF\nhashCode();///CLOVER:ON\n}}"],

        ] as String[][])

        checkInstrumentation([
                ["/*///CLOVER:OFF*/ public class MyTest { @Test public void foo(){} }",
                 "/*///CLOVER:OFF*/ public class MyTest { @Test public void foo(){} }"],
                ["public class MyTest {/*///CLOVER:OFF*/ @Test public void foo(){} }",
                 "public class MyTest {/*///CLOVER:OFF*/ @Test public void foo(){} }"],
                ["public class MyTest { @Test public void foo()/*///CLOVER:OFF*/{} }",
                 "public class MyTest { @Test public void foo()/*///CLOVER:OFF*/{} }"],
                ["/*///CLOVER:OFF*/ public class MyTest { public void testFoo(){} }",
                 "/*///CLOVER:OFF*/ public class MyTest { public void testFoo(){} }"],
                ["public class MyTest {/*///CLOVER:OFF*/ public void testFoo(){} }",
                 "public class MyTest {/*///CLOVER:OFF*/ public void testFoo(){} }"],
                ["public class MyTest { public void testFoo()/*///CLOVER:OFF*/{} }",
                 "public class MyTest { public void testFoo()/*///CLOVER:OFF*/{} }"]
        ] as String[][])
    }
}
