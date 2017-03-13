package com.atlassian.clover;

public final class ClassInstanceProxy {
    private final String name;
    private final int classHashCode;
    private final int classloaderHashCode;
    private final int hashCode;

    public ClassInstanceProxy(Class classInstance) {
        name = classInstance.getName();
        classHashCode = classInstance.hashCode();
        classloaderHashCode = classInstance.getClassLoader() == null ? 0 : System.identityHashCode(classInstance.getClassLoader());
        hashCode = 31 * ((31 * name.hashCode()) + classHashCode) + classloaderHashCode;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClassInstanceProxy that = (ClassInstanceProxy)o;

        if (classloaderHashCode != that.classloaderHashCode) return false;
        if (classHashCode != that.classHashCode) return false;
        if (!name.equals(that.name)) return false;

        return true;
    }

    public int hashCode() {
        return hashCode;
    }
}
