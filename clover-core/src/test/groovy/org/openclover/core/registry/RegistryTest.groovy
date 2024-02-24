package org.openclover.core.registry

import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.openclover.buildutil.testutils.IOHelper
import org.openclover.core.TestUtils
import org.openclover.core.api.registry.ClassInfo
import org.openclover.core.api.registry.ContextSet
import org.openclover.core.api.registry.HasMetrics
import org.openclover.core.api.registry.SourceInfo
import org.openclover.core.context.ContextSetImpl
import org.openclover.core.context.ContextStore
import org.openclover.core.registry.entities.FullBranchInfo
import org.openclover.core.registry.entities.FullClassInfo
import org.openclover.core.registry.entities.FullFileInfo
import org.openclover.core.registry.entities.FullMethodInfo
import org.openclover.core.registry.entities.FullPackageInfo
import org.openclover.core.registry.entities.FullProjectInfo
import org.openclover.core.registry.entities.FullStatementInfo
import org.openclover.core.registry.entities.MethodSignature
import org.openclover.core.registry.entities.Modifiers
import org.openclover.core.registry.entities.PackageFragment
import org.openclover.core.registry.format.FreshRegFile
import org.openclover.core.api.registry.HasMetricsFilter
import org.openclover.core.registry.metrics.ProjectMetrics
import org.openclover.runtime.registry.format.RegAccessMode

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue


class RegistryTest {

    File tmpDir

    @Rule
    public TestName testName = new TestName()

    @Before
    void setUp() throws Exception {
        tmpDir = TestUtils.createEmptyDirFor(getClass(), testName.methodName)
    }

    @After
    void tearDown() throws Exception {
        if (!IOHelper.delete(tmpDir)) {
            throw new RuntimeException("Unable to delete temporary test directory ${tmpDir.absolutePath}".toString())
        }
    }

    @Test
    void testFullHierarchyAdd() throws Exception {
        SourceInfo region = new FixedSourceRegion(0, 0, 0, 0)
        ContextSet context = new ContextSetImpl()
        FullProjectInfo proj = new FullProjectInfo(testName.methodName)
        FullPackageInfo pkg = new FullPackageInfo(proj, "com.acme.test", 0)
        proj.addPackage(pkg)

        FullFileInfo file  = new FullFileInfo(pkg, new File("test.java"), null, 0, 0, 0, 0, 0, 0, 0)
        pkg.addFile(file)

        for (int i = 0; i < 10; i++) {
            FullClassInfo cls = new FullClassInfo(pkg, file, 0, "class$i".toString(),
                    region, new Modifiers(),
                    false, false, false)
            file.addClass(cls)
            for (int j = 0; j < 10; j++) {
                FullMethodInfo method = new FullMethodInfo(cls, 0, context, region,
                        new MethodSignature("method" + j),
                        false, null, false, FullMethodInfo.DEFAULT_METHOD_COMPLEXITY)
                cls.addMethod(method)
                for (int k = 0; k < 10; k++) {
                    FullStatementInfo stmt = new FullStatementInfo(method, 0,  context, region, 0)
                    method.addStatement(stmt)
                }
                for (int k = 0; k < 10; k++) {
                    FullBranchInfo branch = new FullBranchInfo(method, 0,  context, region, 0, true)
                    method.addBranch(branch)
                }
            }
        }

        proj = launderModel(proj)

        ProjectMetrics pm = (ProjectMetrics)proj.getMetrics()

        assertEquals("NumPackages", 1, pm.getNumPackages())
        assertEquals("NumFiles", 1, pm.getNumFiles())
        assertEquals("NumClasses", 10, pm.getNumClasses())
        assertEquals("NumMethods", 100, pm.getNumMethods())
        assertEquals("NumStatements", 1000, pm.getNumStatements())
        assertEquals("NumBranches", 2000, pm.getNumBranches())
    }

    @Test
    void testPackageFragmentation() throws Exception {
        FullProjectInfo proj = new FullProjectInfo(testName.methodName)
        FullPackageInfo pkg1 = new FullPackageInfo(proj, "a.b.c.d", 0)
        pkg1.addFile(new FullFileInfo(pkg1, new File("test.java"), null, 0, 0, 0, 0, 0, 0, 0))
        FullPackageInfo pkg2 = new FullPackageInfo(proj, "a.b.e.f", 0)
        pkg2.addFile(new FullFileInfo(pkg2, new File("test.java"), null, 0, 0, 0, 0, 0, 0, 0))
        FullPackageInfo pkg3 = new FullPackageInfo(proj, "p.q", 0)
        pkg3.addFile(new FullFileInfo(pkg3, new File("test.java"), null, 0, 0, 0, 0, 0, 0, 0))
        FullPackageInfo pkg4 = new FullPackageInfo(proj, "p.r", 0)
        pkg4.addFile(new FullFileInfo(pkg4, new File("test.java"), null, 0, 0, 0, 0, 0, 0, 0))
        FullPackageInfo pkg5 = new FullPackageInfo(proj, "p.s", 0)
        pkg5.addFile(new FullFileInfo(pkg5, new File("test.java"), null, 0, 0, 0, 0, 0, 0, 0))
        FullPackageInfo pkg6 = new FullPackageInfo(proj, "i", 0)
        pkg6.addFile(new FullFileInfo(pkg6, new File("test.java"), null, 0, 0, 0, 0, 0, 0, 0))
        FullPackageInfo pkg7 = new FullPackageInfo(proj, "i.j", 0)
        pkg7.addFile(new FullFileInfo(pkg7, new File("test.java"), null, 0, 0, 0, 0, 0, 0, 0))
        FullPackageInfo pkg8 = new FullPackageInfo(proj, "i.k", 0)
        pkg8.addFile(new FullFileInfo(pkg8, new File("test.java"), null, 0, 0, 0, 0, 0, 0, 0))

        proj.addPackage(pkg1)
        proj.addPackage(pkg2)
        proj.addPackage(pkg3)
        proj.addPackage(pkg4)
        proj.addPackage(pkg5)
        proj.addPackage(pkg6)
        proj.addPackage(pkg7)
        proj.addPackage(pkg8)

        PackageFragment[] roots = launderModel(proj).getPackageRoots()

        assertEquals(3, roots.length)

        assertEquals("a", roots[0].getName())
        assertTrue(!roots[0].isConcrete())

        PackageFragment[] akids = roots[0].getChildren()

        assertEquals(1, akids.length)

        assertEquals("i", roots[1].getName())
        assertTrue(roots[1].isConcrete())
        assertEquals(2, roots[1].getNumChildren())

        assertEquals("p", roots[2].getName())
        assertTrue(!roots[2].isConcrete())
        PackageFragment[] pkids = roots[2].getChildren()
        assertEquals(3, pkids.length)
    }

    @Test
    void testCopy() throws Exception {
        FullProjectInfo proj = new FullProjectInfo(testName.methodName)
        proj.setVersion(System.currentTimeMillis())

        FullPackageInfo pkg = new FullPackageInfo(proj, "a", 0)
        proj.addPackage(pkg)
        SourceInfo region = new FixedSourceRegion(0, 0, 0, 0)
        ContextSet context = new ContextSetImpl()

        FullFileInfo file  = new FullFileInfo(pkg, new File("test.java"), null, 0, 0, 0, 0, 0, 0, 0)
        pkg.addFile(file)
        for (int i = 0; i < 10; i++) {
            FullClassInfo cls = new FullClassInfo(pkg, file, 0, "class$i".toString(),
                    region, new Modifiers(),
                    false, false, false)
            file.addClass(cls)
            for (int j = 0; j < 10; j++) {
                FullMethodInfo method = new FullMethodInfo(cls, 0, context, region,
                        new MethodSignature("method" + j),
                        i < 5, null, false, FullMethodInfo.DEFAULT_METHOD_COMPLEXITY)
                cls.addMethod(method)
                for (int k = 0; k < 10; k++) {
                    FullStatementInfo stmt = new FullStatementInfo(method, 0,  context, region, 0)
                    method.addStatement(stmt)
                }
                for (int k = 0; k < 10; k++) {
                    FullBranchInfo branch = new FullBranchInfo(method, 0,  context, region, 0, true)
                    method.addBranch(branch)
                }
            }
        }

        FullProjectInfo ident = launderModel(proj).copy()

        ProjectMetrics pm = (ProjectMetrics)ident.getMetrics()

        assertEquals("Name", testName.methodName,ident.getName())
        assertEquals("Version", proj.getVersion(), ident.getVersion())
        assertEquals("NumPackages", 1, pm.getNumPackages())
        assertEquals("NumFiles", 1, pm.getNumFiles())
        assertEquals("NumClasses", 10, pm.getNumClasses())
        assertEquals("NumMethods", 100, pm.getNumMethods())
        assertEquals("NumStatements", 1000, pm.getNumStatements())
        assertEquals("NumBranches", 2000, pm.getNumBranches())

        FullProjectInfo excludeTestClasses = launderModel(proj).copy(new HasMetricsFilter() {
            boolean accept(HasMetrics node) {
                if (node instanceof ClassInfo) {
                    ClassInfo info = (ClassInfo)node
                    return !info.isTestClass()
                }
                return true
            }
        })

        pm = (ProjectMetrics)excludeTestClasses.getMetrics()

        assertEquals("Version", proj.getVersion(), excludeTestClasses.getVersion())
        assertEquals("NumPackages", 1, pm.getNumPackages())
        assertEquals("NumFiles", 1, pm.getNumFiles())
        assertEquals("NumClasses", 5, pm.getNumClasses())
        assertEquals("NumMethods", 50, pm.getNumMethods())
        assertEquals("NumStatements", 500, pm.getNumStatements())
        assertEquals("NumBranches", 1000, pm.getNumBranches())
    }


    /**
     * serialize the model to storage, then read back in and return. tests that persistence is correctly implemented
     * @param info model to be laundered
     * @return the same model, having been thru a serialize-deserialize step.
     */
    private FullProjectInfo launderModel(FullProjectInfo info) throws Exception {
        final File regFile = File.createTempFile(testName.methodName, ".db")
        regFile.delete()

        new Clover2Registry(
                new FreshRegFile(regFile, RegAccessMode.READWRITE, testName.methodName),
                info,
                Collections.<Clover2Registry.InstrumentationInfo>emptyList(),
                new ContextStore()).saveAndOverwriteFile()
        return Clover2Registry.fromFile(regFile).getProject()
    }
}
