package com.atlassian.clover.context;


public interface NamedContext {
    public enum Type {
        /** Context is related with a source code block type (like 'if', 'while'). It's a built-in type. */
        BLOCK,
        /** Context is related with a regular expression (like matching method signature) */
        REGEXP
    }

    public String getName();
    public int getIndex();
    public Type getType();

}
