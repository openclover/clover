package com.atlassian.clover.eclipse.core.views.testrunexplorer;

import com.atlassian.clover.eclipse.core.views.ExplorerViewComparator;
import com.atlassian.clover.eclipse.core.views.ColumnDefinition;
import com.atlassian.clover.eclipse.core.views.testrunexplorer.nodes.TestCaseNode;
import org.eclipse.jface.viewers.Viewer;

public abstract class TestRunExplorerTreeComparator extends ExplorerViewComparator {
    protected static final Long ZERO_LONG = 0L;
    protected static final Double ZERO_DOUBLE = 0.0;
    protected static final Integer MAX_INTEGER = Integer.MAX_VALUE;

    public static TestRunExplorerTreeComparator createFor(final TestRunExplorerViewSettings settings) {
        final ColumnDefinition column = settings.getTreeColumnSettings().getSortedColumn();
        return new TestRunExplorerTreeComparator() {
            @Override
            public int compare(Viewer viewer, Object value1, Object value2) {
                return invert(
                    settings.getTreeColumnSettings().isReverseSort(),
                    column.getComparator(settings, settings.getMetricsScope()).compare(value1, value2));
            }
        };
    }

    public static int compareTestCaseName(Object value1, Object value2, boolean flatten) {
        TestCaseNode testCase1 = toTestCaseNode(value1);
        TestCaseNode testCase2 = toTestCaseNode(value2);
        if (testCase1 != null && testCase2 != null) {
            if (flatten) {
                String name1 = testCase1.getTestMethod().getParent().getElementName() + "." + testCase1.getTestMethod().getElementName();
                String name2 = testCase2.getTestMethod().getParent().getElementName() + "." + testCase2.getTestMethod().getElementName();
                return compareName(name1, name2);
            } else {
                return compareName(testCase1.getTestMethod(), testCase2.getTestMethod());
            }
        } else {
            return compareName(value1, value2);
        }
    }

    public static int compareStarted(Object value1, Object value2) {
        TestCaseNode testCase1 = toTestCaseNode(value1);
        TestCaseNode testCase2 = toTestCaseNode(value2);
        Long started1 = testCase1 == null ? ZERO_LONG : Long.valueOf(testCase1.getStartTime());
        Long started2 = testCase2 == null ? ZERO_LONG : Long.valueOf(testCase2.getStartTime());
        return started1.compareTo(started2);
    }

    public static int compareStatus(Object value1, Object value2) {
        TestCaseNode testCase1 = toTestCaseNode(value1);
        TestCaseNode testCase2 = toTestCaseNode(value2);
        Integer status1 =
            testCase1 == null
                ? MAX_INTEGER
                : Integer.valueOf(testCase1.getStatus());

        Integer status2 =
            testCase2 == null
                ? MAX_INTEGER
                : Integer.valueOf(testCase2.getStatus());

        return status1.compareTo(status2);
    }

    public static int compareTime(Object value1, Object value2) {
        TestCaseNode testCase1 = toTestCaseNode(value1);
        TestCaseNode testCase2 = toTestCaseNode(value2);
        Double time1 = testCase1 == null ? ZERO_DOUBLE : Double.valueOf(testCase1.getDurationInSeconds());
        Double time2 = testCase2 == null ? ZERO_DOUBLE : Double.valueOf(testCase2.getDurationInSeconds());
        return time1.compareTo(time2);
    }

    public static int compareMessage(Object value1, Object value2) {
        TestCaseNode testCase1 = toTestCaseNode(value1);
        TestCaseNode testCase2 = toTestCaseNode(value2);
        String message1 = testCase1 == null ? "" : (testCase1.getFullFailureMessage() == null ? "" : testCase1.getFullFailureMessage());
        String message2 = testCase2 == null ? "" : (testCase2.getFullFailureMessage() == null ? "" : testCase2.getFullFailureMessage());
        return message1.compareTo(message2);
    }

    public static TestCaseNode toTestCaseNode(Object value) {
        return value instanceof TestCaseNode ? (TestCaseNode)value : null;
    }
}
