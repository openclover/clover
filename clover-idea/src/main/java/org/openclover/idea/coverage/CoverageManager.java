package org.openclover.idea.coverage;

import org.jetbrains.annotations.Nullable;
import org.openclover.core.CloverDatabase;
import org.openclover.core.registry.Clover2Registry;
import org.openclover.core.util.Path;

import java.net.URL;

public interface CoverageManager {

    @Nullable
    CloverDatabase getCoverage();

    CoverageTreeModel getCoverageTree();

    boolean delete();

    boolean clean();

    void reload();

    void loadCoverageData(boolean forcePerTestData);

    boolean canLoadCoverageData();

    boolean canRefresh();

    void addCoverageListener(CoverageListener l);

    void removeCoverageListener(CoverageListener l);

    void addCoverageTreeListener(CoverageTreeListener l);

    void removeCoverageTreeListener(CoverageTreeListener l);

    void setSpan(long span);

    long getSpan();

    void setContextFilter(String filterSpec);

    String getContextFilter();

    void setInitString(URL url);

    URL getInitString();

    String getCoverageDatabasePath();

    void setSourcePath(Path path);

    float getCurrentCoverage();

    void lockRegistryForUpdate(Clover2Registry registry);

    void releaseUpdatedRegistry(Clover2Registry registry);

    void cleanup();
}
