package org.openclover.core.api.registry;

public interface ClassMetadata {

    String calcQualifiedName();

    String getName();

    String getQualifiedName();

    ModifiersInfo getModifiers();

    PackageInfo getPackage();

    void setPackage(PackageInfo packageInfo);

    boolean isInterface();

    boolean isEnum();

    boolean isAnnotationType();

    BlockMetrics getRawMetrics();

    BlockMetrics getMetrics();

    void setMetrics(BlockMetrics metrics);

    boolean isTestClass();

}
