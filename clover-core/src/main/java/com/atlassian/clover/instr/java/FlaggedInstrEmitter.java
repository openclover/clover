package com.atlassian.clover.instr.java;

import com.atlassian.clover.context.NamedContext;

/**

 */
public class FlaggedInstrEmitter extends Emitter {

    private FlagDeclEmitter flag;
    private Emitter child;
    private String preInstr = "";
    private String postInstr = "";


    public FlaggedInstrEmitter(FlagDeclEmitter flag, Emitter child) {
        super();
        flag.addDependent(this); // I am dependent on the flag decl
        addDependent(child); // the enclosed emitter is dependent on me
        this.flag = flag;
        this.child = child;
    }

    @Override
    public void addContext(NamedContext ctx) {
        super.addContext(ctx);
        child.addContext(ctx);
    }


    @Override
    public void init(InstrumentationState state) {
       child.initialise(state);
       if (state.isInstrEnabled()) {
           preInstr = "if (!"+flag.getFlagName()+") {";
           postInstr = flag.getFlagName() + "=true;}";
       }
    }

    @Override
    public String getInstr() {
       return preInstr + child.getInstr() + postInstr;
    }

}
