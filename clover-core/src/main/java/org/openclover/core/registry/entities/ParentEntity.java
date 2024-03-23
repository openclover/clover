package org.openclover.core.registry.entities;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.EntityContainer;
import org.openclover.core.api.registry.EntityVisitor;
import org.openclover.core.api.registry.FileInfo;
import org.openclover.core.api.registry.MethodInfo;

/**
 * Helper class holding a reference to a parent of one of the following code entities:
 * <ul>
 *     <li>StatementInfo</li>
 *     <li>MethodInfo</li>
 *     <li>ClassInfo</li>
 * </ul>
 *
 * A statement/method/class can be declared inside:
 * <ul>
 *     <li>MethodInfo - e.g. code statements of the method, inline classes declared, inner functions</li>
 *     <li>ClassInfo - e.g. class members (functions, inner classes) </li>
 *     <li>FileInfo - e.g. top-level functions, statements</li>
 * </ul>
 *
 * A file as parent always exists, but two others not. Thus the containingClass and containingMethod are being set
 * exclusively, i.e. only of them can be non-null (it can also happen that both are null).
 */
public class ParentEntity {
    private final EntityContainer parentEntity;

    private FileInfo containingFile; // not final as we can create class first and next call setter

    public ParentEntity(@NotNull final ClassInfo containingClass) {
        this(containingClass, null);
    }

    public ParentEntity(@NotNull final MethodInfo containingMethod) {
        this(containingMethod, null);
    }

    public ParentEntity(@NotNull final FileInfo containingFile) {
        this(containingFile, containingFile);
    }

    public ParentEntity(@NotNull final EntityContainer parentEntity,
                         @Nullable final FileInfo containingFile) {
        this.parentEntity = parentEntity;
        this.containingFile = containingFile;
    }

    private static class ParentClassExtractor extends EntityVisitor {
        ClassInfo cls = null;

        @Override
        public void visitClass(ClassInfo parentClass) {
            cls = parentClass;
        }

        @Nullable
        public ClassInfo getClassInfo() {
             return cls;
        }
    }

    private static class ParentMethodExtractor extends EntityVisitor {
        MethodInfo methodInfo = null;

        @Override
        public void visitMethod(MethodInfo parentMethod) {
            methodInfo = parentMethod;
        }

        @Nullable
        public MethodInfo getMethodInfo() {
            return methodInfo;
        }
    }

    private static class FileExtractor extends EntityVisitor {
        FileInfo fileInfo = null;

        @Override
        public void visitClass(ClassInfo parentClass) {
            fileInfo = parentClass.getContainingFile();
        }

        @Override
        public void visitFile(FileInfo parentFile) {
            fileInfo = parentFile;
        }

        @Override
        public void visitMethod(MethodInfo parentMethod) {
            fileInfo = parentMethod.getContainingFile();
        }

        @Nullable
        public FileInfo getFileInfo() {
            return fileInfo;
        }
    }

    @Nullable
    public ClassInfo getContainingClass() {
        final ParentClassExtractor callback = new ParentClassExtractor();
        parentEntity.visit(callback);
        return callback.getClassInfo();
    }

    @Nullable
    public FileInfo getContainingFile() {
        final FileExtractor callback = new FileExtractor();
        parentEntity.visit(callback);
        return callback.getFileInfo() != null ? callback.getFileInfo() : containingFile;
    }

    @Nullable
    public MethodInfo getContainingMethod() {
        final ParentMethodExtractor callback = new ParentMethodExtractor();
        parentEntity.visit(callback);
        return callback.getMethodInfo();
    }

    @NotNull
    public EntityContainer getParentEntity() {
        return parentEntity;
    }

    public void setContainingFile(@NotNull final FileInfo fileInfo) {
        this.containingFile = fileInfo;
    }

}
