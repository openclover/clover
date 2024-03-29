package org.openclover.core.instr.java

import org.junit.Test
import org.openclover.runtime.api.CloverException

import static org.junit.Assert.fail

class InstrumentationDetectionTest extends InstrumentationTestBase {

    // tests that "empty" classes don't get a recorder member when they don't need one
    @Test
    void testDirtyDetection() throws Exception {
        checkInstrumentation([
                ["class A {}", "class A {}"],
                ["interface A {}", "interface A {}"],
                ["@interface A {}", "@interface A {}"],
                ["enum A {}", "enum A {}"],
                ["enum A { apple,banana,pear }", "enum A { apple,banana,pear }"],
                ["enum A { apple,banana,pear }", "enum A { apple,banana,pear }"],
                // second top-level class.
                ["class A {public A(){}} class B {}","class A {$classField${snifferField}public A(){RECORDER.R.inc(0);}} class B {}"]
        ] as String[][], true)
    }

    @Test
    void testDoubleInstrDetection() throws Exception {
        checkInstrumentation([
                [ "class A {}", "class A {}" ], //input is smaller than the marker
                [ "", "" ] // empty source file
        ] as String[][]);
        try {
            checkInstrumentation([
                    [CloverTokenStreamFilter.MARKER +"*/" ]
            ] as String[][])
            fail("instrumentation marker not detected")
        }
        catch (CloverException e) {

        }
    }
}
