package org.openclover.core.optimization;

import org.openclover.core.api.registry.ProjectInfo;
import org.openclover.core.api.registry.TestCaseInfo;
import org.openclover.core.registry.entities.FullProjectInfo;

import java.io.Serializable;
import java.util.Objects;

/** Package-internal representation of a recorded call to a test method */
class TestMethodCall implements Serializable {
    private static final long serialVersionUID = 9075409289508758000L;

    private final String runtimeTypeName;
    private final String sourceMethodName;
    private final String runtimeMethodName;
    private final String packagePath;

    private TestMethodCall(String runtimeTypeName, String sourceMethodName, String packagePath) {
        this.runtimeTypeName = runtimeTypeName;
        this.sourceMethodName = sourceMethodName;
        this.runtimeMethodName = runtimeTypeName + "." + getSimpleMethodName(sourceMethodName);
        this.packagePath = packagePath;
    }

    public static TestMethodCall createFor(ProjectInfo project, TestCaseInfo tci) {
        String packagePathName = packagePathNameFor(tci, project);
        if (packagePathName != null
            && tci.getRuntimeTypeName() != null
            && tci.getSourceMethodName() != null) {
            return
                new TestMethodCall(
                    tci.getRuntimeTypeName(),
                    tci.getSourceMethodName(),
                    packagePathName);
        } else {
            return null;
        }
    }

    public String getPackagePath() {
        return packagePath;
    }

    public String getSourceMethodName() {
        return sourceMethodName;
    }

    public String getRuntimeMethodName() {
        return runtimeMethodName;
    }

    public boolean isInheritedCall() {
        return !runtimeMethodName.equals(sourceMethodName);
    }

    public final String getSimpleMethodName(String methodName) {
        return
            methodName == null
                ? null
                : methodName.indexOf('.') == -1
                    ? methodName
                    : methodName.substring(Math.min(methodName.length() - 1, methodName.lastIndexOf('.') + 1));
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestMethodCall that = (TestMethodCall)o;

        if (!Objects.equals(packagePath, that.packagePath))
            return false;
        if (!Objects.equals(runtimeTypeName, that.runtimeTypeName))
            return false;
        return Objects.equals(sourceMethodName, that.sourceMethodName);
    }

    public int hashCode() {
        int result;
        result = (runtimeTypeName != null ? runtimeTypeName.hashCode() : 0);
        result = 31 * result + (sourceMethodName != null ? sourceMethodName.hashCode() : 0);
        result = 31 * result + (packagePath != null ? packagePath.hashCode() : 0);
        return result;
    }

    public static String packagePathNameFor(TestCaseInfo tci, ProjectInfo project) {
        if (!tci.isResolved()) {
            tci.resolve(project);
        }

        if (tci.getRuntimeType() != null && tci.getRuntimeType().getContainingFile() != null) {
            return tci.getRuntimeType().getContainingFile().getPackagePath();
        } else {
            //TODO: Inner classes probably won't work for this simple replacement of . with /
            return tci.getRuntimeTypeName() == null ? null : tci.getRuntimeTypeName().replace('.', '/') + ".java";
        }
    }

    public static String getSourceMethodNameFor(TestCaseInfo tci, ProjectInfo project) {
        if (!tci.isResolved()) {
            tci.resolve(project);
        }
        return tci.getSourceMethodName();
    }

    public String toString() {
        final String sourceMethodName = getSourceMethodName();
        if (getRuntimeMethodName().equals(sourceMethodName)) {
            return getRuntimeMethodName();
        } else {
            return getRuntimeMethodName() + "/" + getSourceMethodName();
        }
    }
}
