package org.openclover.core.instr.java;

import org.openclover.core.context.NamedContext;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import static org.openclover.core.util.Lists.newLinkedList;


public class CloverToken
    extends clover.antlr.CommonHiddenStreamToken
{
    private CloverToken next;
    private CloverToken prev;
    private List<Emitter> preEmitters;
    private List<Emitter> postEmitters;
    private CloverTokenStreamFilter filter;
    private boolean emittersEnabled;

    public CloverToken() {
        super();
    }

    public CloverToken(int t, String txt) {
        super(t, txt);
    }

    public CloverToken(String s) {
        super(s);
    }

    public CloverToken getNext() {
        return next;
    }

    public void setNext(CloverToken next) {
        this.next = next;
    }

    public CloverToken getPrev() {
        return prev;
    }

    public void setPrev(CloverToken prev) {
        this.prev = prev;
    }

    public boolean isEmittersEnabled() {
        return emittersEnabled;
    }

    public void setEmittersEnabled(boolean emittersEnabled) {
        this.emittersEnabled = emittersEnabled;
        if (hasPreEmitters()) {
            for (Emitter emitter : preEmitters) {
                emitter.setEnabled(emittersEnabled);
            }
        }
        if (hasPostEmitters()) {
            for (Emitter emitter : postEmitters) {
                emitter.setEnabled(emittersEnabled);
            }
        }
    }

    public void addContext(NamedContext context) {
        if (hasPreEmitters()) {
            for (Emitter emitter : preEmitters) {
                emitter.addContext(context);
            }
        }
        if (hasPostEmitters()) {
            for (Emitter emitter : postEmitters) {
                emitter.addContext(context);
            }
        }
    }

    public void addPreEmitter(Emitter emitter) {
        if (preEmitters == null) {
            preEmitters = newLinkedList();
        }
        preEmitters.add(emitter);
    }

    public boolean hasPreEmitters() {
        return preEmitters != null;
    }

    public void triggerPreEmitters(Writer out) throws IOException {
        if (hasPreEmitters()) {
            for (Emitter emitter : preEmitters) {
                emitter.emit(out);
            }
        }
    }

    public boolean hasPostEmitters() {
        return postEmitters != null;
    }

    public void triggerPostEmitters(Writer out) throws IOException {
        if (hasPostEmitters()) {
            for (Emitter emitter : postEmitters) {
                emitter.emit(out);
            }
        }
    }

    public void addPostEmitter(Emitter emitter) {
        if (postEmitters == null) {
            postEmitters = newLinkedList();
        }
        postEmitters.add(emitter);
    }

    public void setFilter(CloverTokenStreamFilter filter) {
        this.filter = filter;
    }

    public CloverTokenStreamFilter getFilter() {
        return filter;
    }

    public boolean hasEmitters() {
        return hasPreEmitters() || hasPostEmitters();
    }

    public void initEmitters(InstrumentationState state) {
        if (hasPreEmitters()) {
            for (Emitter emitter : preEmitters) {
                emitter.initialise(state);
            }
        }
        if (hasPostEmitters()) {
            for (Emitter emitter : postEmitters) {
                emitter.initialise(state);
            }
        }
    }


}
