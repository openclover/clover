package com.atlassian.clover.idea.report.cloud;

import com.atlassian.clover.idea.coverage.BaseCoverageNodeViewer;
import com.atlassian.clover.api.registry.HasMetrics;

import javax.swing.Icon;

public interface CloudReportView {
    void setRisksHtml(String risks);

    void setWinsHtml(String wins);

    void setSummaryIcon(Icon icon);

    void setSummaryText(String summary);

    void clean();

    void setSummaryNode(HasMetrics element, BaseCoverageNodeViewer.TestPassInfo testPassInfo);
}
