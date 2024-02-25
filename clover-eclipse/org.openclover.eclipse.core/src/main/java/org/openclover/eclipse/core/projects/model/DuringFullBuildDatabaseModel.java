package org.openclover.eclipse.core.projects.model;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.openclover.core.CloverDatabase;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.FileInfo;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.api.registry.ProjectInfo;
import org.openclover.core.api.registry.TestCaseInfo;
import org.openclover.core.registry.entities.FullMethodInfo;
import org.openclover.core.registry.entities.FullProjectInfo;
import org.openclover.eclipse.core.projects.CloverProject;

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
    public ProjectInfo getFullProjectInfo() { return null; }
    @Override
    public ProjectInfo getTestOnlyProjectInfo() { return null; }
    @Override
    public ProjectInfo getAppOnlyProjectInfo() { return null; }
    @Override
    public HasMetrics getPackageInfoOrFragment(IPackageFragment pack, MetricsScope scope) { return null; }
    @Override
    public FileInfo getSourceFileInfo(ICompilationUnit cu, MetricsScope scope) { return null; }
    @Override
    public ClassInfo getTypeInfo(IType type, MetricsScope scope) { return null; }
    @Override
    public FullMethodInfo getMethodInfo(IMethod method, MetricsScope scope) { return null; }
    @Override
    public TestCaseInfo getTestCaseInfo(IMethod method, MetricsScope scope) { return null; }
    @Override
    public TestCaseInfo[] getTestCaseInfos(IMethod method, MetricsScope scope) { return new TestCaseInfo[0]; }
    @Override
    public HasMetrics metricsProviderFor(Object projectArtifact, MetricsScope scope) { return null; }
}
