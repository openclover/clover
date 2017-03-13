package com.atlassian.clover.reporters.html;

public class TabInfo {
    private final String title;
    private final String filename;
    private final String help;

    public TabInfo(String title, String filename, String help) {
        this.title = title;
        this.filename = filename;
        this.help = help;
    }

    public String getTitle() {
        return title;
    }

    public String getFilename() {
        return filename;
    }

    public String getHelp() {
        return help;
    }
}
