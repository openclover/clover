package org.openclover.core.instr.java

import org.junit.Test
import org.openclover.core.cfg.instr.InstrumentationConfig
import org.openclover.core.cfg.instr.java.JavaInstrumentationConfig
import org.openclover.core.cfg.instr.java.SourceLevel

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

    /**
     * OC-328: at Java 25+ the entry inc() and the invocation's inc() go before super()/this(), but the
     * explicit invocation must not be enclosed in the try block - which starts right after it.
     */
    @Test
    void testThreadedFlushingKeepsExplicitConstructorInvocationOutsideOfTryAtJava25() throws Exception {
        JavaInstrumentationConfig config = getInstrConfig(newDbTempFile().getAbsolutePath(), false, false, false)
        config.setFlushPolicy(InstrumentationConfig.THREADED_FLUSHING)
        config.setSourceLevel(SourceLevel.JAVA_25)
        checkInstrumentation([
                ["class B extends A { public B(int arg) {super(arg);}}",
                 "class B extends A {$classField$snifferField public B(int arg) {RECORDER.R.inc(0);RECORDER.R.inc(1);super(arg);try{}finally{RECORDER.R.flushNeeded();}}}"],
                ["class B { public B(int arg) {this(arg, 0);} public B(int a, int b) {}}",
                 "class B {$classField$snifferField public B(int arg) {RECORDER.R.inc(2);RECORDER.R.inc(3);this(arg, 0);try{}finally{RECORDER.R.flushNeeded();}} public B(int a, int b) {try{RECORDER.R.inc(4);}finally{RECORDER.R.flushNeeded();}}}"],
        ] as String[][],
                config)
    }

    /**
     * OC-328: as above, for a constructor with statements in the prologue (JEP 513) and in the body.
     */
    @Test
    void testIntervalFlushingKeepsExplicitConstructorInvocationOutsideOfTryAfterPrologueAtJava25() throws Exception {
        JavaInstrumentationConfig config = getInstrConfig(newDbTempFile().getAbsolutePath(), false, false, false)
        config.setFlushPolicy(InstrumentationConfig.INTERVAL_FLUSHING)
        config.setSourceLevel(SourceLevel.JAVA_25)
        checkInstrumentation([
                ["class B extends A { public B(int arg) {int i = 0; super(arg); i++;}}",
                 "class B extends A {$classField$snifferField public B(int arg) {RECORDER.R.inc(0);RECORDER.R.inc(1);int i = 0; RECORDER.R.inc(2);super(arg);try{ RECORDER.R.inc(3);i++;}finally{RECORDER.R.maybeFlush();}}}"],
        ] as String[][],
                config)
    }

    /**
     * Below Java 25 the try block still begins right after the '{' and is preceded by the explicit invocation.
     */
    @Test
    void testThreadedFlushingKeepsExplicitConstructorInvocationFirstBeforeJava25() throws Exception {
        JavaInstrumentationConfig config = getInstrConfig(newDbTempFile().getAbsolutePath(), false, false, false)
        config.setFlushPolicy(InstrumentationConfig.THREADED_FLUSHING)
        checkInstrumentation([
                ["class B extends A { public B(int arg) {super(arg);}}",
                 "class B extends A {$classField$snifferField public B(int arg) {super(arg);RECORDER.R.inc(1);try{RECORDER.R.inc(0);}finally{RECORDER.R.flushNeeded();}}}"],
        ] as String[][],
                config)
    }
}
