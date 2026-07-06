package org.openclover.idea;

import com.intellij.openapi.util.InvalidDataException;
import com.intellij.testFramework.HeavyPlatformTestCase;
import org.jdom.JDOMException;

import java.io.IOException;

public class WhenProjectWithInstalledProjectPluginIsClosedIdeaTest extends HeavyPlatformTestCase {
//    public void testIdeaTestDoesNotRunPostStartupActivities() {
//        assertFalse(ProjectPlugin.getPlugin(getProject()).isInitialized());
//    }

    public void testClosingWithPostStartupShouldNotNPE() throws JDOMException, InvalidDataException, IOException {
        // runPostStartupActivities() became a Kotlin coroutine in IDEA 2024 and can't be called directly from Java
        ((ProjectPlugin) ProjectPlugin.getPlugin(getProject())).projectClosed();
    }

    public void testClosingWithoutPostStartupShouldNotNPE() throws JDOMException, InvalidDataException, IOException {
        ((ProjectPlugin) ProjectPlugin.getPlugin(getProject())).projectClosed();
    }

}
