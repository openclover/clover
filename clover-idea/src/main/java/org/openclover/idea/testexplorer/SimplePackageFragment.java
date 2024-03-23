package org.openclover.idea.testexplorer;

import org.openclover.core.api.registry.BlockMetrics;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.api.registry.PackageInfo;
import org.openclover.core.registry.entities.FullPackageInfo;

import java.util.Collection;
import java.util.Map;

import static org.openclover.core.util.Maps.newHashMap;

public class SimplePackageFragment implements HasMetrics {
    private final String name;
    private Map<String, SimplePackageFragment> children = newHashMap();
    private PackageInfo concretePackage;

    SimplePackageFragment(String name) {
        this.name = name;
    }

    SimplePackageFragment add(String shortName) {
        final SimplePackageFragment packageFragment = new SimplePackageFragment(shortName);
        children.put(shortName, packageFragment);
        return packageFragment;
    }

    public void setConcretePackage(PackageInfo concretePackage) {
        this.concretePackage = concretePackage;
    }

    public PackageInfo getConcretePackage() {
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
