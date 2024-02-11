package org.openclover.eclipse.core.ui.editors.treemap;

import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.api.registry.PackageInfo;
import org.openclover.core.registry.entities.BaseClassInfo;
import org.openclover.core.registry.entities.BasePackageInfo;
import org.openclover.core.registry.entities.FullProjectInfo;
import org.openclover.eclipse.core.projects.CloverProject;
import org.openclover.eclipse.core.projects.model.MetricsScope;
import net.sf.jtreemap.ktreemap.TreeMapNode;
import net.sf.jtreemap.ktreemap.TreeMapNodeBuilder;

import java.util.List;

public class ProjectHeatMapBuilder extends TreeMapNodeBuilder {
    public TreeMapNode buildTree(CloverProject project) {
        final FullProjectInfo projectInfo = MetricsScope.FULL.getProjectInfoFor(project);
        final TreeMapNode root = buildBranch(projectInfo, null);

        final List<? extends PackageInfo> packages = projectInfo.getAllPackages();
        for (final PackageInfo packageInfo : packages) {
            final BasePackageInfo basePackageInfo = (BasePackageInfo) packageInfo;
            final TreeMapNode branch = buildBranch(basePackageInfo, root);

            final List<? extends ClassInfo> classes = basePackageInfo.getClasses();
            for (final ClassInfo classInfo : classes) {
                final BaseClassInfo baseClassInfo = (BaseClassInfo) classInfo;
                buildLeaf(baseClassInfo, branch);
            }
        }

        return root;
    }


    @Override
    public double getWeight(Object value) {
        return value instanceof HasMetrics ? ((HasMetrics) value).getMetrics().getNumElements() : 0.0d;
    }
}
