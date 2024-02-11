package org.openclover.core.api.registry;

/**
 * Callback handler for {@link EntityContainer}. The <code>EntityContainer.visit()</code> will call one of the methods
 * in this interface, depending on the runtime type of the EntityContainer.
 */
public abstract class EntityVisitor {
    public void visitProject(ProjectInfo parentProject) {
        // nothing to do
    }

    public void visitPackage(PackageInfo parentPackage) {
        // nothing to do
    }

    public void visitFile(FileInfo parentFile) {
        // nothing to do
    }

    public void visitClass(ClassInfo parentClass) {
        // nothing to do
    }

    public void visitMethod(MethodInfo parentMethod) {
        // nothing to do
    }
}
