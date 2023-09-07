package com.atlassian.clover.idea.testexplorer;

import com.atlassian.clover.api.registry.BlockMetrics;
import com.atlassian.clover.api.registry.HasMetrics;
import com.atlassian.clover.registry.entities.FullPackageInfo;

import java.util.Collection;
import java.util.Map;

import static clover.com.google.common.collect.Maps.newHashMap;

public class SimplePackageFragment implements HasMetrics {
    private final String name;
    private Map<String, SimplePackageFragment> children = newHashMap();
    private FullPackageInfo concretePackage;

    SimplePackageFragment(String name) {
        this.name = name;
    }

    SimplePackageFragment add(String shortName) {
        final SimplePackageFragment packageFragment = new SimplePackageFragment(shortName);
        children.put(shortName, packageFragment);
        return packageFragment;
    }

    public void setConcretePackage(FullPackageInfo concretePackage) {
        this.concretePackage = concretePackage;
    }

    public FullPackageInfo getConcretePackage() {
        return concretePackage;
    }

    SimplePackageFragment getChild(String shortName) {
        return children.get(shortName);
    }

    Collection<SimplePackageFragment> getChildren() {
        return children.values();
    }

    void cleanup() {
        children = null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public BlockMetrics getMetrics() {
        return concretePackage != null ? concretePackage.getMetrics() : null;
    }

    @Override
    public BlockMetrics getRawMetrics() {
        return concretePackage != null ? concretePackage.getRawMetrics() : null;
    }

    @Override
    public void setMetrics(BlockMetrics metrics) {
        throw new UnsupportedOperationException("method setMetrics not implemented");
    }
}
