package com.atlassian.clover.eclipse.core.ui.editors.treemap;

import com.atlassian.clover.api.registry.ClassInfo;
import com.atlassian.clover.api.registry.HasMetrics;
import com.atlassian.clover.api.registry.PackageInfo;
import com.atlassian.clover.registry.entities.BaseClassInfo;
import com.atlassian.clover.registry.entities.BasePackageInfo;
import com.atlassian.clover.registry.entities.FullProjectInfo;
import com.atlassian.clover.eclipse.core.projects.CloverProject;
import com.atlassian.clover.eclipse.core.projects.model.MetricsScope;
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
