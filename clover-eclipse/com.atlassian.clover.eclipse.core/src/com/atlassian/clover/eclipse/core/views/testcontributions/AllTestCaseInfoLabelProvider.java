package com.atlassian.clover.eclipse.core.views.testcontributions;

public class AllTestCaseInfoLabelProvider extends TestCaseInfoLabelProvider {

    @Override
    public String getColumnText(Object value, int column) {
        if (value == AllTestCaseInfoProvider.ALL_TEST_CASES) {
            return "All";
        } else {
            return super.getColumnText(value, column);
        }
    }
}
