package org.openclover.idea.autoupdater;

import com.intellij.testFramework.IdeaTestCase;

public class AutoUpdateComponentTest extends IdeaTestCase {
    public void testParseRemote() {
        new AutoUpdateComponent().run();
    }
}
