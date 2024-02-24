package org.openclover.core.registry

import org.junit.BeforeClass
import org.junit.Test
import org.openclover.core.api.registry.ClassInfo
import org.openclover.core.api.registry.MethodInfo
import org.openclover.core.api.registry.PackageInfo
import org.openclover.core.context.ContextStore
import org.openclover.core.registry.entities.FullClassInfo
import org.openclover.core.registry.entities.FullFileInfo
import org.openclover.core.registry.entities.FullMethodInfo
import org.openclover.core.registry.entities.FullProjectInfo
import org.openclover.core.registry.format.FreshRegFile
import org.openclover.runtime.api.CloverException
import org.openclover.runtime.registry.format.RegAccessMode

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNull

/**
 * Tests whether a project containing recursively nested classes, methods, statements is being properly
 * stored in a file format. Test is limited to a single source file (FullFileInfo stored in FileInfoRecord)
 * as all new entities relationships (see CLOV-1336) are limited to a level of the file.
 */
class Clover2RegistryRecursiveTest {

    protected static File registryFile
    protected static FullProjectInfo projectInfo

    /**
     * Test a following model structure (yes, it's more than pure java)
     *
     * <pre>
     *  + Package "com.acme"
     *    + File "File.java"
     *      + Statement "1"                      // statement on a file level, like for scripting languages
     *      + Statement "2"
     *      + Statement "3"
     *
     *      + Method "methodInFile"              // method on a file level, like for non-OO languages
     *        + Statement "10"
     *        + Class  "classInMethodInFile"
     *
     *      + Method "topGoo"                    // methods nested inside method
     *        + Method "topGooGoo"
     *          + Statement "20"
     *          + Method "topGooGooGoo"
     *        + Class "ClassInMethod"            // class nested inside method
     *          + Method "hooHoo"
     *          + Class "ClassInClassMethod"
     *
     *      + Class "classInFile"                // classes on a file level, classic java
     *        + Statement "30"                   // statement on a class level, like for initializer blocks
     *        + Method "methodInClassInFile"     // method in a class, typical java
     *        + Class "classInClassInFile"       // inner class
     *          + Method "methodInInnerClassInFile"
     *            + Class "classInMethodEtc"     // like inline class
     * </pre>
     *
     * @todo add test for BranchInfo
     */
    protected static FullProjectInfo buildSampleProject() {
        final ModelBuilder modelBuilder = new ModelBuilder()

        return modelBuilder
                .proj("Project")
                    .pkg("com.acme")
                        .file("File.java").withId("file")
                            .stmt(1).endInFile()             // statement-in-file
                            .stmt(2).endInFile()
                            .stmt(3).endInFile()

                            .method("methodInFile", false)   // method-in-file
                                .stmt(10).endInMethod()
                                .clazz("classInMethodInFile").endInMethod()
                            .endInFile()

                            .method("topGoo", false)         // method-in-method, class-in-method
                                .method("topGooGoo", false)
                                    .stmt(20).endInMethod()
                                    .method("topGooGooGoo", false).endInMethod()
                                .endInMethod()
                                .clazz("ClassInMethod")
                                    .method("hooHoo").endInClass()
                                    .clazz("ClassInClassMethod").endInClass()
                                .endInMethod()
                            .endInFile()

                            .clazz("classInFile")            // class-in-file, class-in-class, method-in-class
                                .stmt(30).endInClass()
                                .method("methodInClassInFile").endInClass()
                                .clazz("classInClassInFile")
                                    .method("methodInInnerClassInFile")
                                        .clazz("classInMethodEtc").endInMethod()
                                    .endInClass()
                                .endInClass()
                            .endInFile()
                        .end()
                    .end()
                .getElement()
    }

    @BeforeClass
    static void setUp() throws IOException, CloverException {
        registryFile = File.createTempFile("CloverFileInfoRecursive", "db")
        registryFile.deleteOnExit()

        // build our project model and write to registry file
        final Clover2Registry registry = new Clover2Registry(
                new FreshRegFile(registryFile, RegAccessMode.READWRITE, "registry"),
                buildSampleProject(),
                Collections.<Clover2Registry.InstrumentationInfo>emptyList(),
                new ContextStore())
        registry.saveAndOverwriteFile()

        // and now read it back
        projectInfo = Clover2Registry.fromFile(registryFile).getModel().getProject()
    }

    /**
     * Test what can be written directly under FileInfo
     * @throws Exception
     */
    @Test
    void testFileEntities() throws Exception {
        final FullFileInfo fileInfo = (FullFileInfo)projectInfo.findFile("com/acme/File.java")

        assertEquals("File.java", fileInfo.getName())
        assertEquals("com.acme", fileInfo.getContainingPackage().getName())

        // check size of lists, we expect to have top-level entities only
        assertEquals(3, fileInfo.getStatements().size())
        assertEquals(2, fileInfo.getMethods().size())
        assertEquals(1, fileInfo.getClasses().size())

        // check if we have three statements with proper indexes
        assertEquals(1, fileInfo.getStatements().get(0).getDataIndex())
        assertEquals(2, fileInfo.getStatements().get(1).getDataIndex())
        assertEquals(3, fileInfo.getStatements().get(2).getDataIndex())

        // check if we have two methods with proper parent
        assertEquals("methodInFile", fileInfo.getMethods().get(0).getSimpleName())
        assertNull(fileInfo.getMethods().get(0).getContainingClass())
        assertNull(fileInfo.getMethods().get(0).getContainingMethod())
        assertEquals("File.java", fileInfo.getMethods().get(0).getContainingFile().getName())
        assertEquals("com.acme", fileInfo.getMethods().get(0).getContainingFile().getContainingPackage().getName())

        assertEquals("topGoo", fileInfo.getMethods().get(1).getSimpleName())
        assertNull(fileInfo.getMethods().get(1).getContainingClass())
        assertNull(fileInfo.getMethods().get(1).getContainingMethod())
        assertEquals("File.java", fileInfo.getMethods().get(1).getContainingFile().getName())
        assertEquals("com.acme", fileInfo.getMethods().get(1).getContainingFile().getContainingPackage().getName())

        // check if we have our top-level class
        assertEquals("classInFile", fileInfo.getClasses().get(0).getName())
    }

    /**
     * Test what can be written directly under MethodInfo
     * @throws Exception
     */
    @Test
    void testMethodEntities() throws Exception {
        final FullFileInfo fileInfo = (FullFileInfo)projectInfo.findFile("com/acme/File.java")

        // grab first method and check its properties
        final FullMethodInfo methodInfo =(FullMethodInfo)fileInfo.getMethods().get(0)
        assertEquals("methodInFile", methodInfo.getSimpleName())
        assertEquals(" methodInFile()", methodInfo.getSignature().getNormalizedSignature())

        // check what parent we have
        assertNull(methodInfo.getContainingMethod())
        assertNull(methodInfo.getContainingClass())
        assertEquals(fileInfo, methodInfo.getContainingFile())
        assertEquals(fileInfo, methodInfo.getParent())
        assertEquals("com.acme", methodInfo.getContainingFile().getContainingPackage().getName())

        // check what children we have
        assertEquals(1, methodInfo.getStatements().size())
        assertEquals(10, methodInfo.getStatements().get(0).getDataIndex())

        assertEquals(1, methodInfo.getClasses().size())
        assertEquals("classInMethodInFile", methodInfo.getClasses().get(0).getName())
    }


    /**
     * Test what can be written directly under ClassInfo
     * @throws Exception
     */
    @Test
    void testClassEntities() throws Exception {
        final FullFileInfo fileInfo = (FullFileInfo)projectInfo.findFile("com/acme/File.java")

        // grab first method and check its properties
        final ClassInfo classInfo = fileInfo.getClasses().get(0)
        assertEquals("classInFile", classInfo.getName())

        // check what parent we have
        assertNull(classInfo.getContainingMethod())
        assertNull(classInfo.getContainingClass())
        assertEquals(fileInfo, classInfo.getContainingFile())
        assertEquals("com.acme", classInfo.getContainingFile().getContainingPackage().getName())

        // check what children we have
        assertEquals(1, classInfo.getStatements().size())
        assertEquals(30, classInfo.getStatements().get(0).getDataIndex())

        assertEquals(1, classInfo.getMethods().size())
        assertEquals("methodInClassInFile", classInfo.getMethods().get(0).getSimpleName())

        assertEquals(1, classInfo.getClasses().size())
        assertEquals("classInClassInFile", classInfo.getClasses().get(0).getName())
    }


    /**
     * Test nested methods
     * @throws Exception
     */
    @Test
    void testNestedMethods() throws Exception {
        final FullFileInfo fileInfo = (FullFileInfo)projectInfo.findFile("com/acme/File.java")

        // grab second method and check its properties
        final FullMethodInfo methodInfo =(FullMethodInfo)fileInfo.getMethods().get(1)
        assertEquals("topGoo", methodInfo.getSimpleName())

        // look for deeply nested methods
        assertEquals(1, methodInfo.getMethods().size())
        assertEquals("topGooGoo", methodInfo.getMethods().get(0).getSimpleName())

        assertEquals(1, methodInfo.getMethods().get(0).getMethods().size())
        assertEquals("topGooGooGoo", methodInfo.getMethods().get(0).getMethods().get(0).getSimpleName())

        // look for deeply nested classes
        assertEquals(1, methodInfo.getClasses().size())
        assertEquals("ClassInMethod", methodInfo.getClasses().get(0).getName())

        assertEquals(1, methodInfo.getClasses().get(0).getClasses().size())
        assertEquals("ClassInClassMethod", methodInfo.getClasses().get(0).getClasses().get(0).getName())

        // check that for such deeply nested entities parents are correctly set (i.e. that do not point to any lop-level
        // entity, like FileInfo, but to the exact parent)
        final FullMethodInfo methodInfo2ndLevel = (FullMethodInfo) methodInfo.getMethods().get(0)
        final FullMethodInfo methodInfo3rdLevel = (FullMethodInfo) methodInfo2ndLevel.getMethods().get(0)
        assertEquals(methodInfo2ndLevel, methodInfo3rdLevel.getContainingMethod())
        assertNull(methodInfo3rdLevel.getContainingClass())
        assertEquals(fileInfo, methodInfo3rdLevel.getContainingFile())

        final ClassInfo classInfo2ndLevel = methodInfo.getClasses().get(0)
        final ClassInfo classInfo3rdLevel = classInfo2ndLevel.getClasses().get(0)
        assertNull(classInfo3rdLevel.getContainingMethod())
        assertEquals(classInfo2ndLevel, classInfo3rdLevel.getContainingClass())
        assertEquals(fileInfo, methodInfo3rdLevel.getContainingFile())
    }

    /**
     * Test nested classes
     * @throws Exception
     */
    @Test
    void testNestedClasses() throws Exception {
        final FullFileInfo fileInfo = (FullFileInfo)projectInfo.findFile("com/acme/File.java")

        // grab class and check its properties
        final ClassInfo classInfo = fileInfo.getClasses().get(0)
        assertEquals("classInFile", classInfo.getName())

        // look for class-in-class, check the parent
        final ClassInfo classInfo2ndLevel = classInfo.getClasses().get(0)
        assertEquals("classInClassInFile", classInfo2ndLevel.getName())
        assertNull(classInfo2ndLevel.getContainingMethod())
        assertEquals(classInfo, classInfo2ndLevel.getContainingClass())
        assertEquals(fileInfo, classInfo2ndLevel.getContainingFile())

        assertEquals("This class shall have no direct subclasses",
                0, classInfo2ndLevel.getClasses().size())

        // look for class-in-method-in-class, check the parent
        final FullMethodInfo methodInInnerClass = (FullMethodInfo) classInfo.getClasses().get(0).getMethods().get(0)
        final ClassInfo classInfo3rdLevel = methodInInnerClass.getClasses().get(0)
        assertEquals("classInMethodEtc", classInfo3rdLevel.getName())
        assertEquals(methodInInnerClass, classInfo3rdLevel.getContainingMethod())
        assertNull(classInfo3rdLevel.getContainingClass())
        assertEquals(fileInfo, classInfo3rdLevel.getContainingFile())
    }

    @Test
    void testGetAllClasses() {
        final FullFileInfo fileInfo = (FullFileInfo)projectInfo.findFile("com/acme/File.java")

        // package level
        final PackageInfo comAcmePackage = projectInfo.findPackage("com.acme")
        assertNotNull(comAcmePackage)
        final List<ClassInfo> allClassesInPackage = comAcmePackage.getAllClasses()
        assertEquals(6, allClassesInPackage.size())

        // file level
        final List<ClassInfo> allClassesInFile = fileInfo.getAllClasses()
        assertEquals(6, allClassesInFile.size())

        // class level, check "classInFile"
        final ClassInfo classInFile = fileInfo.getClasses().get(0)
        assertEquals("classInFile", classInFile.getName())
        final List<ClassInfo> allClassesInClass = classInFile.getAllClasses()
        assertEquals(2, allClassesInClass.size())
        assertEquals("classInClassInFile", allClassesInClass.get(0).getName())
        assertEquals("classInMethodEtc", allClassesInClass.get(1).getName())

        // method level, check "topGoo"
        final MethodInfo topGoo = fileInfo.getMethods().get(1)
        assertEquals("topGoo", topGoo.getSimpleName())
        final List<ClassInfo> allClassesInMethod = topGoo.getAllClasses()
        assertEquals(2, allClassesInMethod.size())
        assertEquals("ClassInMethod", allClassesInMethod.get(0).getName())
        assertEquals("ClassInClassMethod", allClassesInMethod.get(1).getName())
    }

    @Test
    void testGetAllMethods() {
        final FullFileInfo fileInfo = (FullFileInfo)projectInfo.findFile("com/acme/File.java")

        // file level
        final List<MethodInfo> allMethodsInFile = fileInfo.getAllMethods()
        assertEquals(7, allMethodsInFile.size())

        // class level, check "classInFile"
        final ClassInfo classInFile = fileInfo.getClasses().get(0)
        assertEquals("classInFile", classInFile.getName())
        final List<MethodInfo> allMethodsInClass = classInFile.getAllMethods()
        assertEquals(2, allMethodsInClass.size())
        assertEquals("methodInClassInFile", allMethodsInClass.get(0).getSimpleName())
        assertEquals("methodInInnerClassInFile", allMethodsInClass.get(1).getSimpleName())

        // method level, check "topGoo"
        final MethodInfo topGoo = fileInfo.getMethods().get(1)
        assertEquals("topGoo", topGoo.getSimpleName())
        final List<MethodInfo> allClassesInMethod = topGoo.getAllMethods()
        assertEquals(3, allClassesInMethod.size())
        assertEquals("topGooGoo", allClassesInMethod.get(0).getSimpleName())
        assertEquals("topGooGooGoo", allClassesInMethod.get(1).getSimpleName())
        assertEquals("hooHoo", allClassesInMethod.get(2).getSimpleName())
    }

}
