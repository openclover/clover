package org.openclover.core.api.registry;

import org.jetbrains.annotations.Nullable;
import org.openclover.core.registry.entities.FullClassInfo;
import org.openclover.core.registry.entities.FullMethodInfo;
import org.openclover.core.registry.entities.FullProjectInfo;
import org.openclover.core.registry.entities.StackTraceInfo;

public interface TestCaseInfo {

    Integer getId();

    boolean isResolved();

    boolean resolve(ProjectInfo project);

    String getKey();

    String getClassName();

    String getTestName();

    double getDuration();

    boolean isError();

    boolean isHasResult();

    boolean isFailure();

    boolean isSuccess();

    String getFailMessage();

    String getFailType();

    String getFailFullMessage();

    @Nullable
    StackTraceInfo getStackTrace();

    @Nullable
    FullClassInfo getRuntimeType();

    @Nullable
    FullMethodInfo getSourceMethod();

    @Nullable
    String getQualifiedName();

    @Nullable
    String getRuntimeTypeName();

    @Nullable
    String getSourceMethodName();

    long getStartTime();

    long getEndTime();
}
