package com.atlassian.clover.idea;

import com.intellij.ide.startup.impl.StartupManagerImpl;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.testFramework.IdeaTestCase;
import org.jdom.JDOMException;

import java.io.IOException;

public class WhenProjectWithInstalledProjectPluginIsClosedIdeaTest extends IdeaTestCase {
//    public void testIdeaTestDoesNotRunPostStartupActivities() {
//        assertFalse(ProjectPlugin.getPlugin(getProject()).isInitialized());
//    }

    public void testClosingWithPostStartupShouldNotNPE() throws JDOMException, InvalidDataException, IOException {
        ((StartupManagerImpl) StartupManager.getInstance(getProject())).runPostStartupActivities();
        ((ProjectPlugin) ProjectPlugin.getPlugin(getProject())).projectClosed();
    }

    public void testClosingWithoutPostStartupShouldNotNPE() throws JDOMException, InvalidDataException, IOException {
        ((ProjectPlugin) ProjectPlugin.getPlugin(getProject())).projectClosed();
    }

}
