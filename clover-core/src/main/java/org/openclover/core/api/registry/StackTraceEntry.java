package org.openclover.core.api.registry;

public interface StackTraceEntry {
    StackTraceInfo getParentTrace();

    int getId();

    String getLine();

    String getLinePrefix();

    String getLinkableLineSegment();

    StackTraceEntry getUp();

    StackTraceEntry getDown();

    void setDown(StackTraceEntry down);

    FileInfo getContainingFile();

    int getLineNum();

    boolean isResolved();

    boolean resolve(ProjectInfo proj);
}
