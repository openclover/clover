package com.atlassian.clover.instr.java

import com.atlassian.clover.cfg.instr.InstrumentationConfig
import com.atlassian.clover.cfg.instr.java.JavaInstrumentationConfig
import org.junit.Test

class InstrumentationFlushingTest extends InstrumentationTestBase {

    @Test
    void testThreadedFlushing() throws Exception {
        JavaInstrumentationConfig config = getInstrConfig(newDbTempFile().getAbsolutePath(), false, false, false)
        config.setFlushPolicy(InstrumentationConfig.THREADED_FLUSHING)
        checkInstrumentation([
                ["class B { public B(int arg) {int i = 0;}}",
                 "class B {" + snifferField + " public B(int arg) {try{RECORDER.inc(0);RECORDER.inc(1);int i = 0;}finally{RECORDER.flushNeeded();}}}"],
        ] as String[][],
                config)
    }

    @Test
    void testIntervalFlushing() throws Exception {
        JavaInstrumentationConfig config = getInstrConfig(newDbTempFile().getAbsolutePath(), false, false, false)
        config.setFlushPolicy(InstrumentationConfig.INTERVAL_FLUSHING)
        checkInstrumentation([
                ["class B { public B(int arg) {}}",
                 "class B {" + snifferField + " public B(int arg) {try{RECORDER.inc(0);}finally{RECORDER.maybeFlush();}}}"],
        ] as String[][],
                config)

    }
}
