package org.openclover.core.api.registry;

public interface PackageFragment extends HasMetricsNode {

    void addChild(PackageFragment pkg);

    PackageFragment[] getChildren();

    PackageInfo getConcretePackage();

    void setConcretePackage(PackageInfo concretePackage);

    boolean isConcrete();

    PackageFragment getParent();

    ProjectInfo getContainingProject();

    String getQualifiedName();

    PackageFragment getChild(String name);
}
