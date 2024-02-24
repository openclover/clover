package org.openclover.core.registry.entities;

import org.openclover.core.api.registry.BlockMetrics;
import org.openclover.core.api.registry.ClassMetadata;
import org.openclover.core.api.registry.ContextSet;
import org.openclover.core.api.registry.EntityVisitor;
import org.openclover.core.api.registry.ModifiersInfo;
import org.openclover.core.api.registry.PackageInfo;

class FullClassMetadata implements ClassMetadata {
    /**
     * Base class name
     */
    protected String name;
    /**
     * Fully qualified class name
     */
    protected String qualifiedName;
    /**
     * Class modifiers (like public, static) with class annotations
     */
    protected Modifiers modifiers;

    /**
     * Whether this object is an interface
     */
    protected boolean typeInterface;
    /**
     * Whether this object is an enum
     */
    protected boolean typeEnum;
    /**
     * Whether this object is an annotation
     */
    protected boolean typeAnnotation;
    /**
     * Whether this object is a test class (according to custom or default test detector
     */
    protected boolean testClass;

    protected transient PackageInfo packageInfo;
    protected transient BlockMetrics rawMetrics;
    protected transient BlockMetrics metrics;
    protected transient ContextSet contextFilter;

    FullClassMetadata(PackageInfo packageInfo, String name, Modifiers modifiers,
                      boolean typeInterface, boolean typeEnum, boolean typeAnnotation) {
        this.name = name;
        this.packageInfo = packageInfo;
        this.typeInterface = typeInterface;
        this.typeEnum = typeEnum;
        this.typeAnnotation = typeAnnotation;
        this.qualifiedName = calcQualifiedName();
        this.modifiers = modifiers;
    }

    FullClassMetadata(String name, String qualifiedName, Modifiers modifiers,
                      boolean typeInterface, boolean typeEnum, boolean typeAnnotation, boolean testClass) {
        this.name = name;
        this.qualifiedName = qualifiedName;
        this.typeInterface = typeInterface;
        this.typeEnum = typeEnum;
        this.typeAnnotation = typeAnnotation;
        this.testClass = testClass;
        this.modifiers = modifiers;
    }

    @Override
    public String calcQualifiedName() {
        return
                packageInfo == null
                        ? null
                        : packageInfo.isDefault() ? getName() : packageInfo.getName() + "." + getName();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getQualifiedName() {
        if (qualifiedName == null && packageInfo != null) {
            qualifiedName = calcQualifiedName();
        }
        return qualifiedName;
    }

    @Override
    public ModifiersInfo getModifiers() {
        return modifiers;
    }

    @Override
    public PackageInfo getPackage() {
        return packageInfo;
    }

    @Override
    public void setPackage(PackageInfo packageInfo) {
        this.packageInfo = packageInfo;
    }

    @Override
    public boolean isInterface() {
        return typeInterface;
    }

    @Override
    public boolean isEnum() {
        return typeEnum;
    }

    @Override
    public boolean isAnnotationType() {
        return typeAnnotation;
    }

    @Override
    public BlockMetrics getRawMetrics() {
        return rawMetrics;
    }

    @Override
    public BlockMetrics getMetrics() {
        return metrics;
    }

    @Override
    public void setMetrics(BlockMetrics metrics) {
        this.metrics = metrics;
    }

    @Override
    public boolean isTestClass() {
        return testClass;
    }

    @Override
    public String toString() {
        return "[" + getQualifiedName() + ",test=" + isTestClass() + ",if=" + isInterface() + ",enum=" + isEnum() + ",anno=" + isAnnotationType() + "]";
    }

}
