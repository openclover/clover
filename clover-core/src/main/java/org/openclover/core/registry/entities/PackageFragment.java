package org.openclover.core.registry.entities;

import org.openclover.core.api.registry.BlockMetrics;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.registry.metrics.HasMetricsNode;
import org.openclover.core.registry.metrics.PackageMetrics;
import org.openclover.core.registry.metrics.ProjectMetrics;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.openclover.core.util.Lists.newArrayList;
import static org.openclover.core.util.Maps.newTreeMap;

public class PackageFragment implements HasMetricsNode {

    private FullProjectInfo containingProject;
    private PackageFragment parent;
    private List orderedKids;
    private Map<String, PackageFragment> children;
    private BlockMetrics rawMetrics;
    private BlockMetrics metrics;
    private FullPackageInfo concretePackage;
    private String qualifiedName;
    private String name;
    private Comparator orderby;


    public PackageFragment(PackageFragment parent, FullProjectInfo containingProject,
                           String fqn, String name) {
        this.parent = parent;
        this.containingProject = containingProject;
        this.qualifiedName = fqn;
        this.name = name;
    }

    void addChild(PackageFragment pkg) {
        if (children == null) {
            children = newTreeMap(); // natural ordering used
        }
        children.put(pkg.getName(), pkg);
    }

    public PackageFragment[] getChildren() {
        if (children != null) {
            return children.values().toArray(new PackageFragment[0]);
        }
        return new PackageFragment[0];
    }


    public FullPackageInfo getConcretePackage() {
        return concretePackage;
    }

    public void setConcretePackage(FullPackageInfo concretePackage) {
        this.concretePackage = concretePackage;
    }

    public boolean isConcrete() {
        return concretePackage != null;
    }

    public PackageFragment getParent() {
        return parent;
    }

    public FullProjectInfo getContainingProject() {
        return containingProject;
    }

    public String getQualifiedName() {
        return isConcrete() ? concretePackage.getName() : qualifiedName;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isEmpty() {
        return (children == null || children.size() == 0) && (concretePackage == null || concretePackage.isEmpty());
    }

    @Override
    public BlockMetrics getMetrics() {
        if (metrics == null) {
            metrics = calcMetrics(true);
        }
        return metrics;
    }

    @Override
    public BlockMetrics getRawMetrics() {
        if (rawMetrics == null) {
            rawMetrics = calcMetrics(false);
        }
        return rawMetrics;
    }

    @Override
    public void setMetrics(BlockMetrics metrics) {
        this.metrics = metrics;
    }

    public PackageFragment getChild(String name) {
        if (children != null) {
            return children.get(name);
        }
        return null;
    }

    @Override
    public String getChildType() {
        // hack - this doesn't make much sense, since child type could either be class or package
        return isConcrete() ? concretePackage.getChildType() : "package";
    }

    @Override
    public int getNumChildren() {

        int kids = 0;
        if (children != null) {
            if (orderedKids == null) {
                buildOrderedChildList();
            }
            kids += orderedKids.size();
        }

        if (isConcrete()) {
            kids += concretePackage.getNumChildren();
        }
        return kids;
    }

    @Override
    public HasMetricsNode getChild(int i) {

        int offset = 0;
        if (children != null) {
            if (orderedKids == null) {
                buildOrderedChildList();
            }
            if (i < orderedKids.size()) {
                return (HasMetricsNode)orderedKids.get(i);
            }
            offset = orderedKids.size();
        }
        if (isConcrete()) {
            return concretePackage.getChild(i - offset);
        }

        return null;
    }

    @Override
    public int getIndexOfChild(HasMetricsNode child) {
        if (child instanceof PackageFragment && children != null) {
            if (orderedKids == null) {
                buildOrderedChildList();
            }
            return orderedKids.indexOf(child);
        }
        else if (child instanceof ClassInfo && isConcrete()) {
            return concretePackage.getIndexOfChild(child);
        }
        return -1;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public void setComparator(Comparator cmp) {
        orderby = cmp;
        orderedKids = null;
    }

    private void buildOrderedChildList() {
        orderedKids = newArrayList(children.values());
        if (orderby != null) {
            orderedKids.sort(orderby);
        }
    }

    private ProjectMetrics calcMetrics(boolean filter) {
        ProjectMetrics metrics = new ProjectMetrics(this);
        if (isConcrete()) {
            if (!filter) {
                metrics.add((PackageMetrics)concretePackage.getRawMetrics());
            } else {
                metrics.add((PackageMetrics)concretePackage.getMetrics());
            }
            metrics.setNumPackages(1);
        }
        if (children != null) {
            for (PackageFragment pkgFrag : children.values()) {
                if (!filter) {
                    metrics.add((ProjectMetrics) pkgFrag.getRawMetrics());
                } else {
                    metrics.add((ProjectMetrics) pkgFrag.getMetrics());
                }
            }
        }
        return metrics;
    }
}
