package com.atlassian.clover.instr.java;

import org.openclover.runtime.CloverNames;
import com.atlassian.clover.cfg.instr.InstrumentationConfig;

import static org.openclover.runtime.instr.Bindings.$CoverageRecorder$flushNeeded;
import static org.openclover.runtime.instr.Bindings.$CoverageRecorder$globalSliceEnd;
import static org.openclover.runtime.instr.Bindings.$CoverageRecorder$maybeFlush;

import java.lang.reflect.Modifier;

/**

 */
public class MethodExitInstrEmitter extends Emitter {

    private MethodEntryInstrEmitter entry;

    public MethodExitInstrEmitter(MethodEntryInstrEmitter entryEmitter, int endline, int endcol) {
        super(endline, endcol);
        this.entry = entryEmitter;
    }

    @Override
    public void init(final InstrumentationState state) {
        state.getSession().exitMethod(getLine(), getColumn());

        final StringBuilder instr = new StringBuilder();

        if (entry.needsFinally()) {
            instr.append("}finally{");

            if (state.isInstrEnabled()) {
                switch (state.getCfg().getFlushPolicy()) {
                    case InstrumentationConfig.INTERVAL_FLUSHING:
                        instr.append($CoverageRecorder$maybeFlush(state.getRecorderPrefix()));
                        instr.append(";");
                        break;
                    case InstrumentationConfig.THREADED_FLUSHING:
                        instr.append($CoverageRecorder$flushNeeded(state.getRecorderPrefix()));
                        instr.append(";");
                        break;
                }
                if (entry.isAddTestInstr()) {
                    String typeInstr = "getClass().getName()";
                    if (Modifier.isStatic(entry.getSignature().getBaseModifiersMask())) {
                       typeInstr = entry.getMethod().getContainingClass().getName() + ".class.getName()";
                    }
                    instr.append($CoverageRecorder$globalSliceEnd(state.getRecorderPrefix(), typeInstr,
                            "\"" + entry.getMethod().getQualifiedName() + "\"",
                            CloverNames.CLOVER_TEST_NAME_SNIFFER + ".getTestName()",
                            Integer.toString(entry.getMethod().getDataIndex())));
                    instr.append(";");
                }
            }
            instr.append("}");
        }
        setInstr(instr.toString());
    }
}
