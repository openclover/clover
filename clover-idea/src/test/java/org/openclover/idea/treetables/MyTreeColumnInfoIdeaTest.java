package org.openclover.idea.treetables;

import com.intellij.testFramework.LightIdeaTestCase;

public class MyTreeColumnInfoIdeaTest extends LightIdeaTestCase {
    public void testGetColumnClass() throws Exception {
        MyTreeColumnInfo info = new MyTreeColumnInfo("Test") {};
        assertNotNull(info.getColumnClass());
    }
}
