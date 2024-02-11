package org.openclover.idea.util;

import org.openclover.idea.PluginVersionInfo;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.diagnostic.ErrorReportSubmitter;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import org_openclover_runtime.CloverVersionInfo;

import java.awt.Component;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class BlameClover extends ErrorReportSubmitter {
    @Override
    public String getReportActionText() {
        return "Create error report in the OpenClover online issue tracker";
    }

    @Override
    public SubmittedReportInfo submit(IdeaLoggingEvent[] events, Component parentComponent) {
        StringBuilder description = new StringBuilder();
        for (IdeaLoggingEvent ideaLoggingEvent : events) {
            description.append(ideaLoggingEvent.getMessage());
            description.append("\n");
            description.append(ideaLoggingEvent.getThrowableText());
            description.append("\n");
        }
        BrowserUtil.browse(getBugWithDescriptionUrl(description.toString()));
        return new SubmittedReportInfo(null, "JIRA ticket", SubmittedReportInfo.SubmissionStatus.NEW_ISSUE);
    }

    private static final String PROJECT_ID = "11772";
    private static final String TICKET_TYPE_BUG = "1";
    private static final String CIJ_COMPONENT="12941";
    private static final int MAX_URI_LENGTH = 4096;

    private final String bugUrl;

    public BlameClover() {
        final String rawEnvironment =
                "Java version=" + System.getProperty("java.version")
                        + ", Java vendor=" + System.getProperty("java.vendor")
                        + ", OS name=" + System.getProperty("os.name")
                        + ", OS architecture=" + System.getProperty("os.arch")
                        + ", IDEA build number=" + getIdeaBuild()
                        + "\nPlugin release=" + PluginVersionInfo.RELEASE_NUMBER;
        String environment;
        try {
            environment = URLEncoder.encode(rawEnvironment, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            environment = "";
            Logger.getInstance(BlameClover.class.getName()).info(e);
        }

        bugUrl = CloverVersionInfo.CLOVER_CREATE_ISSUE_URL
                + "?pid=" + PROJECT_ID
                + "&issuetype=" + TICKET_TYPE_BUG
                + "&components=" + CIJ_COMPONENT
                + "&environment=" + environment;
    }

    protected String getIdeaBuild() {
        return ApplicationInfo.getInstance().getBuild().toString();
    }

    String getBugWithDescriptionUrl(String description) {
        final String urlStart = bugUrl + "&description=";
        final int charsLeft = MAX_URI_LENGTH - urlStart.length();

        return urlStart + getBoundedEncodedString(description, charsLeft);
    }

    String getBoundedEncodedString(String description, int maxLen) {
        try {
            String encoded = URLEncoder.encode(description, "UTF-8");
            while (encoded.length() > maxLen) {
                int lastNewline = description.lastIndexOf('\n');
                if (lastNewline == -1) {
                    return "";
                }
                description = description.substring(0, lastNewline);
                encoded = URLEncoder.encode(description, "UTF-8");
            }

            return encoded;
        } catch (UnsupportedEncodingException e) {
            return "";
        }

    }
}
