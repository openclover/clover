package com.atlassian.clover.eclipse.core.views.testrunexplorer.nodes;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.swt.graphics.Image;
import com.atlassian.clover.registry.entities.TestCaseInfo;
import com.atlassian.clover.eclipse.core.views.nodes.Nodes;
import com.atlassian.clover.eclipse.core.views.nodes.JavaElementNode;

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
        this.tciId = tci.getId().intValue();
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
        if (failureMessage != null ? !failureMessage.equals(that.failureMessage) : that.failureMessage != null)
            return false;
        if (fullFailureMessage != null ? !fullFailureMessage.equals(that.fullFailureMessage) : that.fullFailureMessage != null)
            return false;
        if (testCaseIcon != null ? !testCaseIcon.equals(that.testCaseIcon) : that.testCaseIcon != null)
            return false;
        if (testMethod != null ? !testMethod.equals(that.testMethod) : that.testMethod != null)
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
