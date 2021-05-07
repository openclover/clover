package org.openclover.eclipse.core.projects.model;

import com.atlassian.clover.CloverDatabase;
import com.atlassian.clover.registry.entities.FullProjectInfo;
import org.openclover.eclipse.core.projects.CloverProject;
import com.atlassian.clover.registry.entities.BaseClassInfo;
import com.atlassian.clover.registry.entities.BaseFileInfo;
import com.atlassian.clover.api.registry.HasMetrics;
import com.atlassian.clover.registry.entities.FullMethodInfo;
import com.atlassian.clover.registry.entities.TestCaseInfo;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;

public class DuringFullBuildDatabaseModel extends StableDatabaseModel {
    public DuringFullBuildDatabaseModel(CloverProject project, CoverageModelChangeEvent changeEvent) {
        super(project, changeEvent);
    }

    @Override
    public boolean isLoaded() { return true; }
    @Override
    public boolean isLoading() { return false; }
    @Override
    public CloverDatabase getDatabase() { return null; }

    @Override
    public boolean isCoverageOutOfDate() { return false; }
    @Override
    public boolean isRegistryOfDate() { return false; }

    @Override
    public FullProjectInfo getFullProjectInfo() { return null; }
    @Override
    public FullProjectInfo getTestOnlyProjectInfo() { return null; }
    @Override
    public FullProjectInfo getAppOnlyProjectInfo() { return null; }
    @Override
    public HasMetrics getPackageInfoOrFragment(IPackageFragment pack, MetricsScope scope) { return null; }
    @Override
    public BaseFileInfo getSourceFileInfo(ICompilationUnit cu, MetricsScope scope) { return null; }
    @Override
    public BaseClassInfo getTypeInfo(IType type, MetricsScope scope) { return null; }
    @Override
    public FullMethodInfo getMethodInfo(IMethod method, MetricsScope scope) { return null; }
    @Override
    public TestCaseInfo getTestCaseInfo(IMethod method, MetricsScope scope) { return null; }
    @Override
    public TestCaseInfo[] getTestCaseInfos(IMethod method, MetricsScope scope) { return new TestCaseInfo[0]; }
    @Override
    public HasMetrics metricsProviderFor(Object projectArtifact, MetricsScope scope) { return null; }
}
