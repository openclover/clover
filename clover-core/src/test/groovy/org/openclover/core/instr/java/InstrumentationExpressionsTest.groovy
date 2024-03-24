package org.openclover.core.instr.java

import org.junit.Test

class InstrumentationExpressionsTest extends InstrumentationTestBase {

    @Test
    void testTernaryOperator()
            throws Exception {
        // just a simple assignment
        checkStatement("int i = arg == 2 ? 3 : 4;",
                "RECORDER.R.inc(1);int i = (((arg == 2 )&&(RECORDER.R.iget(2)!=0|true))||(RECORDER.R.iget(3)==0&false))? 3 : 4;")

        // two ternary's embeded
        checkStatement("int i = arg == (b==2?1:2) ? 3 : 4;",
                "RECORDER.R.inc(1);int i = (((arg == (" +
                        "(((b==2)&&(RECORDER.R.iget(2)!=0|true))||(RECORDER.R.iget(3)==0&false))?1:2" +
                        ") )&&(RECORDER.R.iget(4)!=0|true))||(RECORDER.R.iget(5)==0&false))? 3 : 4;")
    }

    @Test
    void testConstExpr()
            throws Exception {
        // constant for loop
        checkStatement(
                "for (;true;) {System.out.println(a[i]);}",
                "RECORDER.R.inc(1);for (;true;) {{RECORDER.R.inc(2);System.out.println(a[i]);}}")
    }


    @Test
    void testConditionalWithAssignment() throws Exception {
        // Don't instrument a conditional containing an assignment since this breaks Definite Assignment rules in javac
        checkStatement(
                "String line; while ((line = in.readLine()) != null) {System.out.println(line);}",
                "RECORDER.R.inc(1);String line; RECORDER.R.inc(2);while ((line = in.readLine()) != null) {{RECORDER.R.inc(5);System.out.println(line);}}")

    }

    @Test
    void testConditionalWithCloverOff() throws Exception {
        // Preserve a conditional and don't add extraneous parentheses when CLOVER:OFF is specified as this may trigger
        // compiler bugs for generic calls. See http://bugs.sun.com/view_bug.do?bug_id=6608961

        //Note Clover inserts extraneous curly braces too
        checkStatement(
                "\n///CLOVER:OFF\nif (a + b == 2) { System.out.println(\"Hello, world\"); }\n///CLOVER:ON\n",
                "\n///CLOVER:OFF\nif (a + b == 2) {{ System.out.println(\"Hello, world\"); }\n///CLOVER:ON\n}")

    }
}
