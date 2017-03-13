package com.atlassian.clover.reporters.console;

import com.atlassian.clover.CodeType;
import com.atlassian.clover.reporters.Current;
import com.atlassian.clover.reporters.Format;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Set;

/**
 * Configuration for the {@link ConsoleReporter}
 */
public class ConsoleReporterConfig extends Current {

    /** What kind of code shall be included in the report */
    private CodeType codeType = CodeType.APPLICATION;

    /** Reporting detail */
    @NotNull
    private ReportingLevel reportingLevel = ReportingLevel.SUMMARY;

    /** Set of package names to be included in the report */
    @Nullable
    private Set<String> packageSet;

    /** Whether to include unit tests in the summary */
    private boolean showUnitTests;

    public CodeType getCodeType() {
        return codeType;
    }

    public void setCodeType(CodeType codeType) {
        this.codeType = codeType;
    }

    @NotNull
    public ReportingLevel getLevel() {
        return reportingLevel;
    }

    public void setLevel(@NotNull final ReportingLevel level) {
        this.reportingLevel = level;
    }

    public void setLevel(@NotNull final String level) throws IllegalArgumentException {
        this.reportingLevel = ReportingLevel.valueOf(level.toUpperCase(Locale.ENGLISH));
    }

    @Nullable
    public Set<String> getPackageSet() {
        return packageSet;
    }

    public void setPackageSet(@Nullable Set<String> packageSet) {
        this.packageSet = packageSet;
    }

    public boolean isShowUnitTests() {
        return showUnitTests;
    }

    public void setShowUnitTests(boolean showUnitTests) {
        this.showUnitTests = showUnitTests;
    }

    @Override
    public boolean validate() {
        if (getInitString() == null || getInitString().length() == 0) {
            setFailureReason(ERR_INITSTRING_NOT_SPECIFIED);
            return false;
        }

        // no format element specified
        if (getFormat() == null) {
           setFormat(Format.DEFAULT_TEXT);
        }

        return true;
    }

}