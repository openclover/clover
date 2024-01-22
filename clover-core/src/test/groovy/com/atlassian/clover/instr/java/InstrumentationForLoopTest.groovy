package com.atlassian.clover.instr.java

import org.junit.Test

class InstrumentationForLoopTest extends InstrumentationTestBase {

    @Test
    void testForLoop()
            throws Exception {
        // traditional for loop
        checkStatement(
                "for (int i = 0; i < a.length; i++) {System.out.println(a[i]);}",
                "RECORDER.inc(1);for (int i = 0; (((i < a.length)&&(RECORDER.iget(2)!=0|true))||(RECORDER.iget(3)==0&false)); i++) {{RECORDER.inc(4);System.out.println(a[i]);}}")
        // traditional for loop, no braces
        checkStatement(
                "for (int i = 0; i < a.length; i++) System.out.println(a[i]);",
                "RECORDER.inc(1);for (int i = 0; (((i < a.length)&&(RECORDER.iget(2)!=0|true))||(RECORDER.iget(3)==0&false)); i++) {RECORDER.inc(4);System.out.println(a[i]);}")
        // enhanced for loop
        checkStatement(
                "for (int i : a) {System.out.println(i);}",
                "RECORDER.inc(1);for (int i : a) {{RECORDER.inc(2);System.out.println(i);}}")
    }
}
