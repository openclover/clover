package org.openclover.eclipse.core.projects.model;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IMethod;
import org.openclover.core.CloverDatabase;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.registry.entities.FullProjectInfo;
import org.openclover.core.registry.entities.TestCaseInfo;
import org.openclover.core.registry.metrics.BlockMetrics;
import org.openclover.eclipse.core.projects.CloverProject;
import org.openclover.eclipse.core.views.nodes.Nodes;

public abstract class MetricsScope {
    public static final MetricsScope APP_ONLY = new MetricsScope() {
        @Override
        public FullProjectInfo getProjectInfoFor(CloverProject project) {
            FullProjectInfo projectInfo = project.getModel().getAppOnlyProjectInfo();
            if (projectInfo == null) {
                projectInfo = FULL.getProjectInfoFor(project);
            }
            return projectInfo;
        }

        @Override
        public FullProjectInfo getProjectInfoFor(CloverDatabase database) {
            FullProjectInfo projectInfo = database.getAppOnlyModel();
            if (projectInfo == null) {
                projectInfo = FULL.getProjectInfoFor(database);
            }
            return projectInfo;
        }

        @Override
        public boolean isProjectInfoAvailable(CloverProject project) {
            return project.getModel().getAppOnlyProjectInfo() == null;
        }

        @Override
        public boolean isScopeClassContainer(Object element) throws CoreException {
            return Nodes.isAppClassContainer(element);
        }
    };
    public static final MetricsScope TEST_ONLY = new MetricsScope() {
        @Override
        public FullProjectInfo getProjectInfoFor(CloverProject project) {
            FullProjectInfo projectInfo = project.getModel().getTestOnlyProjectInfo();
            if (projectInfo == null) {
                projectInfo = FULL.getProjectInfoFor(project);
            }
            return projectInfo;
        }

        @Override
        public FullProjectInfo getProjectInfoFor(CloverDatabase database) {
            FullProjectInfo projectInfo = database.getTestOnlyModel();
            if (projectInfo == null) {
                projectInfo = FULL.getProjectInfoFor(database);
            }
            return projectInfo;
        }

        @Override
        public boolean isProjectInfoAvailable(CloverProject project) {
            return project.getModel().getTestOnlyProjectInfo() == null;
        }

        @Override
        public boolean isScopeClassContainer(Object element) throws CoreException {
            return Nodes.isTestClassContainer(element);
        }
    };
    public static final MetricsScope FULL = new MetricsScope() {
        @Override
        public FullProjectInfo getProjectInfoFor(CloverProject project) {
            return project.getModel().getFullProjectInfo();
        }

        @Override
        public FullProjectInfo getProjectInfoFor(CloverDatabase database) {
            return database.getFullModel();
        }

        @Override
        public boolean isProjectInfoAvailable(CloverProject project) {
            return true;
        }

        @Override
        public boolean isScopeClassContainer(Object element) {
            return true;
        }
    };
    public static final MetricsScope DEFAULT = FULL;

    public BlockMetrics getMetricsFor(Object target) {
        return (BlockMetrics)Platform.getAdapterManager().getAdapter(new Qualification(target, this), BlockMetrics.class);
    }

    public HasMetrics getHasMetricsFor(Object target) {
        return getHasMetricsFor(target, HasMetrics.class);
    }

    public HasMetrics getHasMetricsFor(Object target, Class clazz) {
        return (HasMetrics)Platform.getAdapterManager().getAdapter(new Qualification(target, this), clazz);
    }

    public TestCaseInfo getTestCaseInfoFor(IMethod method) {
        return (TestCaseInfo)Platform.getAdapterManager().getAdapter(new Qualification(method, this), TestCaseInfo.class);
    }

    public TestCaseInfo[] getTestCaseInfosFor(IMethod method) {
        return (TestCaseInfo[])Platform.getAdapterManager().getAdapter(new Qualification(method, this), TestCaseInfo[].class);
    }

    public abstract FullProjectInfo getProjectInfoFor(CloverProject project);
    public abstract FullProjectInfo getProjectInfoFor(CloverDatabase database);
    public abstract boolean isProjectInfoAvailable(CloverProject project);
    public abstract boolean isScopeClassContainer(Object element) throws CoreException;

    public static class Qualification implements IAdaptable {
        private Object adaptable;
        private MetricsScope scope;

        private Qualification(Object adaptable, MetricsScope scope) {
            this.adaptable = adaptable;
            this.scope = scope;
        }

        public Object getAdaptable() {
            return adaptable;
        }

        public MetricsScope getScope() {
            return scope;
        }

        @Override
        public Object getAdapter(Class aClass) { return null; }
    }
}
