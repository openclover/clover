package org.openclover.idea.autoupdater;

import com.intellij.testFramework.HeavyPlatformTestCase;

public class AutoUpdateComponentTest extends HeavyPlatformTestCase {
    public void testParseRemote() {
        new AutoUpdateComponent().run();
    }
}
