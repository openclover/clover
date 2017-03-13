package com.atlassian.clover.registry

import clover.com.google.common.collect.Maps
import com.atlassian.clover.api.registry.PackageInfo
import com.atlassian.clover.context.ContextSet
import com.atlassian.clover.registry.entities.BasicElementInfo
import com.atlassian.clover.registry.entities.BasicMethodInfo
import com.atlassian.clover.registry.entities.FullClassInfo
import com.atlassian.clover.registry.entities.FullFileInfo
import com.atlassian.clover.registry.entities.FullMethodInfo
import com.atlassian.clover.registry.entities.FullPackageInfo
import com.atlassian.clover.registry.entities.FullProjectInfo
import com.atlassian.clover.registry.entities.FullStatementInfo
import com.atlassian.clover.registry.entities.MethodSignature
import com.atlassian.clover.registry.entities.Modifiers
import com.atlassian.clover.spi.lang.LanguageConstruct

/**
 * A helper class to build a hierarchical structure of a project, packages, classes, methods and statements
 * for testing purposes.
 */
class ModelBuilder {
    Map<String, Object> elements = Maps.newHashMap()

    ProjectWrapper proj(String name) {
        return new ProjectWrapper(new FullProjectInfo(name))
    }

    private void putElement(String key, Object element) {
        elements.put(key, element)
    }

    Object get(String key) {
        return elements.get(key)
    }

    class ProjectWrapper extends Wrapper<ProjectWrapper, FullProjectInfo> {
        ProjectWrapper(FullProjectInfo projectInfo) {
            super(projectInfo)
        }

        PackageInfoWrapper pkg(String name) {
            for (PackageInfo pkgInfo : getElement().getAllPackages()) {
                if (name.equals(pkgInfo.getName())) {
                    return new PackageInfoWrapper((FullPackageInfo)pkgInfo)
                }
            }
            final FullPackageInfo pkgInfo = new FullPackageInfo(getElement(), name, 0)
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

    class PackageInfoWrapper extends Wrapper<PackageInfoWrapper, FullPackageInfo>{
        PackageInfoWrapper(FullPackageInfo pkgInfo) {
            super(pkgInfo)
        }

        ProjectWrapper end() {
            return new ProjectWrapper((FullProjectInfo)getElement().getContainingProject())
        }

        FileInfoWrapper file(String name) {
            FullFileInfo fileInfo = (FullFileInfo)getElement().getFile(getElement().getPath() + "/" + name)
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

    class FileInfoWrapper extends Wrapper<FileInfoWrapper, FullFileInfo> {
        FileInfoWrapper(FullFileInfo fileInfo) {
            super(fileInfo)
        }

        /** Declare a class inside a file */
        ClassInfoWrapper clazz(String name) {
            final List<? extends com.atlassian.clover.api.registry.ClassInfo> classes = getElement().getClasses()
            for (com.atlassian.clover.api.registry.ClassInfo classInfo : classes) {
                if (classInfo.getName().equals(name)) {
                    return new ClassInfoWrapper((FullClassInfo)classInfo)
                }
            }
            final FullClassInfo newClassInfo = new FullClassInfo(
                    (FullPackageInfo)getElement().getContainingPackage(), getElement(),
                    0, name, new FixedSourceRegion(0, 0), new Modifiers(),
                    false, false, false)

            getElement().addClass(newClassInfo)

            return new ClassInfoWrapper(newClassInfo)
        }

        /** Declare a method inside a file */
        MethodInfoWrapper method(String name, boolean isTest) {
            final FullMethodInfo method = new FullMethodInfo(
                    getElement(),
                    new ContextSet(),
                    new BasicMethodInfo(new FixedSourceRegion(0, 0), 0,
                            FullMethodInfo.DEFAULT_METHOD_COMPLEXITY, new MethodSignature(name),
                            isTest, null, false))
            getElement().addMethod(method)
            return new MethodInfoWrapper(method)
        }

        /** Declare a statement inside a file */
        StatementInfoWrapper stmt(int index) {
            final FullStatementInfo stmt = new FullStatementInfo(getElement(), new ContextSet(),
                    new BasicElementInfo(new FixedSourceRegion(0, 0), index, 1, LanguageConstruct.Builtin.STATEMENT))
            getElement().addStatement(stmt)
            return new StatementInfoWrapper(stmt)
        }

        PackageInfoWrapper end() {
            return new PackageInfoWrapper((FullPackageInfo)getElement().getContainingPackage())
        }

        protected FileInfoWrapper getThis() {
            return this
        }
    }

    class ClassInfoWrapper extends Wrapper<ClassInfoWrapper, FullClassInfo> {
        ClassInfoWrapper(FullClassInfo classInfo) {
            super(classInfo)
        }

        /** Declare class inside a class */
        ClassInfoWrapper clazz(String name) {
            final FullClassInfo newClassInfo = new FullClassInfo(
                    (FullPackageInfo) getElement().getPackage(), getElement(),
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
            final FullStatementInfo stmt = new FullStatementInfo(getElement(), new ContextSet(),
                    new BasicElementInfo(new FixedSourceRegion(0, 0), index, 1, LanguageConstruct.Builtin.STATEMENT))
            getElement().addStatement(stmt)
            return new StatementInfoWrapper(stmt)
        }

        /** Close class wrapper declared inside a class */
        ClassInfoWrapper endInClass() {
            return new ClassInfoWrapper((FullClassInfo)getElement().getContainingClass())
        }

        /** Close class wrapper declared inside a file */
        FileInfoWrapper endInFile() {
            return new FileInfoWrapper((FullFileInfo)getElement().getContainingFile())
        }

        /** Close class wrapper declared inside a method */
        MethodInfoWrapper endInMethod() {
            return new MethodInfoWrapper((FullMethodInfo)getElement().getContainingMethod())
        }

        protected ClassInfoWrapper getThis() {
            return this
        }

        MethodInfoWrapper method(String name, boolean isTest) {
            final FullMethodInfo method = new FullMethodInfo(getElement(), 0, new ContextSet(),
                    new FixedSourceRegion(0, 0), new MethodSignature(name),
                    isTest, null, false, FullMethodInfo.DEFAULT_METHOD_COMPLEXITY)
            getElement().addMethod(method)
            return new MethodInfoWrapper(method)
        }
    }

    class MethodInfoWrapper extends Wrapper<MethodInfoWrapper, FullMethodInfo> {
        /** Declare a class inside a method */
        ClassInfoWrapper clazz(String name) {
            final FullClassInfo newClassInfo = new FullClassInfo(
                    (FullPackageInfo) getElement().getContainingFile().getContainingPackage(),
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
                    new ContextSet(),
                    new BasicMethodInfo(new FixedSourceRegion(0, 0), 0,
                            FullMethodInfo.DEFAULT_METHOD_COMPLEXITY, new MethodSignature(name),
                            isTest, null, false))
            getElement().addMethod(method)
            return new MethodInfoWrapper(method)
        }

        /** Declare a statement inside a method */
        StatementInfoWrapper stmt(int index) {
            final FullStatementInfo stmt = new FullStatementInfo(getElement(), new ContextSet(),
                    new BasicElementInfo(new FixedSourceRegion(0, 0), index, 1, LanguageConstruct.Builtin.STATEMENT))
            getElement().addStatement(stmt)
            return new StatementInfoWrapper(stmt)
        }

        /** Close method wrapper declared inside a class */
        ClassInfoWrapper endInClass() {
            return new ClassInfoWrapper((FullClassInfo) getElement().getContainingClass())
        }

        /** Close method wrapper declared inside a method */
        MethodInfoWrapper endInMethod() {
            return new MethodInfoWrapper((FullMethodInfo) getElement().getContainingMethod())
        }

        /** Close method wrapper declared inside a file */
        FileInfoWrapper endInFile() {
            return new FileInfoWrapper((FullFileInfo) getElement().getContainingFile())
        }

        protected MethodInfoWrapper(FullMethodInfo element) {
            super(element)
        }

        protected MethodInfoWrapper getThis() {
            return this
        }
    }

    class StatementInfoWrapper extends Wrapper<StatementInfoWrapper, FullStatementInfo> {
        /** Close statement wrapper declared inside a class */
        ClassInfoWrapper endInClass() {
            return new ClassInfoWrapper((FullClassInfo) getElement().getContainingClass())
        }

        /** Close statement wrapper declared inside a method */
        MethodInfoWrapper endInMethod() {
            return new MethodInfoWrapper((FullMethodInfo) getElement().getContainingMethod())
        }

        /** Close statement wrapper declared inside a file */
        FileInfoWrapper endInFile() {
            return new FileInfoWrapper((FullFileInfo) getElement().getContainingFile())
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
