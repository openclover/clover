package org.openclover.core.reporters.console;

/**
 * Describes how detailed the console report should be
 */
public enum ReportingLevel {
    SUMMARY(false, false, false, false),
    PACKAGE(true, false, false, false),
    CLASS(true, true, false, false),
    METHOD(true, true, true, false),
    STATEMENT(true, true, true, true);

    public boolean isShowPackages() {
        return showPackages;
    }

    public boolean isShowClasses() {
        return showClasses;
    }

    public boolean isShowMethods() {
        return showMethods;
    }

    public boolean isShowStatements() {
        return showStatements;
    }

    private final boolean showPackages;
    private final boolean showClasses;
    private final boolean showMethods;
    private final boolean showStatements;

    ReportingLevel(boolean showPackages, boolean showClasses, boolean showMethods, boolean showStatements) {
        this.showPackages = showPackages;
        this.showClasses = showClasses;
        this.showMethods = showMethods;
        this.showStatements = showStatements;
    }
}
