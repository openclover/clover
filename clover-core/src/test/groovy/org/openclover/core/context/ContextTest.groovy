package org.openclover.core.context

import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.openclover.core.api.registry.ClassInfo
import org.openclover.core.api.registry.MethodInfo
import org.openclover.core.api.registry.PackageInfo
import org.openclover.core.api.registry.ProjectInfo
import org.openclover.core.cfg.instr.java.JavaInstrumentationConfig
import org.openclover.core.cfg.instr.java.LambdaInstrumentation
import org.openclover.core.cfg.instr.java.SourceLevel
import org.openclover.core.instr.java.InstrumentationSource
import org.openclover.core.instr.java.Instrumenter
import org.openclover.core.instr.java.StringInstrumentationSource
import org.openclover.core.registry.Clover2Registry
import org.openclover.core.registry.entities.FullMethodInfo
import org.openclover.core.util.FileUtils

import java.util.regex.Pattern

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue
import static org.openclover.core.util.Maps.newHashMap

class ContextTest {

    private File workingDir
    private Clover2Registry registry
    private ContextStore contextStore

    @Rule
    public TestName testName = new TestName()

    @Before
    public void setUp() throws Exception {
        workingDir = File.createTempFile(getClass().getName() + "." + testName.methodName, ".tmp")
        workingDir.delete()
        workingDir.mkdir()

        registry = new Clover2Registry(getCoverageDbFile(), testName.methodName)
        contextStore = registry.getContextStore()
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deltree(workingDir)
    }

    @Test
    void testReservedContexts() throws Exception {

        ProjectInfo model = getModelForInstr("package A; class B { public void notAMatch() {hashcode();} private B() {hashcode();} public int getProperty() {return 42;}}")
        PackageInfo pkgA = model.getNamedPackage("A")

        ClassInfo classB = (ClassInfo)pkgA.getClasses().get(0)
        assertEquals(classB.getName(), "B")

        List<MethodInfo> methods = classB.getMethods()

        assertFalse(methods.get(0).getContext().get(ContextStore.CONTEXT_PROPERTY_ACCESSOR))
        assertFalse(methods.get(0).getContext().get(ContextStore.CONTEXT_PRIVATE_METHOD))
        assertFalse(methods.get(0).getStatements().get(0).getContext().get(ContextStore.CONTEXT_PROPERTY_ACCESSOR))
        assertFalse(methods.get(0).getStatements().get(0).getContext().get(ContextStore.CONTEXT_PRIVATE_METHOD))

        assertFalse(methods.get(1).getContext().get(ContextStore.CONTEXT_PROPERTY_ACCESSOR))
        assertTrue(methods.get(1).getContext().get(ContextStore.CONTEXT_PRIVATE_METHOD))
        assertFalse(methods.get(1).getStatements().get(0).getContext().get(ContextStore.CONTEXT_PROPERTY_ACCESSOR))
        assertTrue(methods.get(1).getStatements().get(0).getContext().get(ContextStore.CONTEXT_PRIVATE_METHOD))

        assertTrue(methods.get(2).getContext().get(ContextStore.CONTEXT_PROPERTY_ACCESSOR))
        assertFalse(methods.get(2).getContext().get(ContextStore.CONTEXT_PRIVATE_METHOD))
        assertTrue(methods.get(2).getStatements().get(0).getContext().get(ContextStore.CONTEXT_PROPERTY_ACCESSOR))
        assertFalse(methods.get(2).getStatements().get(0).getContext().get(ContextStore.CONTEXT_PRIVATE_METHOD))
    }

    @Test
    void testReservedPropertyContext() throws Exception {

        ProjectInfo model = getModelForInstr("""package A; class B
                {
                    public void getFakie(String s) {}
                    public int setFakie() {}
                    public int getProperty() {return 42;}
                    public void setProperty(int val) {}
                    public void notAProperty(int val) {}
                    private String getPrivate() {}
                    public String setPrivate(int notASetter) {}
                }""")
        PackageInfo pkgA = model.getNamedPackage("A")

        ClassInfo classB = (ClassInfo)pkgA.getClasses().get(0)
        assertEquals(classB.getName(), "B")

        List<MethodInfo> methods = classB.getMethods()

        assertFalse(methods.get(0).getContext().get(ContextStore.CONTEXT_PROPERTY_ACCESSOR))
        assertFalse(methods.get(1).getContext().get(ContextStore.CONTEXT_PROPERTY_ACCESSOR))
        assertTrue(methods.get(2).getContext().get(ContextStore.CONTEXT_PROPERTY_ACCESSOR))
        assertTrue(methods.get(3).getContext().get(ContextStore.CONTEXT_PROPERTY_ACCESSOR))
        assertFalse(methods.get(4).getContext().get(ContextStore.CONTEXT_PROPERTY_ACCESSOR))
        assertFalse(methods.get(5).getContext().get(ContextStore.CONTEXT_PROPERTY_ACCESSOR))
    }

    @Test
    void testMethodContexts() throws Exception {

        MethodRegexpContext ctx1 = new MethodRegexpContext("nameMatch", Pattern.compile(".*notComplex.*"))
        MethodRegexpContext ctx2 = new MethodRegexpContext("cmpLE2", Pattern.compile(".*"), 2, Integer.MAX_VALUE)
        MethodRegexpContext ctx3 = new MethodRegexpContext("cmpLE3", Pattern.compile(".*"), 3, Integer.MAX_VALUE)
        MethodRegexpContext ctx4 = new MethodRegexpContext("annotation", Pattern.compile(".*@Test.*"))
        MethodRegexpContext ctx5 = new MethodRegexpContext("twoStatements", Pattern.compile(".*"), Integer.MAX_VALUE, 2)

        int c1 = contextStore.addMethodContext(ctx1)
        int c2 = contextStore.addMethodContext(ctx2)
        int c3 = contextStore.addMethodContext(ctx3)
        int c4 = contextStore.addMethodContext(ctx4)
        int c5 = contextStore.addMethodContext(ctx5)

        Map<String, MethodRegexpContext> hashTest = newHashMap()
        hashTest.put(ctx1.getName(), ctx1)
        hashTest.put(ctx2.getName(), ctx2)
        hashTest.put(ctx3.getName(), ctx3)
        hashTest.put(ctx4.getName(), ctx4)
        hashTest.put(ctx5.getName(), ctx5)

        assertEquals(5, hashTest.size())
        assertEquals(ctx1, hashTest.get(ctx1.getName()))
        assertEquals(ctx2, hashTest.get(ctx2.getName()))
        assertEquals(ctx3, hashTest.get(ctx3.getName()))
        assertEquals(ctx4, hashTest.get(ctx4.getName()))
        assertEquals(ctx5, hashTest.get(ctx5.getName()))

        ProjectInfo model = getModelForInstr(registry, null, """package A; class B {
                public void notComplex() {hashcode();}
                public void complexity1() {hashcode();}
                public void complexity2() {if (hashcode() == 0L) {}}
                public void complexity3() {if (hashcode() == 0L || hashcode() == 1L) {}}
                @Test \n public void methodWithAnnotation() { assertTrue(true); }
                public void statements2() {hashcode(); hashcode();}
                public void statements3() {hashcode(); hashcode(); hashcode();}
                }"""
        )

        PackageInfo pkgA = model.getNamedPackage("A")
        ClassInfo classB = (ClassInfo)pkgA.getClasses().get(0)
        assertEquals(classB.getName(), "B")

        List<MethodInfo> methods = classB.getMethods()

        assertTrue(Pattern.matches(".*notComplex.*","public void notComplex()"))

        assertTrue(methods.get(0).getContext().get(c1))
        assertTrue(!methods.get(1).getContext().get(c1))
        assertTrue(!methods.get(2).getContext().get(c1))
        assertTrue(!methods.get(3).getContext().get(c1))
        assertTrue(!methods.get(4).getContext().get(c1))
        assertTrue(!methods.get(5).getContext().get(c1))
        assertTrue(!methods.get(6).getContext().get(c1))

        assertTrue(methods.get(0).getContext().get(c2))
        assertTrue(methods.get(1).getContext().get(c2))
        assertTrue(methods.get(2).getContext().get(c2))
        assertTrue(!methods.get(3).getContext().get(c2))
        assertTrue(methods.get(4).getContext().get(c2))
        assertTrue(methods.get(5).getContext().get(c2))
        assertTrue(methods.get(6).getContext().get(c2))

        assertTrue(methods.get(0).getContext().get(c3))
        assertTrue(methods.get(1).getContext().get(c3))
        assertTrue(methods.get(2).getContext().get(c3))
        assertTrue(methods.get(3).getContext().get(c3))
        assertTrue(methods.get(4).getContext().get(c3))
        assertTrue(methods.get(5).getContext().get(c3))
        assertTrue(methods.get(6).getContext().get(c3))

        assertTrue(!methods.get(0).getContext().get(c4))
        assertTrue(!methods.get(1).getContext().get(c4))
        assertTrue(!methods.get(2).getContext().get(c4))
        assertTrue(!methods.get(3).getContext().get(c4))
        assertTrue(methods.get(4).getContext().get(c4))
        assertTrue(!methods.get(5).getContext().get(c4))
        assertTrue(!methods.get(6).getContext().get(c4))

        assertTrue(methods.get(0).getContext().get(c5))
        assertTrue(methods.get(1).getContext().get(c5))
        assertTrue(methods.get(2).getContext().get(c5))
        assertTrue(methods.get(3).getContext().get(c5))
        assertTrue(methods.get(4).getContext().get(c5))
        assertTrue(methods.get(5).getContext().get(c5))
        assertTrue(!methods.get(6).getContext().get(c5))
    }

    @Test
    void testCloverOnOff() throws Exception {
        ProjectInfo model = getModelForInstr("/**/ package A; class B { public B() {int p = 0;///CLOVER:OFF\nint i = 0;///CLOVER:ON\nint j = 0;}}")

        PackageInfo pkgA = model.getNamedPackage("A")

        ClassInfo classB = (ClassInfo)pkgA.getClasses().get(0)
        assertEquals(classB.getName(), "B")

        MethodInfo ctor = classB.getMethods().get(0)
        assertEquals(ctor.getName(), "B()")

        assertTrue(!ctor.getStatements().get(0).getContext().get(ContextStore.CONTEXT_CLOVER_OFF))
        assertTrue(ctor.getStatements().get(1).getContext().get(ContextStore.CONTEXT_CLOVER_OFF))
        assertTrue(!ctor.getStatements().get(2).getContext().get(ContextStore.CONTEXT_CLOVER_OFF))
    }

    @Test
    void testCloverVoid() throws Exception {
        JavaInstrumentationConfig cfg = createDefaultInstrConfig(getCoverageDbFile())
        cfg.setSourceLevel(SourceLevel.JAVA_8)
        cfg.setInstrumentLambda(LambdaInstrumentation.ALL)

        ProjectInfo model = getModelForInstr(null, cfg, """/**/ package A; class B { public B() {/*CLOVER:VOID*/
            Function<String, Void> some = (a) -> {/*CLOVER:VOID*/
            Runnable object = () -> {/*CLOVER:VOID*/
                Callable<Object> callable = () -> {
                    final Runnable runnable = () -> /*CLOVER:VOID*/System.out.println("some");
                    return new Object();
                };
            };
            return null;
        };}}""")

        PackageInfo pkgA = model.getNamedPackage("A")
        ClassInfo classB = (ClassInfo)pkgA.classes.get(0)
        FullMethodInfo ctor = classB.methods.get(0)

        def innerMethodClosure
        innerMethodClosure = {List<FullMethodInfo> methods, FullMethodInfo method ->
            methods.add(method)
            method.methods.each { it ->
                innerMethodClosure(methods, it as FullMethodInfo)
            }
        }

        def methods = []

        ctor.methods.each { method ->
            innerMethodClosure(methods, method as FullMethodInfo)
        }

        assertFalse ctor.voidReturnType
        assert methods.size() == 4
        assert methods.findAll { it.lambda }.size() == 4
        assertTrue methods[0].voidReturnType
        assertTrue methods[1].voidReturnType
        assertFalse methods[2].voidReturnType
        assertTrue methods[3].voidReturnType
    }

    @Test
    void testCloverVoidForExpressions() throws Exception {
        JavaInstrumentationConfig cfg = createDefaultInstrConfig(getCoverageDbFile())
        cfg.setSourceLevel(SourceLevel.JAVA_8)
        cfg.setInstrumentLambda(LambdaInstrumentation.ALL)

        ProjectInfo model = getModelForInstr(null, cfg, """/**/ package A; class B { public B() {
        stream.map(e -> e.toUpperCase());
        /*CLOVER:VOID*/
        mapClass.map(() -> /*CLOVER:VOID*/ internalMap.put("some", "some"));
        System.out.println("hello world");
        Effect<String> some = (a) -> /*CLOVER:VOID*/ System.out.println(a.toString());
        };}""")

        PackageInfo pkgA = model.getNamedPackage("A")
        ClassInfo classB = (ClassInfo) pkgA.getClasses().get(0)
        FullMethodInfo ctor = classB.getMethods().get(0)

        def innerMethodClosure
        innerMethodClosure = { List<FullMethodInfo> methods, FullMethodInfo method ->
            methods.add(method)
            method.methods.each { it ->
                innerMethodClosure(methods, it as FullMethodInfo)
            }
        }

        def methods = []

        ctor.methods.each { method ->
            innerMethodClosure(methods, method as FullMethodInfo)
        }

        assertFalse ctor.voidReturnType
        assert methods.size() == 3
        assert methods.findAll { it.lambda }.size() == 3
        assertFalse methods[0].voidReturnType
        assertTrue methods[1].voidReturnType
        assertTrue methods[2].voidReturnType
    }

    private ProjectInfo getModelForInstr(String input) throws Exception {
        return getModelForInstr(null, null, input)
    }

    private JavaInstrumentationConfig createDefaultInstrConfig(File coverageDbFile) {
        final JavaInstrumentationConfig cfg = new JavaInstrumentationConfig()
        cfg.setInitstring(coverageDbFile.getAbsolutePath())
        cfg.setDefaultBaseDir(workingDir)
        cfg.setProjectName(testName.methodName)
        cfg.setSourceLevel(SourceLevel.JAVA_8)
        cfg.setReportInitErrors(false)
        return cfg
    }

    private ProjectInfo getModelForInstr(final Clover2Registry reg, final JavaInstrumentationConfig config, final String input) throws Exception {
        // generate temporary file name for the database
        final File coverageDbFile
        final JavaInstrumentationConfig cfg

        // set configuration
        if (config == null) {
            coverageDbFile = getCoverageDbFile()
            cfg = createDefaultInstrConfig(coverageDbFile)
        } else {
            cfg = config
            coverageDbFile = new File(cfg.getInitString())
        }

        // create instrumenter
        final Instrumenter instr = new Instrumenter(cfg)
        if (reg != null) {
            instr.startInstrumentation(reg)
        } else {
            instr.startInstrumentation()
        }

        // create input source and output sink
        final File tempSourceFile = File.createTempFile(getClass().getName() +"." + testName.methodName, ".tmp", workingDir)
        final StringWriter out = new StringWriter()
        final InstrumentationSource ins = new StringInstrumentationSource(tempSourceFile, input)

        // instrument the source and get the project structure
        instr.instrument(ins, out, null)
        ProjectInfo project = instr.endInstrumentation().getProject()

        // cleanup stuff
        tempSourceFile.delete()
        coverageDbFile.delete()

        return project
    }

    private File getCoverageDbFile() throws IOException {
        File coverageDbFile = File.createTempFile(getClass().getName() +"." + testName.methodName, ".tmp", workingDir)
        coverageDbFile.delete()
        return coverageDbFile
    }


}
