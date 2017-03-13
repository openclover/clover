package com.atlassian.clover.instr.java;

import com.atlassian.clover.context.ContextSet;
import com.atlassian.clover.context.NamedContext;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import static clover.com.google.common.collect.Lists.newLinkedList;

/**
 * represents an instrumentation
 */
public abstract class Emitter {

    private ContextSet context = new ContextSet();
    private int line;
    private int column;
    private String instr = "";
    private boolean enabled = true;
    private List<Emitter> dependents;

    protected Emitter() {}   

    protected Emitter(int line, int column) {
        this(new ContextSet(), line, column);
    }

    protected Emitter(ContextSet context, int line, int column) {
        this.context = context;
        this.line = line;
        this.column = column;
    }

    public ContextSet getElementContext() {
        return context;
    }

    public void addContext(NamedContext ctx) {
        if (acceptsContextType(ctx)) {
            context = context.set(ctx.getIndex());
        }
    }

    protected boolean acceptsContextType(NamedContext context) {
        return true; // unless overridden, emitters accept all context types
    }


    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (dependents != null) {
            for (Emitter kid : dependents) {
                kid.setEnabled(enabled);
            }
        }
    }

    public final void addDependent(Emitter emitter) {
        if (dependents == null) {
            dependents = newLinkedList();
        }
        emitter.setEnabled(isEnabled());
        dependents.add(emitter);
    }

    public final void emit(Writer out) throws IOException {
        if (isEnabled()) {
            out.write(getInstr());
        }
    }

    public final void initialise(InstrumentationState state) {
        context = context.or(state.getInstrContext());
        init(state);
    }

    protected abstract void init(InstrumentationState state);

    public void setInstr(String instr) {
        this.instr = instr;
    }

    public String getInstr() {
        return instr;
    }
}
