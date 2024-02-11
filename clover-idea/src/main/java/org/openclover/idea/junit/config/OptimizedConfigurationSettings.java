package org.openclover.idea.junit.config;

import org.openclover.core.api.optimization.OptimizationOptions;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.openapi.util.WriteExternalException;
import org.jdom.Element;

public class OptimizedConfigurationSettings implements RunnerSettings {
    private static final OptimizationOptions.TestSortOrder REORDER_DEFAULT = OptimizationOptions.TestSortOrder.FAILFAST;
    private static final int COMPILES_BEFORE_STALE_SNAPSHOT_DEFAULT = 10;

    private boolean discardSnapshots;
    private int compilesBeforeStaleSnapshot = COMPILES_BEFORE_STALE_SNAPSHOT_DEFAULT;
    private boolean minimize = true;
    private OptimizationOptions.TestSortOrder reorder = REORDER_DEFAULT;

    public int getCompilesBeforeStaleSnapshot() {
        return compilesBeforeStaleSnapshot;
    }

    public void setCompilesBeforeStaleSnapshot(int compilesBeforeStaleSnapshot) {
        this.compilesBeforeStaleSnapshot = compilesBeforeStaleSnapshot;
    }

    public boolean isMinimize() {
        return minimize;
    }

    public void setMinimize(boolean minimize) {
        this.minimize = minimize;
    }

    public OptimizationOptions.TestSortOrder getReorder() {
        return reorder;
    }

    public void setReorder(OptimizationOptions.TestSortOrder reorder) {
        this.reorder = reorder;
    }

    public boolean isDiscardSnapshots() {
        return discardSnapshots;
    }

    public void setDiscardSnapshots(boolean discardSnapshots) {
        this.discardSnapshots = discardSnapshots;
    }

    public void readExternal(Element element) throws InvalidDataException {
        discardSnapshots = "true".equals(JDOMExternalizerUtil.readField(element, "discardSnapshots"));
        minimize = "true".equals(JDOMExternalizerUtil.readField(element, "minimize"));

        final String compilesBeforeStr = JDOMExternalizerUtil.readField(element, "compilesBeforeStaleSnapshot");
        compilesBeforeStaleSnapshot = compilesBeforeStr != null ? Integer.parseInt(compilesBeforeStr) : COMPILES_BEFORE_STALE_SNAPSHOT_DEFAULT;

        final String reorderStr = JDOMExternalizerUtil.readField(element, "reorder");
        reorder = reorderStr != null ? OptimizationOptions.TestSortOrder.valueOf(reorderStr) : REORDER_DEFAULT;
    }

    public void writeExternal(Element element) throws WriteExternalException {
        JDOMExternalizerUtil.writeField(element, "discardSnapshots", discardSnapshots ? "true" : "false");
        JDOMExternalizerUtil.writeField(element, "compilesBeforeStaleSnapshot", String.valueOf(compilesBeforeStaleSnapshot));
        JDOMExternalizerUtil.writeField(element, "minimize", minimize ? "true" : "false");
        JDOMExternalizerUtil.writeField(element, "reorder", reorder.name());
    }
}
