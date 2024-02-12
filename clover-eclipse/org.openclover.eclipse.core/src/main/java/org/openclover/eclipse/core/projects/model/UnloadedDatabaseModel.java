package org.openclover.eclipse.core.projects.model;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.openclover.core.CloverDatabase;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.registry.entities.BaseClassInfo;
import org.openclover.core.registry.entities.BaseFileInfo;
import org.openclover.core.registry.entities.FullMethodInfo;
import org.openclover.core.registry.entities.FullProjectInfo;
import org.openclover.core.registry.entities.TestCaseInfo;
import org.openclover.eclipse.core.projects.CloverProject;

public class UnloadedDatabaseModel
    extends StableDatabaseModel {

    public UnloadedDatabaseModel(CloverProject project) {
        super(project, CoverageModelChangeEvent.INIT(project));
    }

    protected UnloadedDatabaseModel(CloverProject project, CoverageModelChangeEvent changeEvent) {
        super(project, changeEvent);
    }

    @Override
    public CloverDatabase getDatabase() {
        return null;
    }

    @Override
    public boolean isLoaded() { return false; }
    @Override
    public boolean isLoading() { return false; }

    @Override
    public boolean isCoverageOutOfDate() { return !CloverDatabase.isRecordingInProgress(project.getRegistryFile()); }
    @Override
    public boolean isRegistryOfDate() { return true; }

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
    public TestCaseInfo getTestCaseInfo(IMethod method, MetricsScope scope) { return null; }
    @Override
    public TestCaseInfo[] getTestCaseInfos(IMethod method, MetricsScope scope) { return new TestCaseInfo[] {}; }
    @Override
    public FullMethodInfo getMethodInfo(IMethod method, MetricsScope scope) { return null; }

    @Override
    public HasMetrics metricsProviderFor(Object projectArtifact, MetricsScope scope) { return null; }
}
