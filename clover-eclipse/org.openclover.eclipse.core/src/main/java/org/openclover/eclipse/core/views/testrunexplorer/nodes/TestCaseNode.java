package org.openclover.eclipse.core.views.testrunexplorer.nodes;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.swt.graphics.Image;
import org.openclover.core.registry.entities.TestCaseInfo;
import org.openclover.eclipse.core.views.nodes.Nodes;
import org.openclover.eclipse.core.views.nodes.JavaElementNode;

import java.util.Objects;

public class TestCaseNode extends JavaElementNode {
    public static final int STATUS_PASS = -1;
    public static final int STATUS_FAIL = 0;
    public static final int STATUS_ERROR = 1;

    private IMethod testMethod;
    private long tciId;
    private long startTime;
    private int status;
    private double durationInSeconds;
    private String failureMessage;
    private String fullFailureMessage;
    private Image testCaseIcon;

    public TestCaseNode(IMethod testMethod, TestCaseInfo tci) {
        this.testMethod = testMethod;
        this.tciId = tci.getId();
        this.startTime = tci.getStartTime();
        this.status =
            tci.isSuccess()
                ? STATUS_PASS
                : tci.isFailure()
                    ? STATUS_FAIL
                    : STATUS_ERROR;
        this.durationInSeconds = tci.getDuration();
        this.failureMessage = tci.getFailMessage();
        this.fullFailureMessage = tci.getFailFullMessage();
        this.testCaseIcon = Nodes.iconFor(tci);
    }

    public IMethod getTestMethod() {
        return testMethod;
    }

    public long getStartTime() {
        return startTime;
    }

    public int getStatus() {
        return status;
    }

    public double getDurationInSeconds() {
        return durationInSeconds;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public String getFullFailureMessage() {
        return fullFailureMessage;
    }

    public Image getTestCaseIcon() {
        return testCaseIcon;
    }

    @Override
    public IJavaElement toJavaElement() {
        return testMethod;
    }

    public long getTciId() {
        return tciId;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestCaseNode that = (TestCaseNode)o;

        if (Double.compare(that.durationInSeconds, durationInSeconds) != 0)
            return false;
        if (startTime != that.startTime) return false;
        if (status != that.status) return false;
        if (tciId != that.tciId) return false;
        if (!Objects.equals(failureMessage, that.failureMessage))
            return false;
        if (!Objects.equals(fullFailureMessage, that.fullFailureMessage))
            return false;
        if (!Objects.equals(testCaseIcon, that.testCaseIcon))
            return false;
        if (!Objects.equals(testMethod, that.testMethod))
            return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (testMethod != null ? testMethod.hashCode() : 0);
        result = 31 * result + (int)(tciId ^ (tciId >>> 32));
        result = 31 * result + (int)(startTime ^ (startTime >>> 32));
        result = 31 * result + status;
        result = 31 * result + (durationInSeconds != +0.0d ? (int)Double.doubleToLongBits(durationInSeconds) : 0);
        result = 31 * result + (failureMessage != null ? failureMessage.hashCode() : 0);
        result = 31 * result + (fullFailureMessage != null ? fullFailureMessage.hashCode() : 0);
        result = 31 * result + (testCaseIcon != null ? testCaseIcon.hashCode() : 0);
        return result;
    }
}
