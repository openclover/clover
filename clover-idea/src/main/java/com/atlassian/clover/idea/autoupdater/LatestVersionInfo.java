package com.atlassian.clover.idea.autoupdater;

public class LatestVersionInfo {
    private final boolean stable;
    private final String number;
    private final String downloadUrl;
    private final String releaseNotes;
    private final String releaseNotesUrl;

    public LatestVersionInfo(boolean stable, String number, String downloadUrl, String releaseNotes, String releaseNotesUrl) {
        this.stable = stable;
        this.number = number;
        this.downloadUrl = downloadUrl;
        this.releaseNotes = releaseNotes;
        this.releaseNotesUrl = releaseNotesUrl;
    }

    public boolean isStable() {
        return stable;
    }

    public String getNumber() {
        return number;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getReleaseNotes() {
        return releaseNotes;
    }

    public String getReleaseNotesUrl() {
        return releaseNotesUrl;
    }
}
