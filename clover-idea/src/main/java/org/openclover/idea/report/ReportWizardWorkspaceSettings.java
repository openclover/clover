package org.openclover.idea.report;

import org.openclover.core.reporters.ShowLambdaFunctions;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;

@State(name = "CloverReportSettings", storages = {@Storage(id = "other", file = "$APP_CONFIG$/other.xml")})
public class ReportWizardWorkspaceSettings implements PersistentStateComponent<ReportWizardWorkspaceSettings> {
    private Boolean includeSources = true;
    private Boolean includeFailedCoverage = false;
    private ShowLambdaFunctions showLambda = ShowLambdaFunctions.NONE;

    public boolean isIncludeSources() {
        return includeSources != null && includeSources;
    }

    public void setIncludeSources(boolean include) {
        includeSources = include;
    }

    public boolean isIncludeFailedCoverage() {
        return includeFailedCoverage != null && includeFailedCoverage;
    }

    public void setIncludeFailedCoverage(boolean include) {
        includeFailedCoverage = include;
    }

    @Override
    public void loadState(ReportWizardWorkspaceSettings persisted) {
        includeSources = persisted.includeSources;
        includeFailedCoverage = persisted.includeFailedCoverage;
    }

    @Override
    public ReportWizardWorkspaceSettings getState() {
        return this;
    }

    public static ReportWizardWorkspaceSettings getInstance() {
        return ServiceManager.getService(ReportWizardWorkspaceSettings.class);
    }

    public ShowLambdaFunctions getShowLambda() {
        return showLambda;
    }

    public void setShowLambda(ShowLambdaFunctions showLambda) {
        this.showLambda = showLambda;
    }
}
