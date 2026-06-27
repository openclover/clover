package org.openclover.eclipse.functest.runner;

import java.util.ArrayList;
import java.util.List;

public class TestResult {

    private final String projectName;
    private final List<String> failures = new ArrayList<>();
    private long durationMs;
    private String skipReason;

    public TestResult(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void skip(String reason) {
        this.skipReason = reason;
    }

    public boolean isSkipped() {
        return skipReason != null;
    }

    public String getSkipReason() {
        return skipReason;
    }

    public void fail(String message) {
        failures.add(message);
    }

    public boolean hasFailed() {
        return !failures.isEmpty();
    }

    public boolean hasBuildErrors() {
        return failures.stream().anyMatch(m -> m.startsWith("Build error"));
    }

    public List<String> getFailures() {
        return failures;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public void assertNotNull(Object value, String message) {
        if (value == null) {
            fail(message);
        }
    }

    public void assertFalse(boolean condition, String message) {
        if (condition) {
            fail(message);
        }
    }

    public void assertTrue(boolean condition, String message) {
        if (!condition) {
            fail(message);
        }
    }
}
