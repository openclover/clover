package org.openclover.core.api.registry;

import org.jetbrains.annotations.Nullable;

/**
 * Represents an entity which can be closed in another entity, for example a statement
 * can be inside a method or function.
 */
public interface EntityEnclosure {

    /**
     * Returns a class in which this entity is declared or <code>null</code> otherwise.
     * A case for inner classes or methods inside clases or statements inside classes like field initializers.
     */
    @Nullable
    ClassInfo getContainingClass();

    /**
     * Returns a file in which this entity is declared or <code>null</code> otherwise.
     */
    @Nullable
    FileInfo getContainingFile();

    /**
     * Returns a method in which this entity is declared or <code>null</code> otherwise.
     * An anonymous inline class, a function inside a function, etc.
     */
    @Nullable
    MethodInfo getContainingMethod();

    void setContainingClass(ClassInfo containingClass);

    void setContainingMethod(MethodInfo methodInfo);

    void setContainingFile(FileInfo containingFile);

}
