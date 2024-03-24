package org.openclover.core.instr.java

import org.junit.Test

class InstrumentationForLoopTest extends InstrumentationTestBase {

    @Test
    void testForLoop()
            throws Exception {
        // traditional for loop
        checkStatement(
                "for (int i = 0; i < a.length; i++) {System.out.println(a[i]);}",
                "RECORDER.R.inc(1);for (int i = 0; (((i < a.length)&&(RECORDER.R.iget(2)!=0|true))||(RECORDER.R.iget(3)==0&false)); i++) {{RECORDER.R.inc(4);System.out.println(a[i]);}}")
        // traditional for loop, no braces
        checkStatement(
                "for (int i = 0; i < a.length; i++) System.out.println(a[i]);",
                "RECORDER.R.inc(1);for (int i = 0; (((i < a.length)&&(RECORDER.R.iget(2)!=0|true))||(RECORDER.R.iget(3)==0&false)); i++) {RECORDER.R.inc(4);System.out.println(a[i]);}")
        // enhanced for loop
        checkStatement(
                "for (int i : a) {System.out.println(i);}",
                "RECORDER.R.inc(1);for (int i : a) {{RECORDER.R.inc(2);System.out.println(i);}}")
    }
}
