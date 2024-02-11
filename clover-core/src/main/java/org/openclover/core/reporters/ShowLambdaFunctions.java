package org.openclover.core.reporters;

/**
 * Helper enumeration to make easier choice which lambda functions shall be shown in a report.
 * It's related with {@link Current#showLambdaFunctions} and {@link Current#showInnerFunctions}.
 */
public enum ShowLambdaFunctions {
    NONE("None"),
    FIELDS_ONLY("Declared in fields only"),
    FIELDS_AND_INLINE("Declared in fields and in-line");

    private final String description;

    ShowLambdaFunctions(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
