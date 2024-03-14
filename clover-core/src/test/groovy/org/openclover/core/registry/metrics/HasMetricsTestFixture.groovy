package org.openclover.core.registry.metrics

import org.openclover.core.TestUtils
import org.openclover.core.api.registry.ContextSet
import org.openclover.core.api.registry.PackageInfo
import org.openclover.core.api.registry.ProjectInfo
import org.openclover.core.api.registry.SourceInfo
import org.openclover.core.context.ContextSetImpl
import org.openclover.core.context.ContextStore
import org.openclover.core.registry.Clover2Registry
import org.openclover.core.api.registry.CoverageDataProvider
import org.openclover.core.registry.FixedSourceRegion
import org.openclover.core.registry.entities.BasicElementInfo
import org.openclover.core.registry.entities.BasicMethodInfo
import org.openclover.core.registry.entities.FullBranchInfo
import org.openclover.core.registry.entities.FullClassInfo
import org.openclover.core.registry.entities.FullFileInfo
import org.openclover.core.registry.entities.FullMethodInfo
import org.openclover.core.registry.entities.FullPackageInfo
import org.openclover.core.registry.entities.FullProjectInfo
import org.openclover.core.registry.entities.FullStatementInfo
import org.openclover.core.registry.entities.MethodSignature
import org.openclover.core.registry.entities.Modifiers
import org.openclover.core.spi.lang.LanguageConstruct
import org.openclover.runtime.api.registry.CloverRegistryException

class HasMetricsTestFixture {
    ProjectInfo projectInfo
    final FullFileInfo defaultFileInfo
    File tmpDir
    String initStr
    int index
    CoverageDataProvider dataProvider // a chain of mock data providers

    HasMetricsTestFixture(String projectName) throws IOException {
        projectInfo = new FullProjectInfo(projectName)
        defaultFileInfo = newFile(newPackage("testpkg"), "Test.java")
    }

    FullFileInfo getDefaultFileInfo() {
        return defaultFileInfo
    }

    ProjectInfo getProject() {
        return projectInfo
    }

    void setProject(ProjectInfo projectInfo) {
        this.projectInfo = projectInfo
    }

    PackageInfo newPackage(String name) {
        PackageInfo packageInfo = new FullPackageInfo(projectInfo, name, index)
        projectInfo.addPackage(packageInfo)
        return packageInfo
    }

    FullFileInfo newFile(PackageInfo packageInfo, String fileName) throws IOException {
        FullFileInfo fileInfo =
            new FullFileInfo(packageInfo,
                File.createTempFile(fileName, ".java"), "UTF-8", index, 100, 50,
                System.currentTimeMillis(), 10000, 10000, 0)
        packageInfo.addFile(fileInfo)
        
        return fileInfo
    }

    FullClassInfo newClass(String name, int startLine) {
        return newClass(defaultFileInfo, name, startLine)
    }

    FullClassInfo newClass(FullFileInfo fileInfo, String name, int startLine) {
        final SourceInfo srcRegion = new FixedSourceRegion(startLine, 1)
        final FullClassInfo classInfo = new FullClassInfo(fileInfo.getContainingPackage(), fileInfo,
                index, name, srcRegion, new Modifiers(),
                false, false, false)
        fileInfo.addClass(classInfo)
        return classInfo
    }

    FullClassInfo newClass(FullClassInfo parentClass, String name, int startLine) {
        final SourceInfo srcRegion = new FixedSourceRegion(startLine, 1)
        final FullClassInfo classInfo = new FullClassInfo(parentClass.getPackage(), parentClass,
                index, name, srcRegion, new Modifiers(),
                false, false, false)
        parentClass.addClass(classInfo)
        return classInfo
    }

    FullMethodInfo newMethod(FullClassInfo classInfo, String name, int startLine) {
        SourceInfo srcRegion = new FixedSourceRegion(startLine, 1)
        MethodSignature sig = new MethodSignature(name)
        ContextSet ctx = new ContextSetImpl().set(ContextStore.CONTEXT_METHOD)
        FullMethodInfo method = new FullMethodInfo(classInfo, index++, ctx, srcRegion, sig,
                false, null, false, FullMethodInfo.DEFAULT_METHOD_COMPLEXITY)
        classInfo.addMethod(method)
        return method
    }

    FullMethodInfo newMethod(FullFileInfo fileInfo, String name, int startLine) {
        SourceInfo srcRegion = new FixedSourceRegion(startLine, 1)
        MethodSignature sig = new MethodSignature(name)
        ContextSet ctx = new ContextSetImpl().set(ContextStore.CONTEXT_METHOD)
        FullMethodInfo method = new FullMethodInfo(fileInfo, ctx,
                new BasicMethodInfo(srcRegion, index++, FullMethodInfo.DEFAULT_METHOD_COMPLEXITY, sig, false, null, false))
        fileInfo.addMethod(method)
        return method
    }

    /** Create new inner function (like lambda) with complexity 5 */
    FullMethodInfo newMethod(FullMethodInfo methodInfo, String name, int startLine) {
        SourceInfo srcRegion = new FixedSourceRegion(startLine, 1)
        MethodSignature sig = new MethodSignature(name)
        ContextSet ctx = new ContextSetImpl().set(ContextStore.CONTEXT_METHOD)
        FullMethodInfo method = new FullMethodInfo(methodInfo, ctx,
                new BasicMethodInfo(srcRegion, index++, 5, sig, false, null, false, LanguageConstruct.Builtin.METHOD))
        methodInfo.addMethod(method)
        return method
    }


    FullBranchInfo addBranch(FullMethodInfo method, ContextSet ctx, int startLine, int hitCount) {
        FullBranchInfo branch = new FullBranchInfo(method, index, ctx, new FixedSourceRegion(startLine, 4), 2, true)
        newMockCoverageDataProvider(index, hitCount)
        method.setDataProvider(dataProvider)
        method.addBranch(branch)
        index += 2 //Branches have 2 slots
        return branch
    }

    FullStatementInfo addStatement(FullMethodInfo method, int complexity, int startLine, final int hitCount) {
        FixedSourceRegion src = new FixedSourceRegion(startLine, 1)
        FullStatementInfo stmt = new FullStatementInfo(method, index, new ContextSetImpl(), src, complexity)
        newMockCoverageDataProvider(index++, hitCount)
        method.setDataProvider(dataProvider)
        method.addStatement(stmt)
        return stmt
    }

    FullStatementInfo addStatement(FullClassInfo containingClass, int complexity, int startLine, final int hitCount) {
        FixedSourceRegion src = new FixedSourceRegion(startLine, 1)
        FullStatementInfo stmt = new FullStatementInfo(containingClass, new ContextSetImpl(),
                new BasicElementInfo(src, index, complexity, LanguageConstruct.Builtin.STATEMENT))
        newMockCoverageDataProvider(index++, hitCount)
        containingClass.setDataProvider(dataProvider)
        containingClass.addStatement(stmt)
        return stmt
    }

    FullStatementInfo addStatement(FullFileInfo containingFile, int complexity, int startLine, final int hitCount) {
        FixedSourceRegion src = new FixedSourceRegion(startLine, 1)
        FullStatementInfo stmt = new FullStatementInfo(containingFile, new ContextSetImpl(),
                new BasicElementInfo(src, index, complexity, LanguageConstruct.Builtin.STATEMENT))
        newMockCoverageDataProvider(index++, hitCount)
        containingFile.setDataProvider(dataProvider)
        containingFile.addStatement(stmt)
        return stmt
    }

    CoverageDataProvider newMockCoverageDataProvider(int index, int hitCount) {
        dataProvider = new MockCoverageDataProvider(dataProvider, index, hitCount)
        return dataProvider
    }

    private static class MockCoverageDataProvider implements CoverageDataProvider {
        private CoverageDataProvider previous
        private final int startIndex
        private final int hitCount

        MockCoverageDataProvider(CoverageDataProvider previous, int startIndex, int hitCount) {
            this.previous = previous
            this.startIndex = startIndex
            this.hitCount = hitCount
        }

        int getHitCount(int index) {
            if (index >= startIndex) {
                return hitCount
            } else {
                return previous == null ? 0 : previous.getHitCount(index)
            }
        }
    }

    Clover2Registry createSampleRegistry() throws IOException, CloverRegistryException {
        createTmpDir()
        File dbLocation = new File(tmpDir.getAbsolutePath() + "/clover.db")
        return createSampleRegistryAt(dbLocation)
    }

    Clover2Registry createSampleRegistryAt(File dbLocation) throws IOException, CloverRegistryException {

        initStr = dbLocation.getAbsolutePath()
        Clover2Registry reg = new Clover2Registry(dbLocation, "Test")
        reg.saveAndOverwriteFile()
        return reg
    }


    void createTmpDir() throws IOException {
        tmpDir = TestUtils.createEmptyDirFor(getClass())
    }


    File getTmpDir() {
        return tmpDir
    }

    String getInitStr() {
        return initStr
    }
}
