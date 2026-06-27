package org.openclover.eclipse.core.projects.builder;

public class FileNameWithEncoding {
    private final String fileName;
    private final String encoding;

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
