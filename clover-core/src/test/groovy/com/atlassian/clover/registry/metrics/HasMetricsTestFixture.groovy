package com.atlassian.clover.registry.metrics

import org.openclover.runtime.api.registry.CloverRegistryException
import com.atlassian.clover.api.registry.SourceInfo
import com.atlassian.clover.TestUtils
import com.atlassian.clover.context.ContextSet
import com.atlassian.clover.context.ContextStore
import com.atlassian.clover.registry.Clover2Registry
import com.atlassian.clover.registry.CoverageDataProvider
import com.atlassian.clover.registry.FixedSourceRegion
import com.atlassian.clover.registry.entities.BasicElementInfo
import com.atlassian.clover.registry.entities.BasicMethodInfo
import com.atlassian.clover.registry.entities.FullBranchInfo
import com.atlassian.clover.registry.entities.FullClassInfo
import com.atlassian.clover.registry.entities.FullFileInfo
import com.atlassian.clover.registry.entities.FullMethodInfo
import com.atlassian.clover.registry.entities.FullPackageInfo
import com.atlassian.clover.registry.entities.FullProjectInfo
import com.atlassian.clover.registry.entities.FullStatementInfo
import com.atlassian.clover.registry.entities.MethodSignature
import com.atlassian.clover.registry.entities.Modifiers
import com.atlassian.clover.spi.lang.LanguageConstruct

class HasMetricsTestFixture {
    FullProjectInfo projectInfo
    final FullFileInfo defaultFileInfo
    File tmpDir
    String initStr
    int index
    CoverageDataProvider dataProvider; // a chain of mock data providers

    HasMetricsTestFixture(String projectName) throws IOException {
        projectInfo = new FullProjectInfo(projectName)
        defaultFileInfo = newFile(newPackage("testpkg"), "Test.java")
    }

    FullFileInfo getDefaultFileInfo() {
        return defaultFileInfo
    }

    FullProjectInfo getProject() {
        return projectInfo
    }

    void setProject(FullProjectInfo projectInfo) {
        this.projectInfo = projectInfo
    }

    FullPackageInfo newPackage(String name) {
        FullPackageInfo packageInfo = new FullPackageInfo(projectInfo, name, index)
        projectInfo.addPackage(packageInfo)
        return packageInfo
    }

    FullFileInfo newFile(FullPackageInfo packageInfo, String fileName) throws IOException {
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

    FullClassInfo newClass(FullFileInfo finfo, String name, int startLine) {
        final SourceInfo srcRegion = new FixedSourceRegion(startLine, 1)
        final FullClassInfo classInfo = new FullClassInfo((FullPackageInfo)finfo.getContainingPackage(), finfo,
                index, name, srcRegion, new Modifiers(),
                false, false, false)
        finfo.addClass(classInfo)
        return classInfo
    }

    FullClassInfo newClass(FullClassInfo parentClass, String name, int startLine) {
        final SourceInfo srcRegion = new FixedSourceRegion(startLine, 1)
        final FullClassInfo classInfo = new FullClassInfo((FullPackageInfo) parentClass.getPackage(), parentClass,
                index, name, srcRegion, new Modifiers(),
                false, false, false)
        parentClass.addClass(classInfo)
        return classInfo
    }

    FullMethodInfo newMethod(FullClassInfo classInfo, String name, int startLine) {
        SourceInfo srcRegion = new FixedSourceRegion(startLine, 1)
        MethodSignature sig = new MethodSignature(name)
        ContextSet ctx = new ContextSet().set(ContextStore.CONTEXT_METHOD)
        FullMethodInfo method = new FullMethodInfo(classInfo, index++, ctx, srcRegion, sig,
                false, null, false, FullMethodInfo.DEFAULT_METHOD_COMPLEXITY)
        classInfo.addMethod(method)
        return method
    }

    FullMethodInfo newMethod(FullFileInfo fileInfo, String name, int startLine) {
        SourceInfo srcRegion = new FixedSourceRegion(startLine, 1)
        MethodSignature sig = new MethodSignature(name)
        ContextSet ctx = new ContextSet().set(ContextStore.CONTEXT_METHOD)
        FullMethodInfo method = new FullMethodInfo(fileInfo, ctx,
                new BasicMethodInfo(srcRegion, index++, FullMethodInfo.DEFAULT_METHOD_COMPLEXITY, sig, false, null, false))
        fileInfo.addMethod(method)
        return method
    }

    /** Create new inner function (like lambda) with complexity 5 */
    FullMethodInfo newMethod(FullMethodInfo methodInfo, String name, int startLine) {
        SourceInfo srcRegion = new FixedSourceRegion(startLine, 1)
        MethodSignature sig = new MethodSignature(name)
        ContextSet ctx = new ContextSet().set(ContextStore.CONTEXT_METHOD)
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
        index += 2; //Branches have 2 slots
        return branch
    }

    FullStatementInfo addStatement(FullMethodInfo method, int complexity, int startLine, final int hitCount) {
        FixedSourceRegion src = new FixedSourceRegion(startLine, 1)
        FullStatementInfo stmt = new FullStatementInfo(method, index, new ContextSet(), src, complexity)
        newMockCoverageDataProvider(index++, hitCount)
        method.setDataProvider(dataProvider)
        method.addStatement(stmt)
        return stmt
    }

    FullStatementInfo addStatement(FullClassInfo containingClass, int complexity, int startLine, final int hitCount) {
        FixedSourceRegion src = new FixedSourceRegion(startLine, 1)
        FullStatementInfo stmt = new FullStatementInfo(containingClass, new ContextSet(),
                new BasicElementInfo(src, index, complexity, LanguageConstruct.Builtin.STATEMENT))
        newMockCoverageDataProvider(index++, hitCount)
        containingClass.setDataProvider(dataProvider)
        containingClass.addStatement(stmt)
        return stmt
    }

    FullStatementInfo addStatement(FullFileInfo containingFile, int complexity, int startLine, final int hitCount) {
        FixedSourceRegion src = new FixedSourceRegion(startLine, 1)
        FullStatementInfo stmt = new FullStatementInfo(containingFile, new ContextSet(),
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
