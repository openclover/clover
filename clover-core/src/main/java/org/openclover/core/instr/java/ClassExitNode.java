package org.openclover.core.instr.java;

/**

 */
public class ClassExitNode extends Emitter {


    private ClassEntryNode entry;

    public ClassExitNode(ClassEntryNode entry, String className, int endline, int endcol) {
        super(endline, endcol);
        this.entry = entry;
    }

    @Override
    public void init(InstrumentationState state) {
        state.getSession().exitClass(getLine(), getColumn());
        state.setDetectTests(entry.isOuterDetectTests());

        CloverToken insertPoint = entry.getRecorderInsertPoint();
        if (insertPoint != null) {
            insertPoint.setEmittersEnabled(state.isDirty());
            entry.getRecorderInstrEmitter().setMaxDataIndex(
                state.getSession().getCurrentFileMaxIndex());
            state.setDirty(false);
        }

    }

    public ClassEntryNode getEntry() {
        return entry;
    }
}
