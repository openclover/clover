package org.openclover.core.instr.java

import org.junit.Test
import org.openclover.core.cfg.instr.InstrumentationConfig
import org.openclover.core.cfg.instr.InstrumentationLevel
import org.openclover.core.cfg.instr.java.JavaInstrumentationConfig

class InstrumentationMethodLevelTest extends InstrumentationTestBase {

    @Test
    void testMethodLevelInstr() throws Exception {
        checkStatement("int i = 0;", "int i = 0;", InstrumentationLevel.METHOD.ordinal())
        checkStatement("int i = arg == 2 ? 3 : 4;", "int i = arg == 2 ? 3 : 4;", InstrumentationLevel.METHOD.ordinal())
        checkStatement("assert arg > 0;", "assert arg > 0;", InstrumentationLevel.METHOD.ordinal())

        JavaInstrumentationConfig config = getInstrConfig(newDbTempFile().getAbsolutePath(), false, true, false)
        config.setFlushPolicy(InstrumentationConfig.INTERVAL_FLUSHING)
        config.setInstrLevel(InstrumentationLevel.METHOD.ordinal())
        checkInstrumentation([
                ["class B { public B(int arg) {}}",
                 "class B {$classField$snifferField public B(int arg) {try{RECORDER.R.inc(0);}finally{RECORDER.R.maybeFlush();}}}"]
        ] as String[][],
                config)

        config = getInstrConfig(newDbTempFile().getAbsolutePath(), false, true, false)
        config.setFlushPolicy(InstrumentationConfig.INTERVAL_FLUSHING)
        config.setInstrLevel(InstrumentationLevel.METHOD.ordinal())

        checkInstrumentation([
                ["class B { public B(int arg) {int i = 0;}}",
                 "class B {$classField$snifferField public B(int arg) {try{RECORDER.R.inc(0);int i = 0;}finally{RECORDER.R.maybeFlush();}}}"]
        ] as String[][],
                config)

        config = getInstrConfig(newDbTempFile().getAbsolutePath(), false, true, false)
        config.setFlushPolicy(InstrumentationConfig.THREADED_FLUSHING)
        config.setInstrLevel(InstrumentationLevel.METHOD.ordinal())

        checkInstrumentation([
                ["class B { public B(int arg) {}}",
                 "class B {$classField$snifferField public B(int arg) {try{RECORDER.R.inc(0);}finally{RECORDER.R.flushNeeded();}}}"]
        ] as String[][],
                config)

        config = getInstrConfig(newDbTempFile().getAbsolutePath(), false, true, false)
        config.setFlushPolicy(InstrumentationConfig.THREADED_FLUSHING)
        config.setInstrLevel(InstrumentationLevel.METHOD.ordinal())

        checkInstrumentation([
                ["class B { public B(int arg) {int i = 0;}}",
                 "class B {$classField$snifferField public B(int arg) {try{RECORDER.R.inc(0);int i = 0;}finally{RECORDER.R.flushNeeded();}}}"]
        ] as String[][],
                config)
    }
}
