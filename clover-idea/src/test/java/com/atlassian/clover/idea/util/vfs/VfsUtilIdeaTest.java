package org.openclover.idea.util.vfs;

import com.intellij.testFramework.LightIdeaTestCase;

public class VfsUtilIdeaTest extends LightIdeaTestCase {
    public void testCalcRelativeToProjectPath() {

        try {
            System.out.println("java.io.tmpdir = " + System.getProperty("java.io.tmpdir"));
            VfsUtil.calcRelativeToProjectPath(getSourceRoot(), getProject());
        } catch (Throwable e1) {
            e1.printStackTrace();
            System.out.println("ERROR = " + e1.getMessage());
        }

    }
}
