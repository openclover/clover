package org.openclover.core.reporters.html.source;

public class RenderMessages {
    public static final String OUT_OF_DATE =
        "The source file used to generate this report was changed after " +
        "OpenClover generated coverage information. The coverage reported " +
        "may not match the source lines. You should regenerate the " +
        "coverage information and the report to ensure the files " +
        "are in sync.";

    public static final String FAILED_RENDERING =
        "OpenClover encountered a problem rendering the source for this file.";

    public static final String FALLBACK_RENDERING =
        FAILED_RENDERING + " Syntax highlighting has been disabled.";
}
