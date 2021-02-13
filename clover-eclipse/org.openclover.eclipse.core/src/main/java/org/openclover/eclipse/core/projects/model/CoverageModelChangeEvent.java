package com.atlassian.clover.eclipse.core.projects.model;

import com.atlassian.clover.eclipse.core.projects.CloverProject;

public class CoverageModelChangeEvent {
    private final String description;
    private final boolean userInitiated;
    private final long when;
    private DatabasePreLoadDecorator[] preChangeDecorators;
    private DatabasePostLoadDecorator[] postChangeDecorators;

    public static CoverageModelChangeEvent INIT(CloverProject project) {
        return new CoverageModelChangeEvent(project, "Initialisation", false);
    }

    public static CoverageModelChangeEvent IDLY(CloverProject project) {
        return new CoverageModelChangeEvent(project, "Idle load request", false);
    }

    public static CoverageModelChangeEvent COMPILE(CloverProject project) {
        return new CoverageModelChangeEvent(project, "Post-compilation", false);
    }

    public static CoverageModelChangeEvent RETRY(CloverProject project) {
        return new CoverageModelChangeEvent(project, "Loaded request after previous error", false);
    }

    public static CoverageModelChangeEvent CLOSE(CloverProject project) {
        return new CoverageModelChangeEvent(project, "Close request", false);
    }

    public CoverageModelChangeEvent(
        CloverProject project,
        String description,
        boolean userInitiated) {

        this(description,
            userInitiated,
            DatabasePreLoadDecorator.NONE,
            DatabasePostLoadDecorator.NONE);
    }

    public CoverageModelChangeEvent(String description, boolean userInitiated, DatabasePreLoadDecorator[] preChangeDecorators, DatabasePostLoadDecorator[] postChangeDecorators) {
        this.description = description;
        this.userInitiated = userInitiated;
        this.when = System.currentTimeMillis();
        this.postChangeDecorators = postChangeDecorators;
        this.preChangeDecorators = preChangeDecorators;
    }

    public String getDescription() {
        return description;
    }

    public boolean isUserInitiated() {
        return userInitiated;
    }

    public long getWhen() {
        return when;
    }

    public DatabasePreLoadDecorator[] getPreChangeDecorators() {
        return preChangeDecorators;
    }

    public DatabasePostLoadDecorator[] getPostChangeDecorators() {
        return postChangeDecorators;
    }
}
