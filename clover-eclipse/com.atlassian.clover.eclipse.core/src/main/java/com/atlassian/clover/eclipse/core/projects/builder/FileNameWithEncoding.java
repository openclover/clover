package com.atlassian.clover.eclipse.core.projects.builder;

public class FileNameWithEncoding {
    private String fileName;
    private String encoding;

    public FileNameWithEncoding(String fileName, String encoding) {
        this.fileName = fileName;
        this.encoding = encoding;
    }

    public String getFileName() {
        return fileName;
    }

    public String getEncoding() {
        return encoding;
    }
}
