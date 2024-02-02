package org.openclover.idea;

import javax.swing.Icon;
import java.io.File;

public abstract class IDEContext {
    public abstract String getContextFilterSpec();

    public abstract String getProjectName();

    public abstract File getProjectRootDirectory();

    public abstract File getCloverWorkspace();

    public abstract Icon[] getFileTypes();
}
