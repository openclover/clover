package org.openclover.idea.autoupdater;

public class LatestVersionInfo {
    private final String number;
    private final String downloadUrl;
    private final String releaseNotes;
    private final String releaseNotesUrl;

    public LatestVersionInfo(String number, String downloadUrl, String releaseNotes, String releaseNotesUrl) {
        this.number = number;
        this.downloadUrl = downloadUrl;
        this.releaseNotes = releaseNotes;
        this.releaseNotesUrl = releaseNotesUrl;
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
