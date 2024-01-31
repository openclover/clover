package com.atlassian.clover.instr.java

import org.junit.Test

import static org.junit.Assert.assertNotNull

class InstrumentationLiteralsTest extends InstrumentationTestBase {

    @Test
    void testNumericLiterals() throws Exception {
        //More an test of language recognition than an instrumentation test
        //Tests int, long, float, doubles in hex, binary, decimal, octal, some with underscores, some without
        checkInstrumentation([
                [ "class B { static double[] doubles = new double[] { 09, 0_9, 0_0, 0x1_2_3, 1234_5678, 1_2_3_4__5_6_7_8L, 0b0001_0010_0100_1000, 3.141_592_653_589_793d, 0x1.ffff_ffff_ffff_fP1_023, 0x1111_2222_3333_4444L, 0x0.0000000000001P-1f, 0x0.0000000000001P-1_1d }; }",
                  "class B { static double[] doubles = new double[] { 09, 0_9, 0_0, 0x1_2_3, 1234_5678, 1_2_3_4__5_6_7_8L, 0b0001_0010_0100_1000, 3.141_592_653_589_793d, 0x1.ffff_ffff_ffff_fP1_023, 0x1111_2222_3333_4444L, 0x0.0000000000001P-1f, 0x0.0000000000001P-1_1d }; }"]
        ] as String[][])
    }

    @Test
    void testBinaryLiterals() throws Exception {
        //More an test of language recognition than an instrumentation test
        //Test binary literals (int, long with underscores)
        checkInstrumentation([
                [ "class B { static double[] doubles = new double[] { 0b0, 0b1, 0b10101010, 0b1010_1010, 0b1010_1010L, 0b10101010L }; }",
                  "class B { static double[] doubles = new double[] { 0b0, 0b1, 0b10101010, 0b1010_1010, 0b1010_1010L, 0b10101010L }; }"]
        ] as String[][])
    }

    /**
     * Test for CLOV-669
     */
    @Test
    void testUnicodeCharacters()
            throws Exception {

        String instr = getInstrumentedVersion("class B { private void a(int arg) {String s = \"\u023a\";}}", false)
        assertNotNull(instr)
    }

}
