package com.atlassian.clover.idea;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.testFramework.LightIdeaTestCase;

public class IdeaVersionVerificationIdeaTest extends LightIdeaTestCase {
    public void testVersion() {
        final ApplicationInfo info = ApplicationInfo.getInstance();
        final String currentVersion = info.getMajorVersion() + "." + info.getMinorVersion();
        final String expectedVersion = System.getProperty("cij.idea.expected.version", "14.1.7");
        assertEquals("Verifying build environment", expectedVersion, currentVersion);
    }
}
