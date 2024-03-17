package org.openclover.eclipse.core.projects.model;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.openclover.core.CloverDatabase;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.FileInfo;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.api.registry.MethodInfo;
import org.openclover.core.api.registry.ProjectInfo;
import org.openclover.core.api.registry.TestCaseInfo;
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
    public TestCaseInfo getTestCaseInfo(IMethod method, MetricsScope scope) { return null; }
    @Override
    public TestCaseInfo[] getTestCaseInfos(IMethod method, MetricsScope scope) { return new TestCaseInfo[] {}; }
    @Override
    public MethodInfo getMethodInfo(IMethod method, MetricsScope scope) { return null; }

    @Override
    public HasMetrics metricsProviderFor(Object projectArtifact, MetricsScope scope) { return null; }
}
