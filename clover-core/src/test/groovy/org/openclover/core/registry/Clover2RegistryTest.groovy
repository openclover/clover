package org.openclover.core.registry

import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.openclover.buildutil.testutils.IOHelper
import org.openclover.core.api.registry.CoverageDataProvider
import org.openclover.core.api.registry.HasMetrics
import org.openclover.core.api.registry.PackageInfo
import org.openclover.core.api.registry.SourceInfo
import org.openclover.core.context.ContextSetImpl
import org.openclover.core.context.ContextStore
import org.openclover.core.instr.InstrumentationSessionImpl
import org.openclover.core.registry.entities.FullFileInfo
import org.openclover.core.registry.entities.FullMethodInfo
import org.openclover.core.registry.entities.FullProjectInfo
import org.openclover.core.registry.entities.MethodSignature
import org.openclover.core.registry.entities.Modifiers
import org.openclover.core.registry.format.InaccessibleRegFileException
import org.openclover.core.registry.format.RegFile
import org.openclover.core.api.registry.HasMetricsFilter
import org.openclover.core.registry.metrics.ProjectMetrics
import org.openclover.core.util.FileUtils
import org.openclover.runtime.api.CloverException
import org.openclover.runtime.api.registry.CloverRegistryException
import org.openclover.runtime.registry.CorruptedRegistryException

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertTrue
import static org.junit.Assert.fail
import static org.openclover.core.util.Lists.newArrayList

public class Clover2RegistryTest {
    private File tmpDir

    @Rule
    public TestName testName = new TestName()

    @Before
    public void setUp() throws Exception {
        tmpDir = IOHelper.createTmpDir(this.getClass().getName())
    }

    @After
    public void tearDown() throws Exception {
        if (!IOHelper.delete(tmpDir)) {
            throw new RuntimeException("Unable to delete temporary test directory ${tmpDir.getAbsolutePath()}".toString())
        }
    }

    @Test
    public void testReadWrite() throws Exception {
        File registryFile = File.createTempFile("registry","cdb", tmpDir)

        prepareDatabaseFile(registryFile)

        Clover2Registry read = Clover2Registry.fromFile(registryFile)

        FullProjectInfo model = read.getProject()

        ProjectMetrics metrics = (ProjectMetrics)model.getMetrics()
        assertEquals("numPackages", 1, metrics.getNumPackages())
        assertEquals("numFiles", 1, metrics.getNumFiles())
        assertEquals("numClasses", 2, metrics.getNumClasses())
        assertEquals("numMethods", 2, metrics.getNumMethods())
        assertEquals("numStatements", 6, metrics.getNumStatements())
        assertEquals("dataIndex", 0, model.getDataIndex())
        assertEquals("dataLength", metrics.getNumMethods() + metrics.getNumStatements() + metrics.getNumBranches(),
                model.getDataLength()) 
    }

    @Test
    public void testFiltering() throws Exception {
        File registryFile = File.createTempFile("registry","cdb", tmpDir)

        Clover2Registry registry = new Clover2Registry(registryFile, testName.methodName)
        SourceInfo region = new FixedSourceRegion(1, 2, 3, 4)
        final InstrumentationSessionImpl session = (InstrumentationSessionImpl) registry.startInstr()

        session.enterFile("", new File("Foo.java"), 10, 20, 30, 40, 50)

        session.enterClass("Foo", region, new Modifiers(), false, false, false)

        session.enterMethod(new ContextSetImpl(), region, new MethodSignature("aMethod"), false)

        session.addStatement(new ContextSetImpl(), region, 0)
        session.addStatement(new ContextSetImpl(), region, 0)
        session.addStatement(new ContextSetImpl(), region, 0)

        session.exitMethod(0,0)

        session.enterClass("Foo.Inner", region, new Modifiers(), false, false, false)

        session.enterMethod(new ContextSetImpl(), region, new MethodSignature("anotherMethod"), false)

        session.addStatement(new ContextSetImpl(), region, 0)
        session.addStatement(new ContextSetImpl(), region, 0)
        session.addStatement(new ContextSetImpl(), region, 0)

        session.exitMethod(0,0)

        session.exitClass(0,0)
        session.exitClass(0,0)
        session.exitFile()
        session.enterFile("", new File("Bar.java"), 10, 20, 30, 40, 50)

        session.enterClass("Bar", region, new Modifiers(), false, false, false)

        session.enterMethod(new ContextSetImpl(), region, new MethodSignature("aMethod"), false)

        session.addStatement(new ContextSetImpl(), region, 0)
        session.addStatement(new ContextSetImpl(), region, 0)
        session.addStatement(new ContextSetImpl(), region, 0)

        session.exitMethod(0,0)
        session.exitClass(0,0)
        session.exitFile()

        session.finishAndApply()

        registry.saveAndOverwriteFile()

        Clover2Registry read = Clover2Registry.fromFile(registryFile)

        FullProjectInfo model = read.getProject()
        ProjectMetrics metrics = (ProjectMetrics)model.getMetrics()
        assertEquals("numPackages", 1, metrics.getNumPackages())
        assertEquals("numFiles", 2, metrics.getNumFiles())
        assertEquals("numClasses", 3, metrics.getNumClasses())
        assertEquals("numMethods", 3, metrics.getNumMethods())
        assertEquals("numStatements", 9, metrics.getNumStatements())
        assertEquals("dataIndex", 0, model.getDataIndex())
        int dataLength =  metrics.getNumMethods() + metrics.getNumStatements() + metrics.getNumBranches()
        assertEquals("dataLength", dataLength, model.getDataLength()) 

        read = Clover2Registry.fromFile(registryFile, new HasMetricsFilter() {
            public boolean accept(HasMetrics hm) {
                 return !(hm instanceof FullFileInfo) || (!hm.getName().startsWith("B"))
            }
        }, null)

        model = read.getProject();                                             metrics = (ProjectMetrics)model.getMetrics()
        assertEquals("numPackages", 1, metrics.getNumPackages())
        assertEquals("numFiles", 1, metrics.getNumFiles())
        assertEquals("numClasses", 2, metrics.getNumClasses())
        assertEquals("numMethods", 2, metrics.getNumMethods())
        assertEquals("numStatements", 6, metrics.getNumStatements())
        assertEquals("dataIndex", 0, model.getDataIndex())
        assertEquals("dataLength", dataLength, model.getDataLength()) 

    }

    private static interface SaveAction {
        public void run(InstrumentationSessionImpl session) throws Exception
    }

    private void testFileInfoChanges(SaveAction saveAction) throws Exception {
        File registryFile = File.createTempFile("registry","cdb", tmpDir)

        Clover2Registry registry = new Clover2Registry(registryFile, testName.methodName)

        InstrumentationSessionImpl session = (InstrumentationSessionImpl) registry.startInstr()
        addFileClassAndMethodInfo(session, "A", "void foo()", 1)
        addFileClassAndMethodInfo(session, "B", "void foo()", 1)
        addFileClassAndMethodInfo(session, "C", "void foo()", 1)
        RegistryUpdate update = session.finishAndApply()

        long v1 = registry.getVersion()

        saveAction.run(session)

        registry = Clover2Registry.fromFile(registryFile)

        assertEquals("registryVersion", v1, registry.getVersion())

        List instrHistory = registry.getInstrHistory()
        assertEquals("has correct num of instr infos", 1,  instrHistory.size())
        Clover2Registry.InstrumentationInfo instr = (Clover2Registry.InstrumentationInfo)instrHistory.get(0)
        assertTrue("has non-negative elapsed time", instr.getEndTS() >= instr.getStartTS())

        PackageInfo pkg = registry.getProject().getDefaultPackage()

        FullFileInfo fa = (FullFileInfo)pkg.getFile("A")
        FullFileInfo fb = (FullFileInfo)pkg.getFile("B")
        FullFileInfo fc = (FullFileInfo)pkg.getFile("C")
        assertTrue("supports v1", fa.supportsVersion(v1))
        assertTrue("supports v1", fb.supportsVersion(v1))
        assertTrue("supports v1", fc.supportsVersion(v1))

        // add a new file
        session = (InstrumentationSessionImpl) registry.startInstr()
        session.enterFile("", new File("D"), 0, 0, 0, 0, 1)
        session.exitFile()
        update = session.finishAndApply()

        long v2 = registry.getVersion()
        assertTrue("reg versions should differ", v1 != v2)
        assertTrue("reg versions should have incremented", v1 < v2)

        saveAction.run(session)

        registry = Clover2Registry.fromFile(registryFile)

        instrHistory = registry.getInstrHistory()
        assertEquals("has incorrect num of instr infos", 2,  instrHistory.size())
        instr = (Clover2Registry.InstrumentationInfo)instrHistory.get(1)
        assertTrue("has non-negative elapsed time", instr.getEndTS() >= instr.getStartTS())

        assertEquals("registryVersion", v2, registry.getVersion())
        pkg = registry.getProject().getDefaultPackage()

        fa = (FullFileInfo)pkg.getFile("A")
        fb = (FullFileInfo)pkg.getFile("B")
        fc = (FullFileInfo)pkg.getFile("C")
        assertTrue("should support v1", fa.supportsVersion(v1))
        assertTrue("should support v2", fa.supportsVersion(v2))
        assertTrue("should support v1", fb.supportsVersion(v1))
        assertTrue("should support v2", fb.supportsVersion(v2))
        assertTrue("should support v1", fc.supportsVersion(v1))
        assertTrue("should support v2", fc.supportsVersion(v2))

        FullFileInfo fd = (FullFileInfo)pkg.getFile("D")
        assertFalse("supports v1", fd.supportsVersion(v1))
        assertTrue("supports v2", fd.supportsVersion(v2))

        // now revisit A,B but same checksum
        session = (InstrumentationSessionImpl) registry.startInstr()
        addFileClassAndMethodInfo(session, "A", "void foo()", 1)
        addFileClassAndMethodInfo(session, "B", "void foo()", 1)
        update = session.finishAndApply()

        long v3 = registry.getVersion()
        assertTrue("versions differ", v1 != v3)
        assertTrue("versions differ", v2 != v3)
        saveAction.run(session)

        assertEquals("checksum same", fa.getChecksum(), pkg.getFile("A").getChecksum())
        assertEquals("data index same", fa.getDataIndex(), ((FullFileInfo)pkg.getFile("A")).getDataIndex())
        assertEquals("data length same", fa.getDataLength(), ((FullFileInfo)pkg.getFile("A")).getDataLength())

        assertEquals("checksum same", fb.getChecksum(), pkg.getFile("B").getChecksum())
        assertEquals("data index same", fb.getDataIndex(), ((FullFileInfo)pkg.getFile("B")).getDataIndex())
        assertEquals("data length same", fb.getDataLength(), ((FullFileInfo)pkg.getFile("B")).getDataLength())

        registry = Clover2Registry.fromFile(registryFile)

        instrHistory = registry.getInstrHistory()
        assertEquals("has correct num of instr infos", 3,  instrHistory.size())
        instr = (Clover2Registry.InstrumentationInfo)instrHistory.get(2)
        assertTrue("has non-negative elapsed time", instr.getEndTS() >= instr.getStartTS())

        assertEquals("registryVersion", v3, registry.getVersion())
        pkg = registry.getProject().getDefaultPackage()

        assertEquals("checksum same", fa.getChecksum(), pkg.getFile("A").getChecksum())
        assertEquals("data index same", fa.getDataIndex(), ((FullFileInfo)pkg.getFile("A")).getDataIndex())
        assertEquals("data length same", fa.getDataLength(), ((FullFileInfo)pkg.getFile("A")).getDataLength())

        assertEquals("checksum same", fb.getChecksum(), pkg.getFile("B").getChecksum())
        assertEquals("data index same", fb.getDataIndex(), ((FullFileInfo)pkg.getFile("B")).getDataIndex())
        assertEquals("data length same", fb.getDataLength(), ((FullFileInfo)pkg.getFile("B")).getDataLength())

        fa = (FullFileInfo)pkg.getFile("A")
        fb = (FullFileInfo)pkg.getFile("B")
        fc = (FullFileInfo)pkg.getFile("C")
        fd = (FullFileInfo)pkg.getFile("D")
        assertTrue("supports v1", fa.supportsVersion(v1))
        assertTrue("supports v2", fa.supportsVersion(v2))
        assertTrue("supports v3", fa.supportsVersion(v3))
        assertTrue("supports v1", fb.supportsVersion(v1))
        assertTrue("supports v2", fb.supportsVersion(v2))
        assertTrue("supports v3", fb.supportsVersion(v3))
        assertTrue("supports v1", fc.supportsVersion(v1))
        assertTrue("supports v2", fc.supportsVersion(v2))
        assertTrue("supports v3", fc.supportsVersion(v3))
        assertTrue("supports v1", !fd.supportsVersion(v1))
        assertTrue("supports v2", fd.supportsVersion(v2))
        assertTrue("supports v3", fd.supportsVersion(v3))

        // now revisit A, but with different checksum
        session = (InstrumentationSessionImpl) registry.startInstr()
        addFileClassAndMethodInfo(session, "A", "void foo()", 2)
        update = session.finishAndApply()
        saveAction.run(session)

        long v4 = registry.getVersion()
        assertTrue("versions differ", v1 != v4)
        assertTrue("versions differ", v2 != v4)
        assertTrue("versions differ", v3 != v4)

        assertFalse("checksum same", fa.getChecksum() == pkg.getFile("A").getChecksum())
        assertFalse("data index same", fa.getDataIndex() == ((FullFileInfo)pkg.getFile("A")).getDataIndex())
        assertEquals("data length differs", fa.getDataLength(), ((FullFileInfo)pkg.getFile("A")).getDataLength())

        registry = Clover2Registry.fromFile(registryFile)

        instrHistory = registry.getInstrHistory()
        assertEquals("has correct num of instr infos", 4,  instrHistory.size())
        instr = (Clover2Registry.InstrumentationInfo)instrHistory.get(3)
        assertTrue("has non-negative elapsed time", instr.getEndTS() >= instr.getStartTS())

        assertEquals("registryVersion", v4, registry.getVersion())
        pkg = registry.getProject().getDefaultPackage()

        assertFalse("checksum same", fa.getChecksum() == pkg.getFile("A").getChecksum())
        assertFalse("data index has not progressed", fa.getDataIndex() == ((FullFileInfo)pkg.getFile("A")).getDataIndex())
        assertEquals("data length differs", fa.getDataLength(), ((FullFileInfo)pkg.getFile("A")).getDataLength())

        fa = (FullFileInfo)pkg.getFile("A")
        fb = (FullFileInfo)pkg.getFile("B")
        fc = (FullFileInfo)pkg.getFile("C")
        fd = (FullFileInfo)pkg.getFile("D")
        assertFalse("supports v1", fa.supportsVersion(v1))
        assertFalse("supports v2", fa.supportsVersion(v2))
        assertFalse("supports v3", fa.supportsVersion(v3))
        assertTrue("supports v4", fa.supportsVersion(v4))
        assertTrue("supports v1", fb.supportsVersion(v1))
        assertTrue("supports v2", fb.supportsVersion(v2))
        assertTrue("supports v3", fb.supportsVersion(v3))
        assertTrue("supports v4", fb.supportsVersion(v4))
        assertTrue("supports v1", fc.supportsVersion(v1))
        assertTrue("supports v2", fc.supportsVersion(v2))
        assertTrue("supports v3", fc.supportsVersion(v3))
        assertTrue("supports v4", fc.supportsVersion(v4))
        assertFalse("supports v1", fd.supportsVersion(v1))
        assertTrue("supports v2", fd.supportsVersion(v2))
        assertTrue("supports v3", fd.supportsVersion(v3))
        assertTrue("supports v4", fd.supportsVersion(v4))
    }

    @Test
    public void testFileInfoChangesOnRewrite() throws Exception {
        testFileInfoChanges(new SaveAction() {
            public void run(InstrumentationSessionImpl session) throws Exception {
                session.getRegistry().saveAndOverwriteFile()
            }
        })
    }

    @Test
    public void testFileInfoChangesOnAppend() throws Exception {
        testFileInfoChanges(new SaveAction() {
            public void run(InstrumentationSessionImpl session) throws Exception {
                session.getRegistry().saveAndAppendToFile()
            }
        })
    }

    private void addFileClassAndMethodInfo(InstrumentationSessionImpl session, String className, String methodSig, long checksum) {
        session.enterFile("", new File(className), 0, 0, 0, 0, checksum)
        session.enterClass(className, new FixedSourceRegion(0, 0), new Modifiers(), false, false, false)
        session.enterMethod(new ContextSetImpl(), new FixedSourceRegion(0, 0), new MethodSignature(methodSig), false, FullMethodInfo.DEFAULT_METHOD_COMPLEXITY)
        session.exitMethod(0, 0)
        session.exitClass(0, 0)
        session.exitFile()
    }

    @Test
    public void testOutOfDate() throws Exception {
        File registryFile = File.createTempFile("registry","cdb", tmpDir)

        Clover2Registry registry1 = new Clover2Registry(registryFile, testName.methodName)

        InstrumentationSessionImpl session1 = (InstrumentationSessionImpl) registry1.startInstr()
        session1.enterFile("", new File("A"), 0, 0, 0, 0, 1)
        session1.exitFile()
        session1.finishAndApply()

        registry1.saveAndOverwriteFile()

        //Ensure subsequence write to registry is at least 1 second
        //after first write to compensite for OSX's course timestamp
        //granularity
        try {
            Thread.sleep((long)(FileUtils.getInstance().getPlatformFSResolution() * 1.5))
        } catch (InterruptedException e) {
            //Ignore
        }

        Clover2Registry registry2 = new Clover2Registry(registryFile, testName.methodName)

        InstrumentationSessionImpl session2 = (InstrumentationSessionImpl) registry2.startInstr()
        session2.enterFile("", new File("B"), 0, 0, 0, 0, 1)
        session2.exitFile()
        session2.finishAndApply()

        registry2.saveAndOverwriteFile()

        assertTrue(registry1.isOutOfDate())

    }

    @Test
    public void testReadErrors() throws Exception {
        // invalid header
        File notReg = File.createTempFile("notregistry","cdb", tmpDir)

        FileOutputStream out = new FileOutputStream(notReg)

        out.write([1, 2, 3, 4, 5, 6, 7, 8, 9, 0] as byte[])
        out.close()

        try {
            Clover2Registry registry = Clover2Registry.fromFile(notReg)
            fail("read invalid registry")
        }
        catch (CorruptedRegistryException e) {
        }

        // corrupted header with correct magic and version

        File registryFile = File.createTempFile("registry","cdb", tmpDir)
        Clover2Registry registry = new Clover2Registry(registryFile, testName.methodName)

        final InstrumentationSessionImpl session = (InstrumentationSessionImpl) registry.startInstr()
        session.enterFile("", new File("A"), 0, 0, 0, 0, 1)
        session.exitFile()
        session.finishAndApply()
        registry.saveAndOverwriteFile()

        RandomAccessFile raf = new RandomAccessFile(registryFile, "rw")
        raf.skipBytes(12)
        raf.write([1, 2, 3, 4, 5, 6, 7, 8, 9, 0] as byte[])
        raf.close()

        try {
            registry = Clover2Registry.fromFile(registryFile)
            fail("read invalid registry")
        }
        catch (CorruptedRegistryException e) {
        }

    }

    @Test
    public void testNoHalfBakedRegistryElementsDuringInstr() throws Exception {
        File registryFile = File.createTempFile("registry","cdb", tmpDir)

        Clover2Registry registry = new Clover2Registry(registryFile, testName.methodName)

        final InstrumentationSessionImpl session = (InstrumentationSessionImpl) registry.startInstr()
        session.enterFile("", new File("A"), 0, 0, 0, 0, 1)
        session.exitFile()
        session.enterFile("", new File("B"), 0, 0, 0, 0, 1)
        session.exitFile()

        assertNull("default package should not be present until after endInstr()", registry.getProject().getDefaultPackage())

        session.enterFile("com.foo", new File("C"), 0, 0, 0, 0, 1)
        session.exitFile()

        assertNull("package com.foo should not be present until after endInstr()", registry.getProject().getDefaultPackage())

        session.finishAndApply()

        assertNotNull("default package should be present after endInstr()", registry.getProject().getDefaultPackage())
        assertNotNull("file A should be present after instrumentation finished", registry.getProject().getDefaultPackage().getFileInPackage("A"))
        assertNotNull("file B should be present after instrumentation finished", registry.getProject().getDefaultPackage().getFileInPackage("B"))
        assertNotNull("com.foo package should be present after instrumentation finished", registry.getProject().getNamedPackage("com.foo"))
        assertNotNull("file com.foo.C should be present after instrumentation finished", registry.getProject().getNamedPackage("com.foo").getFileInPackage("C"))
    }

    static class MockableRegistry extends Clover2Registry {
        public MockableRegistry(File registryFile, String name) throws InaccessibleRegFileException {
            super(registryFile, name)
        }

        public RegFile saveAndOverwriteFile() throws IOException, CloverRegistryException {
            final FullProjectInfo emptyProject = new FullProjectInfo(getProject().getName())
            emptyProject.setDataLength(getDataLength())
            return saveAndOverwriteFile(emptyProject, newArrayList(), new ContextStore(), null)
        }
    }

    @Test
    public void testStoringAsMockRegistry() throws Exception {
        File registryFile = File.createTempFile("registry","cdb", tmpDir)
        MockableRegistry registry = new MockableRegistry(registryFile, testName.methodName)
        SourceInfo region = new FixedSourceRegion(1, 2, 3, 4)
        final InstrumentationSessionImpl session = (InstrumentationSessionImpl) registry.startInstr()
        FullFileInfo foo = session.enterFile("", new File("Foo.java"), 10, 20, 30, 40, 50)
        session.enterClass("Foo", region, new Modifiers(), false, false, false)
        session.enterMethod(new ContextSetImpl(), region, new MethodSignature("aMethod"), false)
        session.addStatement(new ContextSetImpl(), region, 0)
        session.addStatement(new ContextSetImpl(), region, 0)
        session.addStatement(new ContextSetImpl(), region, 0)
        session.exitMethod(0,0)
        session.enterClass("Foo.Inner", region, new Modifiers(), false, false, false)
        session.enterMethod(new ContextSetImpl(), region, new MethodSignature("anotherMethod"), false)
        session.addStatement(new ContextSetImpl(), region, 0)
        session.addStatement(new ContextSetImpl(), region, 0)
        session.addStatement(new ContextSetImpl(), region, 0)
        session.exitMethod(0,0)
        session.exitClass(0,0)
        session.exitClass(0,0)
        session.exitFile()
        session.finishAndApply()

        registry.saveAndOverwriteFile()

        Clover2Registry read = Clover2Registry.fromFile(registryFile)

        assertEquals("Mock registry should have same data length as in-memory registry", registry.getDataLength(), read.getDataLength())
        assertEquals("Mock registry's project should have same data index as in-memory registry's project", registry.getProject().getDataIndex(), read.getProject().getDataIndex())
        assertEquals("Mock registry's project should have same data length as in-memory registry's project", registry.getProject().getDataLength(), read.getProject().getDataLength())
    }

    @Test
    public void testCanCheckRegistryFileDoesntExist() throws Exception {
        File tempDir = FileUtils.createTempDir("registry")
        File registryFile = new File("registry.db")

        Clover2Registry registry = new Clover2Registry(registryFile, testName.methodName)

        assertFalse("Registry should detect when underlying file doesn't exist", registry.fileExists())
    }

    @Test
    public void testLazyLoadingRegistryExceptionTranslation() throws Exception {
        final File registryFile = File.createTempFile("registry","cdb", tmpDir)

        prepareDatabaseFile(registryFile)

        final RandomAccessFile raf = new RandomAccessFile(registryFile, "rw")
        raf.seek(raf.length() - 1)
        raf.write(0)
        raf.close()

        try {
            Clover2Registry.fromFile(registryFile)
            fail("Exception expected as the file is deliberately broken")
        } catch (CorruptedRegistryException ex) {
        }
    }

    @Test
    public void testBufferUnderflowExceptionTranslation() throws Exception {
        File registryFile = File.createTempFile("registry","cdb", tmpDir)

        prepareDatabaseFile(registryFile)

        final RandomAccessFile raf = new RandomAccessFile(registryFile, "rw")
        raf.setLength(raf.length() -1)
        raf.close()

        try {
            Clover2Registry.fromFile(registryFile)
            fail("Exception expected as the file is deliberately broken")
        } catch (CorruptedRegistryException ex) {
        }
    }

    @Test
    public void testCopyForBackgroundCoverageLoad() throws IOException, CloverException {
        final File registryFile = File.createTempFile("registry","cdb", tmpDir)
        Clover2Registry reg1 = prepareDatabaseFile(registryFile)
        reg1.getProject().setDataProvider(new CoverageDataProvider() {
            public int getHitCount(int index) {
                return 0
            }
        })

        Clover2Registry reg2 = reg1.copyForBackgroundCoverageLoad()
        reg2.getProject().setDataProvider(new CoverageDataProvider() {
            public int getHitCount(int index) {
                return 1
            }
        })

        final long numElements = reg1.getProject().getMetrics().getNumElements()
        assertFalse("There should be some elemenets in the test registry", 0 == numElements)
        assertEquals("Coverage for orig registry should be 0, numCoveredElements", 0, reg1.getProject().getMetrics().getNumCoveredElements())

        assertEquals("Copied registry should contain the same structure, numElements", numElements, reg2.getProject().getMetrics().getNumElements())
        assertEquals("Copied registry should have 100% coverage, numCoveredElements", numElements, reg2.getProject().getMetrics().getNumCoveredElements())

    }

    private Clover2Registry prepareDatabaseFile(File registryFile) throws CloverException, IOException {
        Clover2Registry registry = new Clover2Registry(registryFile, testName.methodName)

        SourceInfo region = new FixedSourceRegion(1, 2, 3, 4)
        final InstrumentationSessionImpl session = (InstrumentationSessionImpl) registry.startInstr()

        session.enterFile("", new File("Foo.java"), 10, 20, 30, 40, 50)

        session.enterClass("Foo", region, new Modifiers(), false, false, false)

        session.enterMethod(new ContextSetImpl(), region, new MethodSignature("aMethod"), false)

        session.addStatement(new ContextSetImpl(), region, 0)
        session.addStatement(new ContextSetImpl(), region, 0)
        session.addStatement(new ContextSetImpl(), region, 0)

        session.exitMethod(0,0)

        session.enterClass("Foo.Inner", region, new Modifiers(), false, false, false)

        session.enterMethod(new ContextSetImpl(), region, new MethodSignature("anotherMethod"), false)

        session.addStatement(new ContextSetImpl(), region, 0)
        session.addStatement(new ContextSetImpl(), region, 0)
        session.addStatement(new ContextSetImpl(), region, 0)

        session.exitMethod(0,0)

        session.exitClass(0,0)
        session.exitClass(0,0)
        session.exitFile()


        session.finishAndApply()

        registry.saveAndOverwriteFile()

        return registry
    }
}
