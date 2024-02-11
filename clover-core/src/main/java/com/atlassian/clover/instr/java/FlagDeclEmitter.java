package com.atlassian.clover.instr.java;

import org_openclover_runtime.CloverVersionInfo;


public class FlagDeclEmitter extends Emitter {

    private String flag;
    private boolean initval;

    public FlagDeclEmitter() {
        this(false);
    }

    public FlagDeclEmitter(boolean initval) {
        super();
        this.initval = initval;
    }

    @Override
    public void init(InstrumentationState state) {
        flag = "__CLB" + CloverVersionInfo.SANITIZED_RN +"_bool" + state.getIncBoolIndex();
        if (state.isInstrEnabled()) {
            setInstr("boolean " + flag + "="+initval+";");
        }
        else {
            // tell my dependents
            setEnabled(false);
        }
    }

    public String getFlagName() {
        return flag;
    }
}
