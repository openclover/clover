package org.openclover.eclipse.core.views.coverageexplorer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.openclover.core.registry.metrics.BlockMetrics;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.CloverProject;
import org.openclover.eclipse.core.projects.model.MetricsScope;
import org.openclover.eclipse.core.views.CloveredWorkspaceProvider;
import org.openclover.eclipse.core.views.TypeUtils;
import org.openclover.eclipse.core.views.WorkingSetNodeFilter;
import org.openclover.eclipse.core.views.nodes.NodeRelationshipFilter;

import static org.openclover.eclipse.core.CloverPlugin.logError;

public class CoverageProvider
    extends CloveredWorkspaceProvider {

    public CoverageProvider(CoverageView part, CoverageViewSettings settings) {
        super(part, settings);
    }

    @Override
    protected NodeRelationshipFilter getFilter() {
        return new WorkingSetNodeFilter() {

            @Override
            public boolean accept(Object element) {
                return
                    (super.accept(element)
                    && inMetricsScope(element)
                    && inMetricsRange(element))
                    || isMethod(element)
                    || isCloverProjectAndModelNotYetLoaded(element);
            }

            private boolean isCloverProjectAndModelNotYetLoaded(Object element) {
                if (element instanceof IProject) {
                    try {
                        CloverProject project = CloverProject.getFor((IProject)element);
                        return project != null && !project.getModel().isLoaded();
                    } catch (CoreException e) {
                        //Ignore
                    }
                }
                return false;
            }

            private boolean isMethod(Object element) {
                return element instanceof IMethod;
            }

            private boolean inMetricsScope(Object element) {
                try {
                    return getSettings().getMetricsScope().isScopeClassContainer(element);
                } catch (CoreException e) {
                    return false;
                }
            }

            private boolean inMetricsRange(Object element) {
                BlockMetrics fullMetrics = MetricsScope.FULL.getMetricsFor(element);
                BlockMetrics appropriateMetrics = getSettings().getMetricsScope().getMetricsFor(element);

                boolean accept = false;
                prerequisites:
                {
                    if (getSettings().shouldHideUnavailableCoverage()
                        && isCoverageUnavailable(element, appropriateMetrics)) {
                        break prerequisites;
                    }
                    if (getSettings().shouldHideFullyCovered()
                        && isFullyCovered(element, fullMetrics)) {
                        break prerequisites;
                    }
                    accept = true;
                }
                return accept;
            }

            @Override
            public boolean requiresFiltering() {
                return
                    super.requiresFiltering()
                        || (getSettings().getCoverageModel() != CoverageViewSettings.COVERAGE_MODEL_FULL)
                        || getSettings().shouldHideUnavailableCoverage()
                        || getSettings().shouldHideFullyCovered();
            }
        };
    }

    private boolean isFullyCovered(Object element, BlockMetrics metrics) {
        boolean result =
            (metrics != null
                && (metrics.getNumElements() > 0)
                && (metrics.getNumCoveredElements() == metrics.getNumElements())
                //Always show the project so as not to confuse or users.
                && !TypeUtils.isProject(element));
        if (result) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isCoverageUnavailable(Object element, BlockMetrics metrics) {
        try {
            if (TypeUtils.isPackageFragmentRoot(element)) {
                //HACK: adaptation of fragment roots to blockmetrics doesn't work
                //hence why we need the special handling here. until the
                //above is fixed this path always need to be present
                return false;
            } else if (TypeUtils.isType(element)) {
                //Special handling of types - inner types may have coverage even if outer types don't
                //and this may happen at any depth of nested inner classes
                if (TypeUtils.unavailableCoverage(metrics)) {
                    IType type = (IType) element;
                    IType[] innerTypes = type.getTypes();
                    for (IType innerType : innerTypes) {
                        if (!isCoverageUnavailable(innerType, MetricsScope.FULL.getMetricsFor(innerType))) {
                            //For the first inner class that has coverage, we bail
                            //and the parent type is now deemed to have coverage
                            //so that the path to the child is enabled
                            return false;
                        }
                    }
                    return true;
                } else {
                    return false;
                }
            } else {
                //For all non-types & non-package fragment roots, just determine
                //based on metrics alone
                return TypeUtils.unavailableCoverage(metrics);
            }
        } catch (JavaModelException e) {
            logError("Unable to determine inner classes", e);
            //Show the element if there's an error
            return false;
        }
    }

    private CoverageViewSettings getSettings() {
        return (CoverageViewSettings)settings;
    }
}
