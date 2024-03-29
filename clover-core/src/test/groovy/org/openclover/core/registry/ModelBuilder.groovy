package org.openclover.core.registry

import org.openclover.core.api.registry.ClassInfo
import org.openclover.core.api.registry.FileInfo
import org.openclover.core.api.registry.MethodInfo
import org.openclover.core.api.registry.PackageInfo
import org.openclover.core.api.registry.ProjectInfo
import org.openclover.core.context.ContextSetImpl
import org.openclover.core.registry.entities.BasicElementInfo
import org.openclover.core.registry.entities.FullClassInfo
import org.openclover.core.registry.entities.FullFileInfo
import org.openclover.core.registry.entities.FullMethodInfo
import org.openclover.core.registry.entities.FullPackageInfo
import org.openclover.core.registry.entities.FullProjectInfo
import org.openclover.core.registry.entities.FullStatementInfo
import org.openclover.core.registry.entities.MethodSignature
import org.openclover.core.registry.entities.Modifiers
import org.openclover.core.spi.lang.LanguageConstruct

import static org.openclover.core.registry.entities.FullMethodInfo.DEFAULT_METHOD_COMPLEXITY
import static org.openclover.core.spi.lang.LanguageConstruct.Builtin.METHOD
import static org.openclover.core.util.Maps.newHashMap

/**
 * A helper class to build a hierarchical structure of a project, packages, classes, methods and statements
 * for testing purposes.
 */
class ModelBuilder {
    Map<String, Object> elements = newHashMap()

    ProjectWrapper proj(String name) {
        return new ProjectWrapper(new FullProjectInfo(name))
    }

    private void putElement(String key, Object element) {
        elements.put(key, element)
    }

    Object get(String key) {
        return elements.get(key)
    }

    class ProjectWrapper extends Wrapper<ProjectWrapper, ProjectInfo> {
        ProjectWrapper(ProjectInfo projectInfo) {
            super(projectInfo)
        }

        PackageInfoWrapper pkg(String name) {
            for (PackageInfo pkgInfo : getElement().getAllPackages()) {
                if (name == pkgInfo.getName()) {
                    return new PackageInfoWrapper(pkgInfo)
                }
            }
            final PackageInfo pkgInfo = new FullPackageInfo(getElement(), name, 0)
            getElement().addPackage(pkgInfo)
            return new PackageInfoWrapper(pkgInfo)
        }

        ModelBuilder end() {
            return ModelBuilder.this
        }

        protected ProjectWrapper getThis() {
            return this
        }
    }

    class PackageInfoWrapper extends Wrapper<PackageInfoWrapper, PackageInfo>{
        PackageInfoWrapper(PackageInfo pkgInfo) {
            super(pkgInfo)
        }

        ProjectWrapper end() {
            return new ProjectWrapper(getElement().getContainingProject())
        }

        FileInfoWrapper file(String name) {
            FileInfo fileInfo = getElement().getFile(getElement().getPath() + "/" + name)
            if (fileInfo == null) {
                fileInfo = new FullFileInfo(getElement(), new File(name), "UTF-8", 0, 0, 0, 0, 0, 0, 0)
                getElement().addFile(fileInfo)
            }
            return new FileInfoWrapper(fileInfo)
        }

        protected PackageInfoWrapper getThis() {
            return this
        }
    }

    class FileInfoWrapper extends Wrapper<FileInfoWrapper, FileInfo> {
        FileInfoWrapper(FileInfo fileInfo) {
            super(fileInfo)
        }

        /** Declare a class inside a file */
        ClassInfoWrapper clazz(String name) {
            final List<ClassInfo> classes = getElement().getClasses()
            for (ClassInfo classInfo : classes) {
                if (classInfo.getName() == name) {
                    return new ClassInfoWrapper(classInfo)
                }
            }
            final FullClassInfo newClassInfo = new FullClassInfo(
                    getElement().getContainingPackage(), getElement(),
                    0, name, new FixedSourceRegion(0, 0), new Modifiers(),
                    false, false, false)

            getElement().addClass(newClassInfo)

            return new ClassInfoWrapper(newClassInfo)
        }

        /** Declare a method inside a file */
        MethodInfoWrapper method(String name, boolean isTest) {
            final FullMethodInfo method = new FullMethodInfo(
                    getElement(),
                    new MethodSignature(name),
                    new ContextSetImpl(),
                    new BasicElementInfo(new FixedSourceRegion(0, 0), 0, DEFAULT_METHOD_COMPLEXITY, METHOD),
                    isTest, null, false)
            getElement().addMethod(method)
            return new MethodInfoWrapper(method)
        }

        /** Declare a statement inside a file */
        StatementInfoWrapper stmt(int index) {
            final FullStatementInfo stmt = new FullStatementInfo(getElement(), new ContextSetImpl(),
                    new BasicElementInfo(new FixedSourceRegion(0, 0), index, 1, LanguageConstruct.Builtin.STATEMENT))
            getElement().addStatement(stmt)
            return new StatementInfoWrapper(stmt)
        }

        PackageInfoWrapper end() {
            return new PackageInfoWrapper(getElement().getContainingPackage())
        }

        protected FileInfoWrapper getThis() {
            return this
        }
    }

    class ClassInfoWrapper extends Wrapper<ClassInfoWrapper, ClassInfo> {
        ClassInfoWrapper(ClassInfo classInfo) {
            super(classInfo)
        }

        /** Declare class inside a class */
        ClassInfoWrapper clazz(String name) {
            final ClassInfo newClassInfo = new FullClassInfo(
                    getElement().getPackage(), getElement(),
                    0, name, new FixedSourceRegion(0, 0), new Modifiers(),
                    false, false, false)
            getElement().addClass(newClassInfo)
            return new ClassInfoWrapper(newClassInfo)
        }

        /** Declare test method inside a class */
        MethodInfoWrapper testMethod(String name) {
            return method(name, true)
        }

        /** Declare method inside a class */
        MethodInfoWrapper method(String name) {
            return method(name, false)
        }

        /** Declare statement inside a class */
        StatementInfoWrapper stmt(int index) {
            final FullStatementInfo stmt = new FullStatementInfo(getElement(), new ContextSetImpl(),
                    new BasicElementInfo(new FixedSourceRegion(0, 0), index, 1, LanguageConstruct.Builtin.STATEMENT))
            getElement().addStatement(stmt)
            return new StatementInfoWrapper(stmt)
        }

        /** Close class wrapper declared inside a class */
        ClassInfoWrapper endInClass() {
            return new ClassInfoWrapper(getElement().getContainingClass())
        }

        /** Close class wrapper declared inside a file */
        FileInfoWrapper endInFile() {
            return new FileInfoWrapper(getElement().getContainingFile())
        }

        /** Close class wrapper declared inside a method */
        MethodInfoWrapper endInMethod() {
            return new MethodInfoWrapper(getElement().getContainingMethod())
        }

        protected ClassInfoWrapper getThis() {
            return this
        }

        MethodInfoWrapper method(String name, boolean isTest) {
            final FullMethodInfo method = new FullMethodInfo(
                    getElement(),
                    new MethodSignature(name),
                    new ContextSetImpl(),
                    new BasicElementInfo(new FixedSourceRegion(0, 0), 0, DEFAULT_METHOD_COMPLEXITY, METHOD),
                    isTest, null, false)
            getElement().addMethod(method)
            return new MethodInfoWrapper(method)
        }
    }

    class MethodInfoWrapper extends Wrapper<MethodInfoWrapper, MethodInfo> {
        /** Declare a class inside a method */
        ClassInfoWrapper clazz(String name) {
            final FullClassInfo newClassInfo = new FullClassInfo(
                    getElement().getContainingFile().getContainingPackage(),
                    getElement(),
                    0, name, new FixedSourceRegion(0, 0), new Modifiers(),
                    false, false, false)
            getElement().addClass(newClassInfo)
            return new ClassInfoWrapper(newClassInfo)
        }

        /** Declare a method inside a method */
        MethodInfoWrapper method(String name, boolean isTest) {
            final FullMethodInfo method = new FullMethodInfo(
                    getElement(),
                    new MethodSignature(name),
                    new ContextSetImpl(),
                    new BasicElementInfo(new FixedSourceRegion(0, 0), 0, DEFAULT_METHOD_COMPLEXITY, METHOD),
                    isTest, null, false)
            getElement().addMethod(method)
            return new MethodInfoWrapper(method)
        }

        /** Declare a statement inside a method */
        StatementInfoWrapper stmt(int index) {
            final FullStatementInfo stmt = new FullStatementInfo(getElement(), new ContextSetImpl(),
                    new BasicElementInfo(new FixedSourceRegion(0, 0), index, 1, LanguageConstruct.Builtin.STATEMENT))
            getElement().addStatement(stmt)
            return new StatementInfoWrapper(stmt)
        }

        /** Close method wrapper declared inside a class */
        ClassInfoWrapper endInClass() {
            return new ClassInfoWrapper(getElement().getContainingClass())
        }

        /** Close method wrapper declared inside a method */
        MethodInfoWrapper endInMethod() {
            return new MethodInfoWrapper(getElement().getContainingMethod())
        }

        /** Close method wrapper declared inside a file */
        FileInfoWrapper endInFile() {
            return new FileInfoWrapper(getElement().getContainingFile())
        }

        protected MethodInfoWrapper(MethodInfo element) {
            super(element)
        }

        protected MethodInfoWrapper getThis() {
            return this
        }
    }

    class StatementInfoWrapper extends Wrapper<StatementInfoWrapper, FullStatementInfo> {
        /** Close statement wrapper declared inside a class */
        ClassInfoWrapper endInClass() {
            return new ClassInfoWrapper(getElement().getContainingClass())
        }

        /** Close statement wrapper declared inside a method */
        MethodInfoWrapper endInMethod() {
            return new MethodInfoWrapper(getElement().getContainingMethod())
        }

        /** Close statement wrapper declared inside a file */
        FileInfoWrapper endInFile() {
            return new FileInfoWrapper(getElement().getContainingFile())
        }

        protected StatementInfoWrapper(FullStatementInfo element) {
            super(element)
        }

        protected StatementInfoWrapper getThis() {
            return this
        }
    }

    abstract class Wrapper<X extends Wrapper, Y> {
        private Y element

        protected Wrapper(Y element) {
            this.element = element
        }

        X withId(String name) {
            ModelBuilder.this.putElement(name, getElement())
            return getThis()
        }

        protected abstract X getThis()

        Y getElement() {
            return element
        }
    }
}
