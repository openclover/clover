package com.atlassian.clover.idea.testexplorer;

public class SourceFolderDescription {
    private final String name;
    private final boolean testFolder;

    public SourceFolderDescription(String name, boolean testFolder) {
        this.name = name;
        this.testFolder = testFolder;
    }


    public String getName() {
        return name;
    }

    public boolean isTestFolder() {
        return testFolder;
    }
}
