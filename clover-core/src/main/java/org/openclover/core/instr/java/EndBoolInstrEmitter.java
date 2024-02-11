package org.openclover.core.instr.java;

import static org.openclover.runtime.instr.Bindings.$CoverageRecorder$iget;

import org.openclover.core.registry.entities.FullBranchInfo;
import org.openclover.core.spi.lang.LanguageConstruct;
import org.openclover.core.context.ContextSet;
import org.openclover.core.context.NamedContext;
import org.openclover.core.registry.FixedSourceRegion;


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
