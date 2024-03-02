package org.openclover.core.api.registry;

import org.jetbrains.annotations.Nullable;

public interface TestCaseInfo {

    String getClassName();

    double getDuration();

    String getFailFullMessage();

    String getFailMessage();

    String getFailType();

    String getKey();

    Integer getId();

    String getTestName();

    @Nullable
    StackTraceInfo getStackTrace();

    @Nullable
    ClassInfo getRuntimeType();

    @Nullable
    MethodInfo getSourceMethod();

    @Nullable
    String getQualifiedName();

    @Nullable
    String getRuntimeTypeName();

    @Nullable
    String getSourceMethodName();

    long getStartTime();

    long getEndTime();

    boolean isError();

    boolean isFailure();

    boolean isHasResult();

    boolean isResolved();

    boolean isSuccess();

    boolean resolve(ProjectInfo project);
}
