package org.openclover.core.api.registry;

import org.openclover.core.registry.entities.FullFileInfo;

public interface StackTraceEntry {
    StackTraceInfo getParentTrace();

    int getId();

    String getLine();

    String getLinePrefix();

    String getLinkableLineSegment();

    StackTraceEntry getUp();

    StackTraceEntry getDown();

    void setDown(StackTraceEntry down);

    FullFileInfo getContainingFile();

    int getLineNum();

    boolean isResolved();

    boolean resolve(ProjectInfo proj);
}
