package org.openclover.idea.report.treemap;

import com.atlassian.clover.CloverDatabase;
import com.atlassian.clover.api.registry.ClassInfo;
import com.atlassian.clover.api.registry.PackageInfo;
import com.atlassian.clover.registry.entities.FullClassInfo;
import com.atlassian.clover.registry.entities.FullProjectInfo;
import clover.net.sf.jtreemap.swing.TreeMapNode;
import clover.net.sf.jtreemap.swing.TreeMapNodeBuilder;
import clover.net.sf.jtreemap.swing.Value;

import java.util.List;

/**
 * Builds a treemap from Clover Database.
 */
public class ProjectHeapMapBuilder extends TreeMapNodeBuilder {
    public TreeMapNode buildTree(CloverDatabase cloverDatabase) {
        final FullProjectInfo projectInfo = cloverDatabase.getAppOnlyModel();
        final TreeMapNode root = buildBranch("", null);

        final List<? extends PackageInfo> packages = projectInfo.getAllPackages();
        for (final PackageInfo packageInfo : packages) {
            final TreeMapNode branch = buildBranch(packageInfo.getName(), root);

            final List<? extends ClassInfo> classes = packageInfo.getClasses();
            for (final ClassInfo classInfo : classes) {
                final double weight = classInfo.getMetrics().getNumElements();
                final Value value = new ClassInfoValue((FullClassInfo)classInfo);
                buildLeaf(classInfo.getName(), weight, value, branch);
            }
        }

        return root;
    }
}
