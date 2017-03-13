package com.atlassian.clover.instr.java;

import static com.atlassian.clover.instr.Bindings.$CoverageRecorder$iget;

import com.atlassian.clover.registry.entities.FullBranchInfo;
import com.atlassian.clover.spi.lang.LanguageConstruct;
import com.atlassian.clover.context.ContextSet;
import com.atlassian.clover.context.NamedContext;
import com.atlassian.clover.registry.FixedSourceRegion;


public class EndBoolInstrEmitter extends Emitter {

    private ExpressionInfo expr;
    private int endline;
    private int endcol;
    private FullBranchInfo branchInfo;

    public EndBoolInstrEmitter(ContextSet context, int line, int column, int endline, int endcol,
                               ExpressionInfo expr) {
        super(context, line, column);
        this.endline = endline;
        this.endcol = endcol;
        this.expr = expr;
        setInstr("");
    }

      @Override
      public void init(InstrumentationState state) {
        branchInfo =
            state.getSession().addBranch(
                getElementContext(),
                new FixedSourceRegion(getLine(), getColumn(), endline, endcol),
                expr.isInstrumentable(), expr.getComplexity(),
                LanguageConstruct.Builtin.BRANCH);
        if (state.isInstrEnabled()) {
            if (branchInfo != null && // HACK - see CCD-317. ternary operators can occur outside methods
                    expr.isInstrumentable()) {
                int index = branchInfo.getDataIndex();
                state.setDirty();

                setInstr(")&&(" + $CoverageRecorder$iget(state.getRecorderPrefix(), Integer.toString(index)) + "!=0|true))||("
                        + $CoverageRecorder$iget(state.getRecorderPrefix(), Integer.toString(index + 1)) + "==0&false))");
            }
        }
    }

    @Override
    public void addContext(NamedContext ctx) {
        super.addContext(ctx);
        if (branchInfo != null) {
            branchInfo.addContext(ctx);
        }
    }

}
