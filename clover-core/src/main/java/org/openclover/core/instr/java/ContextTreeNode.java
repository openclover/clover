package org.openclover.core.instr.java;

import org.openclover.core.context.ContextSet;

/** Used to track and minimise context sets created  during instrumentation */
public class ContextTreeNode {
    private final ContextTreeNode parent;
    private final ContextSet context;
    private ContextTreeNode[] children;

    ContextTreeNode(ContextTreeNode parent, ContextSet context) {
        this.parent = parent;
        this.children = new ContextTreeNode[parent.children.length];
        this.context = context;
    }

    public ContextTreeNode(int childCount, ContextSet context) {
        this.parent = null;
        this.children = new ContextTreeNode[childCount];
        this.context = context;
    }

    public ContextTreeNode enterContext(int index) {
        if (index >= children.length) {
            ContextTreeNode[] children = new ContextTreeNode[this.children.length * 2];
            System.arraycopy(this.children, 0, children, 0, this.children.length);
        }
        ContextTreeNode child = children[index];
        if (child == null) {
            ContextSet context = new ContextSet(this.context);
            context = context.set(index);
            child = new ContextTreeNode(this, context);
            children[index] = child;
        }
        return child;
    }

    public ContextTreeNode exitContext() {
        return parent;
    }

    public ContextSet getContext() {
        return context;           
    }

    public int countSelfAndDescendants() {
        int count = 1;
        for (ContextTreeNode child : children) {
            if (child != null) {
                count += child.countSelfAndDescendants();
            }
        }
        return count;
    }
}
