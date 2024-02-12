package org.openclover.idea.report.cloud;

import org.openclover.core.api.registry.HasMetrics;
import org.openclover.idea.coverage.BaseCoverageNodeViewer;

import javax.swing.Icon;

public interface CloudReportView {
    void setRisksHtml(String risks);

    void setWinsHtml(String wins);

    void setSummaryIcon(Icon icon);

    void setSummaryText(String summary);

    void clean();

    void setSummaryNode(HasMetrics element, BaseCoverageNodeViewer.TestPassInfo testPassInfo);
}
