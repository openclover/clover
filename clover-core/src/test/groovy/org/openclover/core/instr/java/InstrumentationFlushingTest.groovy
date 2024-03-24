package org.openclover.core.instr.java

import org.junit.Test
import org.openclover.core.cfg.instr.InstrumentationConfig
import org.openclover.core.cfg.instr.java.JavaInstrumentationConfig

class InstrumentationFlushingTest extends InstrumentationTestBase {

    @Test
    void testThreadedFlushing() throws Exception {
        JavaInstrumentationConfig config = getInstrConfig(newDbTempFile().getAbsolutePath(), false, false, false)
        config.setFlushPolicy(InstrumentationConfig.THREADED_FLUSHING)
        checkInstrumentation([
                ["class B { public B(int arg) {int i = 0;}}",
                 "class B {$classField$snifferField public B(int arg) {try{RECORDER.R.inc(0);RECORDER.R.inc(1);int i = 0;}finally{RECORDER.R.flushNeeded();}}}"],
        ] as String[][],
                config)
    }

    @Test
    void testIntervalFlushing() throws Exception {
        JavaInstrumentationConfig config = getInstrConfig(newDbTempFile().getAbsolutePath(), false, false, false)
        config.setFlushPolicy(InstrumentationConfig.INTERVAL_FLUSHING)
        checkInstrumentation([
                ["class B { public B(int arg) {}}",
                 "class B {$classField$snifferField public B(int arg) {try{RECORDER.R.inc(0);}finally{RECORDER.R.maybeFlush();}}}"],
        ] as String[][],
                config)

    }
}
