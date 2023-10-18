package com.atlassian.clover.instr.java

import com.atlassian.clover.api.CloverException
import com.atlassian.clover.api.registry.ClassInfo
import com.atlassian.clover.api.registry.StatementInfo
import com.atlassian.clover.cfg.instr.InstrumentationConfig
import com.atlassian.clover.cfg.instr.InstrumentationLevel
import com.atlassian.clover.CloverNames
import com.atlassian.clover.cfg.instr.java.JavaInstrumentationConfig
import com.atlassian.clover.cfg.instr.java.LambdaInstrumentation
import com.atlassian.clover.cfg.instr.java.SourceLevel
import com.atlassian.clover.registry.Clover2Registry
import com.atlassian.clover.registry.FixedSourceRegion
import com.atlassian.clover.registry.entities.FullMethodInfo
import com.atlassian.clover.registry.entities.FullProjectInfo
import com.atlassian.clover.registry.metrics.ProjectMetrics
import com.atlassian.clover.util.FileUtils
import com_atlassian_clover.CloverVersionInfo
import com_atlassian_clover.CoverageRecorder
import com_atlassian_clover.TestNameSniffer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName

import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.MatcherAssert.assertThat
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue
import static org.junit.Assert.fail

public class InstrumentationTest {
    private File workingDir

    private String snifferField = "public static final " + TestNameSniffer.class.getName() + " SNIFFER=" +
            TestNameSniffer.class.getName() + ".NULL_INSTANCE;"

    @Rule
    public TestName name = new TestName()

    @Before
    public void setUp()
            throws Exception {
        workingDir = File.createTempFile(getClass().getName() + "." + name, ".tmp")
        workingDir.delete()
        workingDir.mkdir()
    }

    @After
    public void tearDown()
            throws Exception {
        FileUtils.deltree(workingDir)
    }

    @Test
    public void testAssertStatements()
            throws Exception {
        checkStatement(
            " assert arg > 0;",
            " assert (((arg > 0)&&(RECORDER.iget(1)!=0|true))||(RECORDER.iget(2)==0&false));")
        checkStatement(
            " assert arg > 0 : \"arg must be greater than zero\";",
            " assert (((arg > 0 )&&(RECORDER.iget(1)!=0|true))||(RECORDER.iget(2)==0&false)): \"arg must be greater than zero\";")
    }

    @Test
    public void testMethodEntries()
            throws Exception {
        checkInstrumentation([
            ["class B { B(int arg) {}}",
             "class B {$snifferField B(int arg) {RECORDER.inc(0);}}"],
            ["class B { public B(int arg) {}}",
             "class B {$snifferField public B(int arg) {RECORDER.inc(0);}}"],
            ["class B { protected B(int arg) {}}",
             "class B {$snifferField protected B(int arg) {RECORDER.inc(0);}}"],
            ["class B { private B(int arg) {}}",
             "class B {$snifferField private B(int arg) {RECORDER.inc(0);}}"],
            ["class B { void a(int arg) { }}",
             "class B {$snifferField void a(int arg) {RECORDER.inc(0); }}"],
            ["class B { public void a(int arg) { }}",
             "class B {$snifferField public void a(int arg) {RECORDER.inc(0); }}"],
            ["class B { protected void a(int arg) { }}",
             "class B {$snifferField protected void a(int arg) {RECORDER.inc(0); }}"],
            ["class B { private void a(int arg) { }}",
             "class B {$snifferField private void a(int arg) {RECORDER.inc(0); }}"],
            ["class B { private void a(int arg) {}\nprivate void b() {}\nprivate void c() {}}",
             "class B {$snifferField private void a(int arg) {RECORDER.inc(0);}\nprivate" +
                " void b() {RECORDER.inc(1);}\nprivate void c() {RECORDER.inc(2);}}"]
        ] as String[][])
    }

    @Test
    public void testStatements() throws Exception {
        checkInstrumentation([
                [ "class B { private void a(int arg) {hashCode();}}",
                  "class B {$snifferField private void a(int arg) {RECORDER.inc(0);RECORDER.inc(1);hashCode();}}" ]
        ] as String[][])
    }

    @Test
    public void testTryResourceStatements() throws Exception {
        checkInstrumentation([
                [ "class B { private void a(int arg) { try(A a = new A()) { } catch(Exception e) { } finally{ } }}",
                  "class B {$snifferField " + 'private void a(int arg) {RECORDER.inc(0); class RECORDER$AC0 implements java.lang.AutoCloseable {public void close(){}}; RECORDER.inc(1);try(RECORDER$AC0 CLR$ACI0=new RECORDER$AC0(){{RECORDER.inc(2);}};A a = new A()) { } catch(Exception e) { } finally{ } }}']
        ] as String[][])
        checkInstrumentation([
                [ "class B { private void a(int arg) { try(A a = new A();) { } catch(Exception e) { } finally{ } }}",
                  "class B {$snifferField " + 'private void a(int arg) {RECORDER.inc(0); class RECORDER$AC0 implements java.lang.AutoCloseable {public void close(){}}; RECORDER.inc(1);try(RECORDER$AC0 CLR$ACI0=new RECORDER$AC0(){{RECORDER.inc(2);}};A a = new A();) { } catch(Exception e) { } finally{ } }}']
        ] as String[][])
        checkInstrumentation([
                [ "class B { private void a(int arg) { try(A a = new A(); B b = new B()) { } catch(Exception e) { } finally{ } }}",
                  "class B {$snifferField " + 'private void a(int arg) {RECORDER.inc(0); class RECORDER$AC0 implements java.lang.AutoCloseable {public void close(){}}; RECORDER.inc(1);try(RECORDER$AC0 CLR$ACI0=new RECORDER$AC0(){{RECORDER.inc(2);}};A a = new A(); RECORDER$AC0 CLR$ACI1=new RECORDER$AC0(){{RECORDER.inc(3);}};B b = new B()) { } catch(Exception e) { } finally{ } }}']
        ] as String[][])
        checkInstrumentation([
                [ "class B { private void a(int arg) { try(A a = new A(); B b = new B();) { } catch(Exception e) { } finally{ } }}",
                  "class B {$snifferField " + 'private void a(int arg) {RECORDER.inc(0); class RECORDER$AC0 implements java.lang.AutoCloseable {public void close(){}}; RECORDER.inc(1);try(RECORDER$AC0 CLR$ACI0=new RECORDER$AC0(){{RECORDER.inc(2);}};A a = new A(); RECORDER$AC0 CLR$ACI1=new RECORDER$AC0(){{RECORDER.inc(3);}};B b = new B();) { } catch(Exception e) { } finally{ } }}']
        ] as String[][])
    }

    @Test
    public void testMultiCatchBlocks() throws Exception {
        checkInstrumentation([
                [ "class B { private void a(int arg) { try { } catch(FooException|BarException e) { } }}",
                  "class B {$snifferField private void a(int arg) {RECORDER.inc(0); RECORDER.inc(1);try { } catch(FooException|BarException e) { } }}"]
        ] as String[][])
        checkInstrumentation([
                [ "class B { private void a(int arg) { try { } catch(final FooException|BarException|BazException e) { } }}",
                  "class B {$snifferField private void a(int arg) {RECORDER.inc(0); RECORDER.inc(1);try { } catch(final FooException|BarException|BazException e) { } }}"]
        ] as String[][])
    }

    @Test
    public void testDiamondTypeArgs() throws Exception {
        checkInstrumentation([
                [ "class B { private void a(int arg) { List<String> l = new ArrayList<>(); new Foo<>(); }}",
                  "class B {$snifferField private void a(int arg) {RECORDER.inc(0); RECORDER.inc(1);List<String> l = new ArrayList<>(); RECORDER.inc(2);new Foo<>(); }}"]
        ] as String[][])
    }

    @Test
    public void testNumericLiterals() throws Exception {
        //More an test of language recognition than an instrumentation test
        //Tests int, long, float, doubles in hex, binary, decimal, octal, some with underscores, some without
        checkInstrumentation([
                [ "class B { static double[] doubles = new double[] { 09, 0_9, 0_0, 0x1_2_3, 1234_5678, 1_2_3_4__5_6_7_8L, 0b0001_0010_0100_1000, 3.141_592_653_589_793d, 0x1.ffff_ffff_ffff_fP1_023, 0x1111_2222_3333_4444L, 0x0.0000000000001P-1f, 0x0.0000000000001P-1_1d }; }",
                  "class B { static double[] doubles = new double[] { 09, 0_9, 0_0, 0x1_2_3, 1234_5678, 1_2_3_4__5_6_7_8L, 0b0001_0010_0100_1000, 3.141_592_653_589_793d, 0x1.ffff_ffff_ffff_fP1_023, 0x1111_2222_3333_4444L, 0x0.0000000000001P-1f, 0x0.0000000000001P-1_1d }; }"]
        ] as String[][])
    }

    @Test
    public void testBinaryLiterals() throws Exception {
        //More an test of language recognition than an instrumentation test
        //Test binary literals (int, long with underscores)
        checkInstrumentation([
                [ "class B { static double[] doubles = new double[] { 0b0, 0b1, 0b10101010, 0b1010_1010, 0b1010_1010L, 0b10101010L }; }",
                  "class B { static double[] doubles = new double[] { 0b0, 0b1, 0b10101010, 0b1010_1010, 0b1010_1010L, 0b10101010L }; }"]
        ] as String[][])
    }

    /**
     * Test for CLOV-669
     */
    @Test
    public void testUnicodeCharacters()
            throws Exception {

        String instr = getInstrumentedVersion("class B { private void a(int arg) {String s = \"\u023a\";}}", false)
        assertNotNull(instr)
    }

    @Test
    public void testTernaryOperator()
            throws Exception {
        // just a simple assignment
        checkStatement("int i = arg == 2 ? 3 : 4;",
             "RECORDER.inc(1);int i = (((arg == 2 )&&(RECORDER.iget(2)!=0|true))||(RECORDER.iget(3)==0&false))? 3 : 4;")

        // two ternary's embeded
        checkStatement("int i = arg == (b==2?1:2) ? 3 : 4;",
             "RECORDER.inc(1);int i = (((arg == (" +
             "(((b==2)&&(RECORDER.iget(2)!=0|true))||(RECORDER.iget(3)==0&false))?1:2" +
             ") )&&(RECORDER.iget(4)!=0|true))||(RECORDER.iget(5)==0&false))? 3 : 4;")
    }

    @Test
    public void testSwitchStatement() throws Exception {
        String bp = "__CLB" + CloverVersionInfo.SANITIZED_RN
        String src = "{int i = 0;switch(i){case 0:break;case 1:case 2:break;case 3:hashCode();break;}}}"
        String instr = "{RECORDER.inc(0);RECORDER.inc(1);int i = 0;boolean "+bp+"_bool0=false;RECORDER.inc(2);switch(i){case 0:if (!"+bp+"_bool0)" +
                " {RECORDER.inc(3);"+bp+"_bool0=true;}RECORDER.inc(4);break;case 1:if (!"+bp+"_bool0) {RECORDER.inc(5);"+bp+"_bool0=true;}" +
                "case 2:if (!"+bp+"_bool0) {RECORDER.inc(6);"+bp+"_bool0=true;}RECORDER.inc(7);break;case 3:if (!"+bp+"_bool0) {RECORDER.inc(8);"+
                bp+"_bool0=true;}RECORDER.inc(9);hashCode();RECORDER.inc(10);break;}}}"
        String instrWithSniffer = snifferField + instr

        // test suppress warnings injection
        checkInstrumentation([
                // no existing suppression
                [ "class A {" + src,
                        "@java.lang.SuppressWarnings({\"fallthrough\"}) class A {" + instrWithSniffer],

                // existing, empty, no brackets
                [ "@SuppressWarnings class B {" + src,
                        "@SuppressWarnings({\"fallthrough\"}) class B {" + instrWithSniffer],

                // existing, empty
                [ "@SuppressWarnings() class C {" + src,
                        "@SuppressWarnings({\"fallthrough\"}) class C {" + instrWithSniffer],

                // existing, single element in array init
                [ "@SuppressWarnings({\"unchecked\"}) class D {" + src,
                        "@SuppressWarnings({\"unchecked\",\"fallthrough\"}) class D {" + instrWithSniffer],

                // existing, more than one array element
                [ "@SuppressWarnings({\"unchecked\",\"fallthrough\"}) class E {" + src,
                        "@SuppressWarnings({\"unchecked\",\"fallthrough\"}) class E {" + instrWithSniffer],

                //existing, single value
                [ "@SuppressWarnings(\"fallthrough\") class F {" + src,
                        "@SuppressWarnings(\"fallthrough\") class F {" + instrWithSniffer],

                //existing, full syntax
                [ "@SuppressWarnings(value={\"unchecked\"}) class G {" + src,
                        "@SuppressWarnings(value={\"unchecked\",\"fallthrough\"}) class G {" + instrWithSniffer],

                //existing, full syntax without array init
                [ "@SuppressWarnings(value=\"unchecked\") class H {" + src,
                        "@SuppressWarnings(value={\"unchecked\",\"fallthrough\"}) class H {" + instrWithSniffer],

                // no existing suppression, other annotation
                [ "@MyAnnotation class I {" + src,
                        "@java.lang.SuppressWarnings({\"fallthrough\"}) @MyAnnotation class I {" + instrWithSniffer],

                // no existing suppression, other annotations
                [ "@MyAnnotation @MyOtherAnno class J {" + src,
                        "@java.lang.SuppressWarnings({\"fallthrough\"}) @MyAnnotation @MyOtherAnno class J {" + instrWithSniffer],

                // no existing suppression, other annotations
                [ "@MyAnnotation @MyOtherAnno class K { @YetMoreAnno public L() " + src,
                        "@java.lang.SuppressWarnings({\"fallthrough\"}) @MyAnnotation @MyOtherAnno class K {" + snifferField + " @YetMoreAnno public L() " + instr],

                // no existing suppression, other annotations
                [ "@MyAnnotation @MyOtherAnno class M { @B @SuppressWarnings(\"fallthrough\") public N() " + src,
                        "@java.lang.SuppressWarnings({\"fallthrough\"}) @MyAnnotation @MyOtherAnno class M {" + snifferField + " @B @SuppressWarnings(\"fallthrough\") public N() " + instr],
        ] as String[][])
    }

    @Test
    public void testConstExpr()
            throws Exception {
        // constant for loop
        checkStatement(
            "for (;true;) {System.out.println(a[i]);}",
             "RECORDER.inc(1);for (;true;) {{RECORDER.inc(2);System.out.println(a[i]);}}")
    }

    @Test
    public void testForLoop()
            throws Exception {
        // traditional for loop
        checkStatement(
            "for (int i = 0; i < a.length; i++) {System.out.println(a[i]);}",
             "RECORDER.inc(1);for (int i = 0; (((i < a.length)&&(RECORDER.iget(2)!=0|true))||(RECORDER.iget(3)==0&false)); i++) {{RECORDER.inc(4);System.out.println(a[i]);}}")
        // traditional for loop, no braces
        checkStatement(
                 "for (int i = 0; i < a.length; i++) System.out.println(a[i]);",
                  "RECORDER.inc(1);for (int i = 0; (((i < a.length)&&(RECORDER.iget(2)!=0|true))||(RECORDER.iget(3)==0&false)); i++) {RECORDER.inc(4);System.out.println(a[i]);}")
        // enhanced for loop
        checkStatement(
            "for (int i : a) {System.out.println(i);}",
             "RECORDER.inc(1);for (int i : a) {{RECORDER.inc(2);System.out.println(i);}}")
    }

    @Test
    public void testConditionalWithAssignment() throws Exception {
        // Don't instrument a conditional containing an assignment since this breaks Definite Assignment rules in javac
        checkStatement(
            "String line; while ((line = in.readLine()) != null) {System.out.println(line);}",
            "RECORDER.inc(1);String line; RECORDER.inc(2);while ((line = in.readLine()) != null) {{RECORDER.inc(5);System.out.println(line);}}")

    }

    @Test
    public void testConditionalWithCloverOff() throws Exception {
        // Preserve a conditional and don't add extraneous parentheses when CLOVER:OFF is specified as this may trigger
        // compiler bugs for generic calls. See http://bugs.sun.com/view_bug.do?bug_id=6608961

        //Note Clover inserts extraneous curly braces too
        checkStatement(
            "\n///CLOVER:OFF\nif (a + b == 2) { System.out.println(\"Hello, world\"); }\n///CLOVER:ON\n",
            "\n///CLOVER:OFF\nif (a + b == 2) {{ System.out.println(\"Hello, world\"); }\n///CLOVER:ON\n}")

    }

    @Test
    public void testMethodLevelInstr() throws Exception {
        checkStatement("int i = 0;", "int i = 0;", InstrumentationLevel.METHOD.ordinal())
        checkStatement("int i = arg == 2 ? 3 : 4;", "int i = arg == 2 ? 3 : 4;", InstrumentationLevel.METHOD.ordinal())
        checkStatement("assert arg > 0;", "assert arg > 0;", InstrumentationLevel.METHOD.ordinal())

        JavaInstrumentationConfig config = getInstrConfig(newDbTempFile().getAbsolutePath(), false, true, false)
        config.setFlushPolicy(InstrumentationConfig.INTERVAL_FLUSHING)
        config.setInstrLevel(InstrumentationLevel.METHOD.ordinal())
        checkInstrumentation([
                ["class B { public B(int arg) {}}",
                 "class B {" + snifferField + " public B(int arg) {try{RECORDER.inc(0);}finally{RECORDER.maybeFlush();}}}"]
            ] as String[][],
            config)

        config = getInstrConfig(newDbTempFile().getAbsolutePath(), false, true, false)
        config.setFlushPolicy(InstrumentationConfig.INTERVAL_FLUSHING)
        config.setInstrLevel(InstrumentationLevel.METHOD.ordinal())

        checkInstrumentation([
                ["class B { public B(int arg) {int i = 0;}}",
                 "class B {" + snifferField + " public B(int arg) {try{RECORDER.inc(0);int i = 0;}finally{RECORDER.maybeFlush();}}}"]
            ] as String[][],
            config)

        config = getInstrConfig(newDbTempFile().getAbsolutePath(), false, true, false)
        config.setFlushPolicy(InstrumentationConfig.THREADED_FLUSHING)
        config.setInstrLevel(InstrumentationLevel.METHOD.ordinal())

        checkInstrumentation([
                ["class B { public B(int arg) {}}",
                 "class B {" + snifferField + " public B(int arg) {try{RECORDER.inc(0);}finally{RECORDER.flushNeeded();}}}"]
            ] as String[][],
            config)

        config = getInstrConfig(newDbTempFile().getAbsolutePath(), false, true, false)
        config.setFlushPolicy(InstrumentationConfig.THREADED_FLUSHING)
        config.setInstrLevel(InstrumentationLevel.METHOD.ordinal())

        checkInstrumentation([
                ["class B { public B(int arg) {int i = 0;}}",
                 "class B {" + snifferField + " public B(int arg) {try{RECORDER.inc(0);int i = 0;}finally{RECORDER.flushNeeded();}}}"]
            ] as String[][],
            config)
    }

    @Test
    public void testTestMethod() throws Exception {
        checkInstrumentation([
                [ "public class MyTest { @Test public Class<? extends Throwable> foo(){} }",
                  "public class MyTest {" + snifferField + " @Test public Class<? extends Throwable> foo(){" + "RECORDER.globalSliceStart(getClass().getName(),0);int "+ CloverNames.namespace("p")+"=0;java.lang.Throwable "+ CloverNames.namespace("t")+"=null;try{Class<? extends Throwable> "+ CloverNames.namespace("r")+"=RECORDER();"+ CloverNames.namespace("p")+"=1;return "+ CloverNames.namespace("r")+";}catch(java.lang.Throwable "+ CloverNames.namespace("t2")+"){if("+ CloverNames.namespace("p")+"==0&&"+ CloverNames.namespace("t")+"==null){"+ CloverNames.namespace("t")+"="+ CloverNames.namespace("t2")+";}RECORDER.rethrow("+ CloverNames.namespace("t2")+");return null;}finally{RECORDER.globalSliceEnd(getClass().getName(),\"MyTest.foo\",SNIFFER.getTestName(),0,"+ CloverNames.namespace("p")+","+ CloverNames.namespace("t")+");}}private Class<? extends Throwable>  RECORDER(){RECORDER.inc(0);} }" ],

                [ "public class MyTest { @Test public static Class<? extends Throwable> foo(){} }",
                  "public class MyTest {" + snifferField + " @Test public static Class<? extends Throwable> foo(){RECORDER.globalSliceStart(MyTest.class.getName(),0);int "+ CloverNames.namespace("p")+"=0;java.lang.Throwable "+ CloverNames.namespace("t")+"=null;try{Class<? extends Throwable> "+ CloverNames.namespace("r")+"=RECORDER();"+ CloverNames.namespace("p")+"=1;return "+ CloverNames.namespace("r")+";}catch(java.lang.Throwable "+ CloverNames.namespace("t2")+"){if("+ CloverNames.namespace("p")+"==0&&"+ CloverNames.namespace("t")+"==null){"+ CloverNames.namespace("t")+"="+ CloverNames.namespace("t2")+";}RECORDER.rethrow("+ CloverNames.namespace("t2")+");return null;}finally{RECORDER.globalSliceEnd(MyTest.class.getName(),\"MyTest.foo\",SNIFFER.getTestName(),0,"+ CloverNames.namespace("p")+","+ CloverNames.namespace("t")+");}}private static Class<? extends Throwable>  RECORDER(){RECORDER.inc(0);} }" ],

                [ "public class MyTest { @Test public Class<? extends Throwable> foo(int a, boolean b){} }",
                  "public class MyTest {" + snifferField + " @Test public Class<? extends Throwable> foo(int a, boolean b){RECORDER.globalSliceStart(getClass().getName(),0);int "+ CloverNames.namespace("p")+"=0;java.lang.Throwable "+ CloverNames.namespace("t")+"=null;try{Class<? extends Throwable> "+ CloverNames.namespace("r")+"=RECORDER(a,b);"+ CloverNames.namespace("p")+"=1;return "+ CloverNames.namespace("r")+";}catch(java.lang.Throwable "+ CloverNames.namespace("t2")+"){if("+ CloverNames.namespace("p")+"==0&&"+ CloverNames.namespace("t")+"==null){"+ CloverNames.namespace("t")+"="+ CloverNames.namespace("t2")+";}RECORDER.rethrow("+ CloverNames.namespace("t2")+");return null;}finally{RECORDER.globalSliceEnd(getClass().getName(),\"MyTest.foo\",SNIFFER.getTestName(),0,"+ CloverNames.namespace("p")+","+ CloverNames.namespace("t")+");}}private Class<? extends Throwable>  RECORDER(int a, boolean b){RECORDER.inc(0);} }" ],

                [ "public class MyTest { @Test public Class<? extends Throwable> foo(int a[], boolean b){} }",
                  "public class MyTest {" + snifferField + " @Test public Class<? extends Throwable> foo(int a[], boolean b){RECORDER.globalSliceStart(getClass().getName(),0);int "+ CloverNames.namespace("p")+"=0;java.lang.Throwable "+ CloverNames.namespace("t")+"=null;try{Class<? extends Throwable> "+ CloverNames.namespace("r")+"=RECORDER(a,b);"+ CloverNames.namespace("p")+"=1;return "+ CloverNames.namespace("r")+";}catch(java.lang.Throwable "+ CloverNames.namespace("t2")+"){if("+ CloverNames.namespace("p")+"==0&&"+ CloverNames.namespace("t")+"==null){"+ CloverNames.namespace("t")+"="+ CloverNames.namespace("t2")+";}RECORDER.rethrow("+ CloverNames.namespace("t2")+");return null;}finally{RECORDER.globalSliceEnd(getClass().getName(),\"MyTest.foo\",SNIFFER.getTestName(),0,"+ CloverNames.namespace("p")+","+ CloverNames.namespace("t")+");}}private Class<? extends Throwable>  RECORDER(int a[], boolean b){RECORDER.inc(0);} }" ],

                [ "public class MyTest { @Test public <T> Class<T> foo(int a[], boolean b){} }",
                  "public class MyTest {" + snifferField + " @Test public <T> Class<T> foo(int a[], boolean b){RECORDER.globalSliceStart(getClass().getName(),0);int "+ CloverNames.namespace("p")+"=0;java.lang.Throwable "+ CloverNames.namespace("t")+"=null;try{Class<T> "+ CloverNames.namespace("r")+"=RECORDER(a,b);"+ CloverNames.namespace("p")+"=1;return "+ CloverNames.namespace("r")+";}catch(java.lang.Throwable "+ CloverNames.namespace("t2")+"){if("+ CloverNames.namespace("p")+"==0&&"+ CloverNames.namespace("t")+"==null){"+ CloverNames.namespace("t")+"="+ CloverNames.namespace("t2")+";}RECORDER.rethrow("+ CloverNames.namespace("t2")+");return null;}finally{RECORDER.globalSliceEnd(getClass().getName(),\"MyTest.foo\",SNIFFER.getTestName(),0,"+ CloverNames.namespace("p")+","+ CloverNames.namespace("t")+");}}private <T> Class<T>  RECORDER(int a[], boolean b){RECORDER.inc(0);} }" ],

                [ "public class MyTest { @Test public <T extends Serializable> T foo(int a[], boolean b){} }",
                  "public class MyTest {" + snifferField + " @Test public <T extends Serializable> T foo(int a[], boolean b){RECORDER.globalSliceStart(getClass().getName(),0);int "+ CloverNames.namespace("p")+"=0;java.lang.Throwable "+ CloverNames.namespace("t")+"=null;try{T "+ CloverNames.namespace("r")+"=RECORDER(a,b);"+ CloverNames.namespace("p")+"=1;return "+ CloverNames.namespace("r")+";}catch(java.lang.Throwable "+ CloverNames.namespace("t2")+"){if("+ CloverNames.namespace("p")+"==0&&"+ CloverNames.namespace("t")+"==null){"+ CloverNames.namespace("t")+"="+ CloverNames.namespace("t2")+";}RECORDER.rethrow("+ CloverNames.namespace("t2")+");return null;}finally{RECORDER.globalSliceEnd(getClass().getName(),\"MyTest.foo\",SNIFFER.getTestName(),0,"+ CloverNames.namespace("p")+","+ CloverNames.namespace("t")+");}}private <T extends Serializable> T  RECORDER(int a[], boolean b){RECORDER.inc(0);} }" ],
        ] as String[][])
    }

    @Test
    public void testTestMethodWithNoRewrite() throws Exception {
        checkInstrumentation([
                ["public class MyTest { @Test public void testFoo(){} }",
                 "public class MyTest {" + snifferField + " @Test public void testFoo(){try{RECORDER.globalSliceStart(getClass().getName(),0);RECORDER.inc(0);}finally{RECORDER.globalSliceEnd(getClass().getName(),\"MyTest.testFoo\",SNIFFER.getTestName(),0);}} }" ],

                ["public class MyTest { @Test public static void testFoo(){} }",
                 "public class MyTest {" + snifferField + " @Test public static void testFoo(){try{RECORDER.globalSliceStart(MyTest.class.getName(),0);RECORDER.inc(0);}finally{RECORDER.globalSliceEnd(MyTest.class.getName(),\"MyTest.testFoo\",SNIFFER.getTestName(),0);}} }"]
            ] as String[][], false)
    }

    @Test
    public void testExpectedExceptions() throws Exception {
        checkInstrumentation([
                [ "public class MyTestA { @Test public void foo(){} }",
                  "public class MyTestA {" + snifferField + " @Test public void foo(){RECORDER.globalSliceStart(getClass().getName(),0);int " + CloverNames.namespace("p") + "=0;java.lang.Throwable " + CloverNames.namespace("t") + "=null;try{RECORDER();"+ CloverNames.namespace("p")+"=1;}catch(java.lang.Throwable "+ CloverNames.namespace("t2")+"){if("+ CloverNames.namespace("p")+"==0&&"+ CloverNames.namespace("t")+"==null){"+ CloverNames.namespace("t")+"="+ CloverNames.namespace("t2")+";}RECORDER.rethrow("+ CloverNames.namespace("t2")+");}finally{RECORDER.globalSliceEnd(getClass().getName(),\"MyTestA.foo\",SNIFFER.getTestName(),0,"+ CloverNames.namespace("p")+","+ CloverNames.namespace("t")+");}}private void  RECORDER(){RECORDER.inc(0);} }" ],

                [ "public class MyTestB { /** @testng.test */public void foo(){} }",
                  "public class MyTestB {" + snifferField + " /** @testng.test */public void foo(){RECORDER.globalSliceStart(getClass().getName(),0);int " + CloverNames.namespace("p") + "=0;java.lang.Throwable " + CloverNames.namespace("t") + "=null;try{RECORDER();"+ CloverNames.namespace("p")+"=1;}catch(java.lang.Throwable "+ CloverNames.namespace("t2")+"){if("+ CloverNames.namespace("p")+"==0&&"+ CloverNames.namespace("t")+"==null){"+ CloverNames.namespace("t")+"="+ CloverNames.namespace("t2")+";}RECORDER.rethrow("+ CloverNames.namespace("t2")+");}finally{RECORDER.globalSliceEnd(getClass().getName(),\"MyTestB.foo\",SNIFFER.getTestName(),0,"+ CloverNames.namespace("p")+","+ CloverNames.namespace("t")+");}}private void  RECORDER(){RECORDER.inc(0);} }" ],

                [ "public class MyTestC { @Test(expectedExceptions={}) public void foo(){} }",
                  "public class MyTestC {" + snifferField + " @Test(expectedExceptions={}) public void foo(){RECORDER.globalSliceStart(getClass().getName(),0);int " + CloverNames.namespace("p") + "=0;java.lang.Throwable " + CloverNames.namespace("t") + "=null;try{RECORDER();"+ CloverNames.namespace("p")+"=1;}catch(java.lang.Throwable "+ CloverNames.namespace("t2")+"){if("+ CloverNames.namespace("p")+"==0&&"+ CloverNames.namespace("t")+"==null){"+ CloverNames.namespace("t")+"="+ CloverNames.namespace("t2")+";}RECORDER.rethrow("+ CloverNames.namespace("t2")+");}finally{RECORDER.globalSliceEnd(getClass().getName(),\"MyTestC.foo\",SNIFFER.getTestName(),0,"+ CloverNames.namespace("p")+","+ CloverNames.namespace("t")+");}}private void  RECORDER(){RECORDER.inc(0);} }" ],

                [ "public class MyTestD { /** @testng.test expectedExceptions=\"\" */public void foo(){} }",
                  "public class MyTestD {" + snifferField + " /** @testng.test expectedExceptions=\"\" */public void foo(){RECORDER.globalSliceStart(getClass().getName(),0);int " + CloverNames.namespace("p") + "=0;java.lang.Throwable " + CloverNames.namespace("t") + "=null;try{RECORDER();"+ CloverNames.namespace("p")+"=1;}catch(java.lang.Throwable "+ CloverNames.namespace("t2")+"){if("+ CloverNames.namespace("p")+"==0&&"+ CloverNames.namespace("t")+"==null){"+ CloverNames.namespace("t")+"="+ CloverNames.namespace("t2")+";}RECORDER.rethrow("+ CloverNames.namespace("t2")+");}finally{RECORDER.globalSliceEnd(getClass().getName(),\"MyTestD.foo\",SNIFFER.getTestName(),0,"+ CloverNames.namespace("p")+","+ CloverNames.namespace("t")+");}}private void  RECORDER(){RECORDER.inc(0);} }" ],

                [ "public class MyTestE { @org.testng.annotations.Test(expectedExceptions={}) public void foo(){} }",
                  "public class MyTestE {" + snifferField + " @org.testng.annotations.Test(expectedExceptions={}) public void foo(){RECORDER.globalSliceStart(getClass().getName(),0);int " + CloverNames.namespace("p") + "=0;java.lang.Throwable " + CloverNames.namespace("t") + "=null;try{RECORDER();"+ CloverNames.namespace("p")+"=1;}catch(java.lang.Throwable "+ CloverNames.namespace("t2")+"){if("+ CloverNames.namespace("p")+"==0&&"+ CloverNames.namespace("t")+"==null){"+ CloverNames.namespace("t")+"="+ CloverNames.namespace("t2")+";}RECORDER.rethrow("+ CloverNames.namespace("t2")+");}finally{RECORDER.globalSliceEnd(getClass().getName(),\"MyTestE.foo\",SNIFFER.getTestName(),0,"+ CloverNames.namespace("p")+","+ CloverNames.namespace("t")+");}}private void  RECORDER(){RECORDER.inc(0);} }" ],

                [ "public class MyTestF { @Test(expected={}) public void foo(){} }",
                  "public class MyTestF {" + snifferField + " @Test(expected={}) public void foo(){RECORDER.globalSliceStart(getClass().getName(),0);int " + CloverNames.namespace("p") + "=0;java.lang.Throwable " + CloverNames.namespace("t") + "=null;try{RECORDER();"+ CloverNames.namespace("p")+"=1;}catch(java.lang.Throwable "+ CloverNames.namespace("t2")+"){if("+ CloverNames.namespace("p")+"==0&&"+ CloverNames.namespace("t")+"==null){"+ CloverNames.namespace("t")+"="+ CloverNames.namespace("t2")+";}RECORDER.rethrow("+ CloverNames.namespace("t2")+");}finally{RECORDER.globalSliceEnd(getClass().getName(),\"MyTestF.foo\",SNIFFER.getTestName(),0,"+ CloverNames.namespace("p")+","+ CloverNames.namespace("t")+");}}private void  RECORDER(){RECORDER.inc(0);} }" ],

                [ "public class MyTestG { @org.junit.Test(expected={}) public void foo(){} }",
                  "public class MyTestG {" + snifferField + " @org.junit.Test(expected={}) public void foo(){RECORDER.globalSliceStart(getClass().getName(),0);int " + CloverNames.namespace("p") + "=0;java.lang.Throwable " + CloverNames.namespace("t") + "=null;try{RECORDER();"+ CloverNames.namespace("p")+"=1;}catch(java.lang.Throwable "+ CloverNames.namespace("t2")+"){if("+ CloverNames.namespace("p")+"==0&&"+ CloverNames.namespace("t")+"==null){"+ CloverNames.namespace("t")+"="+ CloverNames.namespace("t2")+";}RECORDER.rethrow("+ CloverNames.namespace("t2")+");}finally{RECORDER.globalSliceEnd(getClass().getName(),\"MyTestG.foo\",SNIFFER.getTestName(),0,"+ CloverNames.namespace("p")+","+ CloverNames.namespace("t")+");}}private void  RECORDER(){RECORDER.inc(0);} }" ],

                [ "public class MyTestH { @Test @ExpectedExceptions({}) public void foo(){} }",
                  "public class MyTestH {" + snifferField + " @Test @ExpectedExceptions({}) public void foo(){RECORDER.globalSliceStart(getClass().getName(),0);int " + CloverNames.namespace("p") + "=0;java.lang.Throwable " + CloverNames.namespace("t") + "=null;try{RECORDER();"+ CloverNames.namespace("p")+"=1;}catch(java.lang.Throwable "+ CloverNames.namespace("t2")+"){if("+ CloverNames.namespace("p")+"==0&&"+ CloverNames.namespace("t")+"==null){"+ CloverNames.namespace("t")+"="+ CloverNames.namespace("t2")+";}RECORDER.rethrow("+ CloverNames.namespace("t2")+");}finally{RECORDER.globalSliceEnd(getClass().getName(),\"MyTestH.foo\",SNIFFER.getTestName(),0,"+ CloverNames.namespace("p")+","+ CloverNames.namespace("t")+");}}private void  RECORDER(){RECORDER.inc(0);} }" ],

                [ "public class MyTestI { /** @testng.test\n* @testng.expected-exceptions value=\"\" */\npublic void foo(){} }",
                  "public class MyTestI {" + snifferField + " /** @testng.test\n* @testng.expected-exceptions value=\"\" */\npublic void foo(){RECORDER.globalSliceStart(getClass().getName(),0);int " + CloverNames.namespace("p") + "=0;java.lang.Throwable " + CloverNames.namespace("t") + "=null;try{RECORDER();"+ CloverNames.namespace("p")+"=1;}catch(java.lang.Throwable "+ CloverNames.namespace("t2")+"){if("+ CloverNames.namespace("p")+"==0&&"+ CloverNames.namespace("t")+"==null){"+ CloverNames.namespace("t")+"="+ CloverNames.namespace("t2")+";}RECORDER.rethrow("+ CloverNames.namespace("t2")+");}finally{RECORDER.globalSliceEnd(getClass().getName(),\"MyTestI.foo\",SNIFFER.getTestName(),0,"+ CloverNames.namespace("p")+","+ CloverNames.namespace("t")+");}}private void  RECORDER(){RECORDER.inc(0);} }" ],

                [ "public class MyTestJ { @Test @org.testng.annotations.ExpectedExceptions({}) public void foo(){} }",
                  "public class MyTestJ {" + snifferField + " @Test @org.testng.annotations.ExpectedExceptions({}) public void foo(){RECORDER.globalSliceStart(getClass().getName(),0);int " + CloverNames.namespace("p") + "=0;java.lang.Throwable " + CloverNames.namespace("t") + "=null;try{RECORDER();"+ CloverNames.namespace("p")+"=1;}catch(java.lang.Throwable "+ CloverNames.namespace("t2")+"){if("+ CloverNames.namespace("p")+"==0&&"+ CloverNames.namespace("t")+"==null){"+ CloverNames.namespace("t")+"="+ CloverNames.namespace("t2")+";}RECORDER.rethrow("+ CloverNames.namespace("t2")+");}finally{RECORDER.globalSliceEnd(getClass().getName(),\"MyTestJ.foo\",SNIFFER.getTestName(),0,"+ CloverNames.namespace("p")+","+ CloverNames.namespace("t")+");}}private void  RECORDER(){RECORDER.inc(0);} }" ],

                [ "public class MyTestK { @org.testng.annotations.Test(expectedExceptions={Foo.class,Bar.class}) public void foo(){} }",
                  "public class MyTestK {" + snifferField + " @org.testng.annotations.Test(expectedExceptions={Foo.class,Bar.class}) public void foo(){RECORDER.globalSliceStart(getClass().getName(),0);int " + CloverNames.namespace("p") + "=0;java.lang.Throwable " + CloverNames.namespace("t") + "=null;try{RECORDER();"+ CloverNames.namespace("p")+"=0;"+ CloverNames.namespace("t")+"=new java.lang.RuntimeException(new String(new char[] {69,120,112,101,99,116,101,100,32,111,110,101,32,111,102,32,116,104,101,32,102,111,108,108,111,119,105,110,103,32,101,120,99,101,112,116,105,111,110,115,32,116,111,32,98,101,32,116,104,114,111,119,110,32,102,114,111,109,32,116,101,115,116,32,109,101,116,104,111,100,32,102,111,111,58,32,91,66,97,114,44,32,70,111,111,93,}));}catch(java.lang.Throwable "+ CloverNames.namespace("t2")+"){if("+ CloverNames.namespace("t2")+" instanceof Bar||"+ CloverNames.namespace("t2")+" instanceof Foo){"+ CloverNames.namespace("p")+"=1;" + CloverNames.namespace("t") + "=null;}else{"+ CloverNames.namespace("p")+"=0;"+ CloverNames.namespace("t")+"="+ CloverNames.namespace("t2")+";}if("+ CloverNames.namespace("p")+"==0&&"+ CloverNames.namespace("t")+"==null){"+ CloverNames.namespace("t")+"="+ CloverNames.namespace("t2")+";}RECORDER.rethrow("+ CloverNames.namespace("t2")+");}finally{RECORDER.globalSliceEnd(getClass().getName(),\"MyTestK.foo\",SNIFFER.getTestName(),0,"+ CloverNames.namespace("p")+","+ CloverNames.namespace("t")+");}}private void  RECORDER(){RECORDER.inc(0);} }" ],

                [ "public class MyTestL { /** @testng.test expectedExceptions = \"Foo Bar\" */public void foo(){} }",
                  "public class MyTestL {" + snifferField + " /** @testng.test expectedExceptions = \"Foo Bar\" */public void foo(){RECORDER.globalSliceStart(getClass().getName(),0);int " + CloverNames.namespace("p") + "=0;java.lang.Throwable " + CloverNames.namespace("t") + "=null;try{RECORDER();"+ CloverNames.namespace("p")+"=0;"+ CloverNames.namespace("t")+"=new java.lang.RuntimeException(new String(new char[] {69,120,112,101,99,116,101,100,32,111,110,101,32,111,102,32,116,104,101,32,102,111,108,108,111,119,105,110,103,32,101,120,99,101,112,116,105,111,110,115,32,116,111,32,98,101,32,116,104,114,111,119,110,32,102,114,111,109,32,116,101,115,116,32,109,101,116,104,111,100,32,102,111,111,58,32,91,66,97,114,44,32,70,111,111,93,}));}catch(java.lang.Throwable "+ CloverNames.namespace("t2")+"){if("+ CloverNames.namespace("t2")+" instanceof Bar||"+ CloverNames.namespace("t2")+" instanceof Foo){"+ CloverNames.namespace("p")+"=1;" + CloverNames.namespace("t") + "=null;}else{"+ CloverNames.namespace("p")+"=0;"+ CloverNames.namespace("t")+"="+ CloverNames.namespace("t2")+";}if("+ CloverNames.namespace("p")+"==0&&"+ CloverNames.namespace("t")+"==null){"+ CloverNames.namespace("t")+"="+ CloverNames.namespace("t2")+";}RECORDER.rethrow("+ CloverNames.namespace("t2")+");}finally{RECORDER.globalSliceEnd(getClass().getName(),\"MyTestL.foo\",SNIFFER.getTestName(),0,"+ CloverNames.namespace("p")+","+ CloverNames.namespace("t")+");}}private void  RECORDER(){RECORDER.inc(0);} }" ],

                [ "public class MyTestM { @Test @ExpectedExceptions({Foo.class,Bar.class}) public void foo(){} }",
                  "public class MyTestM {" + snifferField + " @Test @ExpectedExceptions({Foo.class,Bar.class}) public void foo(){RECORDER.globalSliceStart(getClass().getName(),0);int " + CloverNames.namespace("p") + "=0;java.lang.Throwable " + CloverNames.namespace("t") + "=null;try{RECORDER();"+ CloverNames.namespace("p")+"=0;"+ CloverNames.namespace("t")+"=new java.lang.RuntimeException(new String(new char[] {69,120,112,101,99,116,101,100,32,111,110,101,32,111,102,32,116,104,101,32,102,111,108,108,111,119,105,110,103,32,101,120,99,101,112,116,105,111,110,115,32,116,111,32,98,101,32,116,104,114,111,119,110,32,102,114,111,109,32,116,101,115,116,32,109,101,116,104,111,100,32,102,111,111,58,32,91,66,97,114,44,32,70,111,111,93,}));}catch(java.lang.Throwable "+ CloverNames.namespace("t2")+"){if("+ CloverNames.namespace("t2")+" instanceof Bar||"+ CloverNames.namespace("t2")+" instanceof Foo){"+ CloverNames.namespace("p")+"=1;" + CloverNames.namespace("t") + "=null;}else{"+ CloverNames.namespace("p")+"=0;"+ CloverNames.namespace("t")+"="+ CloverNames.namespace("t2")+";}if("+ CloverNames.namespace("p")+"==0&&"+ CloverNames.namespace("t")+"==null){"+ CloverNames.namespace("t")+"="+ CloverNames.namespace("t2")+";}RECORDER.rethrow("+ CloverNames.namespace("t2")+");}finally{RECORDER.globalSliceEnd(getClass().getName(),\"MyTestM.foo\",SNIFFER.getTestName(),0,"+ CloverNames.namespace("p")+","+ CloverNames.namespace("t")+");}}private void  RECORDER(){RECORDER.inc(0);} }" ],

                [ "public class MyTestN { @org.testng.annotations.Test @org.testng.annotations.ExpectedExceptions({Foo.class,Bar.class}) public void foo(){} }",
                  "public class MyTestN {" + snifferField + " @org.testng.annotations.Test @org.testng.annotations.ExpectedExceptions({Foo.class,Bar.class}) public void foo(){RECORDER.globalSliceStart(getClass().getName(),0);int " + CloverNames.namespace("p") + "=0;java.lang.Throwable " + CloverNames.namespace("t") + "=null;try{RECORDER();"+ CloverNames.namespace("p")+"=0;"+ CloverNames.namespace("t")+"=new java.lang.RuntimeException(new String(new char[] {69,120,112,101,99,116,101,100,32,111,110,101,32,111,102,32,116,104,101,32,102,111,108,108,111,119,105,110,103,32,101,120,99,101,112,116,105,111,110,115,32,116,111,32,98,101,32,116,104,114,111,119,110,32,102,114,111,109,32,116,101,115,116,32,109,101,116,104,111,100,32,102,111,111,58,32,91,66,97,114,44,32,70,111,111,93,}));}catch(java.lang.Throwable "+ CloverNames.namespace("t2")+"){if("+ CloverNames.namespace("t2")+" instanceof Bar||"+ CloverNames.namespace("t2")+" instanceof Foo){"+ CloverNames.namespace("p")+"=1;" + CloverNames.namespace("t") + "=null;}else{"+ CloverNames.namespace("p")+"=0;"+ CloverNames.namespace("t")+"="+ CloverNames.namespace("t2")+";}if("+ CloverNames.namespace("p")+"==0&&"+ CloverNames.namespace("t")+"==null){"+ CloverNames.namespace("t")+"="+ CloverNames.namespace("t2")+";}RECORDER.rethrow("+ CloverNames.namespace("t2")+");}finally{RECORDER.globalSliceEnd(getClass().getName(),\"MyTestN.foo\",SNIFFER.getTestName(),0,"+ CloverNames.namespace("p")+","+ CloverNames.namespace("t")+");}}private void  RECORDER(){RECORDER.inc(0);} }" ],

                [ "public class MyTestO { /** @testng.test\n * @testng.expected-exceptions value = \"Foo Bar\"\n*/\npublic void foo(){} }",
                  "public class MyTestO {" + snifferField + " /** @testng.test\n * @testng.expected-exceptions value = \"Foo Bar\"\n*/\npublic void foo(){RECORDER.globalSliceStart(getClass().getName(),0);int " + CloverNames.namespace("p") + "=0;java.lang.Throwable " + CloverNames.namespace("t") + "=null;try{RECORDER();"+ CloverNames.namespace("p")+"=0;"+ CloverNames.namespace("t")+"=new java.lang.RuntimeException(new String(new char[] {69,120,112,101,99,116,101,100,32,111,110,101,32,111,102,32,116,104,101,32,102,111,108,108,111,119,105,110,103,32,101,120,99,101,112,116,105,111,110,115,32,116,111,32,98,101,32,116,104,114,111,119,110,32,102,114,111,109,32,116,101,115,116,32,109,101,116,104,111,100,32,102,111,111,58,32,91,66,97,114,44,32,70,111,111,93,}));}catch(java.lang.Throwable "+ CloverNames.namespace("t2")+"){if("+ CloverNames.namespace("t2")+" instanceof Bar||"+ CloverNames.namespace("t2")+" instanceof Foo){"+ CloverNames.namespace("p")+"=1;" + CloverNames.namespace("t") + "=null;}else{"+ CloverNames.namespace("p")+"=0;"+ CloverNames.namespace("t")+"="+ CloverNames.namespace("t2")+";}if("+ CloverNames.namespace("p")+"==0&&"+ CloverNames.namespace("t")+"==null){"+ CloverNames.namespace("t")+"="+ CloverNames.namespace("t2")+";}RECORDER.rethrow("+ CloverNames.namespace("t2")+");}finally{RECORDER.globalSliceEnd(getClass().getName(),\"MyTestO.foo\",SNIFFER.getTestName(),0,"+ CloverNames.namespace("p")+","+ CloverNames.namespace("t")+");}}private void  RECORDER(){RECORDER.inc(0);} }" ],

                [ "public class MyTestP { @Test(expected=Foo.class) public void foo() throws Foo, Bar {} }",
                  "public class MyTestP {" + snifferField + " @Test(expected=Foo.class) public void foo() throws Foo, Bar {RECORDER.globalSliceStart(getClass().getName(),0);int " + CloverNames.namespace("p") + "=0;java.lang.Throwable " + CloverNames.namespace("t") + "=null;try{RECORDER();"+ CloverNames.namespace("p")+"=0;"+ CloverNames.namespace("t")+"=new java.lang.RuntimeException(new String(new char[] {69,120,112,101,99,116,101,100,32,111,110,101,32,111,102,32,116,104,101,32,102,111,108,108,111,119,105,110,103,32,101,120,99,101,112,116,105,111,110,115,32,116,111,32,98,101,32,116,104,114,111,119,110,32,102,114,111,109,32,116,101,115,116,32,109,101,116,104,111,100,32,102,111,111,58,32,91,70,111,111,93,}));}catch(java.lang.Throwable "+ CloverNames.namespace("t2")+"){if("+ CloverNames.namespace("t2")+" instanceof Foo){"+ CloverNames.namespace("p")+"=1;" + CloverNames.namespace("t") + "=null;}else{"+ CloverNames.namespace("p")+"=0;"+ CloverNames.namespace("t")+"="+ CloverNames.namespace("t2")+";}if("+ CloverNames.namespace("p")+"==0&&"+ CloverNames.namespace("t")+"==null){"+ CloverNames.namespace("t")+"="+ CloverNames.namespace("t2")+";}RECORDER.rethrow("+ CloverNames.namespace("t2")+");}finally{RECORDER.globalSliceEnd(getClass().getName(),\"MyTestP.foo\",SNIFFER.getTestName(),0,"+ CloverNames.namespace("p")+","+ CloverNames.namespace("t")+");}}private void  RECORDER() throws Foo, Bar{RECORDER.inc(0);} }" ],
            ] as String[][])
    }

    @Test
    public void testLambdaExprToBlockReplace() throws Exception {
        checkInstrumentation([
                [ "public class MyTestA { @Test public void foo(){Stream.of(1, 2, 3, 4, 5, 6).map((i) -> \"String:\" + String.valueOf(i));} }",
                  "public class MyTestA {" + snifferField + " @Test public void foo(){RECORDER.globalSliceStart(getClass().getName(),0);int " + CloverNames.namespace("p") + "=0;java.lang.Throwable " + CloverNames.namespace("t") + "=null;try{RECORDER();"+ CloverNames.namespace("p")+"=1;}catch(java.lang.Throwable "+ CloverNames.namespace("t2")+"){if("+ CloverNames.namespace("p")+"==0&&"+ CloverNames.namespace("t")+"==null){"+ CloverNames.namespace("t")+"="+ CloverNames.namespace("t2")+";}RECORDER.rethrow("+ CloverNames.namespace("t2")+");}finally{RECORDER.globalSliceEnd(getClass().getName(),\"MyTestA.foo\",SNIFFER.getTestName(),0,"+ CloverNames.namespace("p")+","+ CloverNames.namespace("t")+");}}private void  RECORDER(){RECORDER.inc(0);RECORDER.inc(1);Stream.of(1, 2, 3, 4, 5, 6).map((i) -> {RECORDER.inc(2);return \"String:\" + String.valueOf(i);});} }" ],
                [ "public class MyTestA { @Test public void foo(){map((int i) -> /*CLOVER:VOID*/ System.out.println(i));} }",
                  "public class MyTestA {" + snifferField + " @Test public void foo(){RECORDER.globalSliceStart(getClass().getName(),0);int " + CloverNames.namespace("p") + "=0;java.lang.Throwable " + CloverNames.namespace("t") + "=null;try{RECORDER();"+ CloverNames.namespace("p")+"=1;}catch(java.lang.Throwable "+ CloverNames.namespace("t2")+"){if("+ CloverNames.namespace("p")+"==0&&"+ CloverNames.namespace("t")+"==null){"+ CloverNames.namespace("t")+"="+ CloverNames.namespace("t2")+";}RECORDER.rethrow("+ CloverNames.namespace("t2")+");}finally{RECORDER.globalSliceEnd(getClass().getName(),\"MyTestA.foo\",SNIFFER.getTestName(),0,"+ CloverNames.namespace("p")+","+ CloverNames.namespace("t")+");}}private void  RECORDER(){RECORDER.inc(0);RECORDER.inc(1);map((int i) -> /*CLOVER:VOID*/ {RECORDER.inc(2);System.out.println(i);});} }" ],
                [ "public class MyTestA { @Test public void foo(){ map((i) -> /** CLOVER:VOID */ System.out.println(\"String:\" + String.valueOf(i))); }}",
                  "public class MyTestA {" + snifferField + " @Test public void foo(){RECORDER.globalSliceStart(getClass().getName(),0);int " + CloverNames.namespace("p") + "=0;java.lang.Throwable " + CloverNames.namespace("t") + "=null;try{RECORDER();"+ CloverNames.namespace("p")+"=1;}catch(java.lang.Throwable "+ CloverNames.namespace("t2")+"){if("+ CloverNames.namespace("p")+"==0&&"+ CloverNames.namespace("t")+"==null){"+ CloverNames.namespace("t")+"="+ CloverNames.namespace("t2")+";}RECORDER.rethrow("+ CloverNames.namespace("t2")+");}finally{RECORDER.globalSliceEnd(getClass().getName(),\"MyTestA.foo\",SNIFFER.getTestName(),0,"+ CloverNames.namespace("p")+","+ CloverNames.namespace("t")+");}}private void  RECORDER(){RECORDER.inc(0); RECORDER.inc(1);map((i) -> /** CLOVER:VOID */ {RECORDER.inc(2);System.out.println(\"String:\" + String.valueOf(i));}); }}" ],
                [ "public class MyTestA { @Test public void foo(){ map((i) -> /*                       \t\n\r CLOVER:VOID */ System.out.println(\"String:\" + String.valueOf(i))); }}",
                  "public class MyTestA {" + snifferField + " @Test public void foo(){RECORDER.globalSliceStart(getClass().getName(),0);int " + CloverNames.namespace("p") + "=0;java.lang.Throwable " + CloverNames.namespace("t") + "=null;try{RECORDER();"+ CloverNames.namespace("p")+"=1;}catch(java.lang.Throwable "+ CloverNames.namespace("t2")+"){if("+ CloverNames.namespace("p")+"==0&&"+ CloverNames.namespace("t")+"==null){"+ CloverNames.namespace("t")+"="+ CloverNames.namespace("t2")+";}RECORDER.rethrow("+ CloverNames.namespace("t2")+");}finally{RECORDER.globalSliceEnd(getClass().getName(),\"MyTestA.foo\",SNIFFER.getTestName(),0,"+ CloverNames.namespace("p")+","+ CloverNames.namespace("t")+");}}private void  RECORDER(){RECORDER.inc(0); RECORDER.inc(1);map((i) -> /*                       \t\n\r CLOVER:VOID */ {RECORDER.inc(2);System.out.println(\"String:\" + String.valueOf(i));}); }}" ],
                [ "public class MyTestA { @Test public void foo(){ map((i) -> ///CLOVER:VOID\n System.out.println(\"String:\" + String.valueOf(i))); }}",
                  "public class MyTestA {" + snifferField + " @Test public void foo(){RECORDER.globalSliceStart(getClass().getName(),0);int " + CloverNames.namespace("p") + "=0;java.lang.Throwable " + CloverNames.namespace("t") + "=null;try{RECORDER();"+ CloverNames.namespace("p")+"=1;}catch(java.lang.Throwable "+ CloverNames.namespace("t2")+"){if("+ CloverNames.namespace("p")+"==0&&"+ CloverNames.namespace("t")+"==null){"+ CloverNames.namespace("t")+"="+ CloverNames.namespace("t2")+";}RECORDER.rethrow("+ CloverNames.namespace("t2")+");}finally{RECORDER.globalSliceEnd(getClass().getName(),\"MyTestA.foo\",SNIFFER.getTestName(),0,"+ CloverNames.namespace("p")+","+ CloverNames.namespace("t")+");}}private void  RECORDER(){RECORDER.inc(0); RECORDER.inc(1);map((i) -> ///CLOVER:VOID\n {RECORDER.inc(2);System.out.println(\"String:\" + String.valueOf(i));}); }}" ],
                [ "public class MyTestA { @Test public void foo(){ map((i) -> // /CLOVER:VOID\n System.out.println(\"String:\" + String.valueOf(i))); }}",
                  "public class MyTestA {" + snifferField + " @Test public void foo(){RECORDER.globalSliceStart(getClass().getName(),0);int " + CloverNames.namespace("p") + "=0;java.lang.Throwable " + CloverNames.namespace("t") + "=null;try{RECORDER();"+ CloverNames.namespace("p")+"=1;}catch(java.lang.Throwable "+ CloverNames.namespace("t2")+"){if("+ CloverNames.namespace("p")+"==0&&"+ CloverNames.namespace("t")+"==null){"+ CloverNames.namespace("t")+"="+ CloverNames.namespace("t2")+";}RECORDER.rethrow("+ CloverNames.namespace("t2")+");}finally{RECORDER.globalSliceEnd(getClass().getName(),\"MyTestA.foo\",SNIFFER.getTestName(),0,"+ CloverNames.namespace("p")+","+ CloverNames.namespace("t")+");}}private void  RECORDER(){RECORDER.inc(0); RECORDER.inc(1);map((i) -> // /CLOVER:VOID\n {RECORDER.inc(2);System.out.println(\"String:\" + String.valueOf(i));}); }}" ],
        ] as String[][])
    }
    
    @Test
    public void testCloverOnOff() throws Exception {
        checkInstrumentation([
                ["/*///CLOVER:OFF*/ package A; class B { public B() {int i = 0;}}",
                        "/*///CLOVER:OFF*/ package A; class B { public B() {int i = 0;}}"],
                ["package A; /*///CLOVER:OFF*/ class B { public B() {int i = 0;}}",
                        "package A; /*///CLOVER:OFF*/ class B { public B() {int i = 0;}}"],
                ["/*CLOVER:OFF*/ class B { public B() {int i = 0;}}",
                        "/*CLOVER:OFF*/ class B { public B() {int i = 0;}}"],
                 // test second directive prefix, added for eclipse auto-format support
                ["/*// /CLOVER:OFF*/ package A; class B { public B() {int i = 0;}}",
                        "/*// /CLOVER:OFF*/ package A; class B { public B() {int i = 0;}}"],
                ["package A; /*// /CLOVER:OFF*/ class B { public B() {int i = 0;}}",
                        "package A; /*// /CLOVER:OFF*/ class B { public B() {int i = 0;}}"],
                ["/*// /CLOVER:OFF*/ class B { public B() {int i = 0;}}",
                        "/*// /CLOVER:OFF*/ class B { public B() {int i = 0;}}"],

                ["class B { ///CLOVER:OFF\nprivate B() {int i = 0;}}",
                        "class B { ///CLOVER:OFF\nprivate B() {int i = 0;}}"],
                ["class B { ///CLOVER:OFF\nprivate B() {}}",
                        "class B { ///CLOVER:OFF\nprivate B() {}}"],
                ["class B { ///CLOVER:OFF\nprivate B() {}\n///CLOVER:ON\n}",
                        "class B { ///CLOVER:OFF\nprivate B() {}\n///CLOVER:ON\n}"],
                ["class B { private B() {///CLOVER:OFF\n}\n///CLOVER:ON\n}",
                        "class B {" + snifferField + " private B() {RECORDER.inc(0);///CLOVER:OFF\n}\n///CLOVER:ON\n}"],
                ["class B { private B() {///CLOVER:OFF\nint i = 0;///CLOVER:ON\n}}",
                        "class B {" + snifferField + " private B() {RECORDER.inc(0);///CLOVER:OFF\nint i = 0;///CLOVER:ON\n}}"],
                ["class B { private B() {///CLOVER:OFF\nhashCode();///CLOVER:ON\n}}",
                        "class B {" + snifferField + " private B() {RECORDER.inc(0);///CLOVER:OFF\nhashCode();///CLOVER:ON\n}}"],

        ] as String[][])

        checkInstrumentation([
                ["/*///CLOVER:OFF*/ public class MyTest { @Test public void foo(){} }",
                        "/*///CLOVER:OFF*/ public class MyTest { @Test public void foo(){} }"],
                ["public class MyTest {/*///CLOVER:OFF*/ @Test public void foo(){} }",
                        "public class MyTest {/*///CLOVER:OFF*/ @Test public void foo(){} }"],
                ["public class MyTest { @Test public void foo()/*///CLOVER:OFF*/{} }",
                        "public class MyTest { @Test public void foo()/*///CLOVER:OFF*/{} }"],
                ["/*///CLOVER:OFF*/ public class MyTest { public void testFoo(){} }",
                        "/*///CLOVER:OFF*/ public class MyTest { public void testFoo(){} }"],
                ["public class MyTest {/*///CLOVER:OFF*/ public void testFoo(){} }",
                        "public class MyTest {/*///CLOVER:OFF*/ public void testFoo(){} }"],
                ["public class MyTest { public void testFoo()/*///CLOVER:OFF*/{} }",
                        "public class MyTest { public void testFoo()/*///CLOVER:OFF*/{} }"]
        ] as String[][])
    }

    @Test
    public void testMethodMetrics() throws Exception {
        checkMethodMetrics("void A() {}",0, 0, 1)
        checkMethodMetrics("void A() {a();}",1, 0, 1)
        checkMethodMetrics("void A() {a = (6 < 7);}",1 ,0 ,1)
        checkMethodMetrics("void A() {a();b();c();}",3, 0, 1)

        // if
        checkMethodMetrics("void A() {if (a()) b(); else c();}",3, 2, 2)
        checkMethodMetrics("void A() {if (a() || b()) c();}", 2, 2, 3)
        checkMethodMetrics("void A() {if (1 + 2 == 4) c();}", 2, 0, 1)

        // for
        checkMethodMetrics("void A() {for (;a();) b(); }",2, 2, 2)
        checkMethodMetrics("void A() {for (;a() || b();) c();}", 2, 2, 3)
        checkMethodMetrics("void A() {for (;1 + 2 == 4;) c();}", 2, 0, 1)

        // while
        checkMethodMetrics("void A() {while (a()) b();}",2, 2, 2)
        checkMethodMetrics("void A() {while (a() || b()) c();}", 2, 2, 3)
        checkMethodMetrics("void A() {while (1 + 2 == 4) c();}", 2, 0, 1)

        // switch
        checkMethodMetrics("void A() {switch (a()) { case 1: b();}}", 3, 0, 2)
        checkMethodMetrics("void A() {switch (a()) { case 1: b(); case 2: c();}}", 5, 0, 3)

        // ternary
        checkMethodMetrics("void A() {a() ? 1 : 2;}", 1, 2, 2)
        checkMethodMetrics("void A() {a() || b()? 1 : 2;}", 1, 2, 3)
        checkMethodMetrics("void A() {a() ? b() ? c()? 1 : 2 : 3 : 4;}", 1, 6, 4)

        // nested functions
    }

    @Test
    public void testLambdaMetrics() throws Exception {
        Clover2Registry registry

        // empty lambda
        registry = checkMetrics(getInstrConfig(newDbTempFile().getAbsolutePath(), false, true, true),
                "class A{void A() { Runnable r = () -> {  }; }}", 1, 2, 1, 0, 2)
        assertEquals(0, getLambda(registry).getStatements().size())

        // block lambda with one statement
        registry = checkMetrics(getInstrConfig(newDbTempFile().getAbsolutePath(), false, true, true),
                "class A{void A() { Runnable r = () -> { return; }; }}", 1, 2, 2, 0, 2)
        assertEquals(1, getLambda(registry).getStatements().size())
        assertSourceRegion(new FixedSourceRegion(1, 41, 1, 48), getLambda(registry).getStatements().get(0))

        // expression lambda with one statement
        registry = checkMetrics(getInstrConfig(newDbTempFile().getAbsolutePath(), false, true, true),
                "class A{void A() { Produce<Integer> i = (x) -> 123; }}", 1, 2, 2, 0, 2)
        assertEquals(1, getLambda(registry).getStatements().size())
        assertSourceRegion(new FixedSourceRegion(1, 48, 1, 51), getLambda(registry).getStatements().get(0))

        // expression lambda with one statement and a branch condition
        registry = checkMetrics(getInstrConfig(newDbTempFile().getAbsolutePath(), false, true, true),
                "class A{void A() { Produce<Integer> i = (x) -> x < 0 ? x * x : -x; }}", 1, 2, 2, 2, 3)
        assertEquals(1, getLambda(registry).getStatements().size())
        assertEquals(1, getLambda(registry).getBranches().size())
        assertSourceRegion(new FixedSourceRegion(1, 48, 1, 66), getLambda(registry).getStatements().get(0))

        // lambda inside a lambda
        registry = checkMetrics(getInstrConfig(newDbTempFile().getAbsolutePath(), false, true, true),
                "class A{void A() { Callable<Runnable> call = () -> () -> { return; }; }}", 1, 3, 3, 0, 3)
        assertEquals(1, getLambda(registry).getStatements().size());                     // outer lambda
        assertEquals(1, getLambda(registry).getMethods().size());                        // outer lambda
        assertEquals(1, getLambda(registry).getMethods().get(0).getStatements().size()); // inner lambda
        assertSourceRegion(
                new FixedSourceRegion(1, 52, 1, 69),
                getLambda(registry).getStatements().get(0)); // outer is "() -> { return; }
        assertSourceRegion(
                new FixedSourceRegion(1, 60, 1, 67),
                getLambda(registry).getMethods().get(0).getStatements().get(0)); // inner is "return;"

        // method reference
        registry = checkMetrics(getInstrConfig(newDbTempFile().getAbsolutePath(), false, true, true),
                "class A{void A() { Integer i = Math::abs; }}", 1, 2, 2, 0, 2)
        assertEquals(1, getLambda(registry).getStatements().size())
        assertSourceRegion(
                new FixedSourceRegion(1, 32, 1, 41),
                getLambda(registry).getStatements().get(0)); // "Math::abs"
    }

    private FullMethodInfo getLambda(Clover2Registry registry) {
        return (FullMethodInfo) registry.getProject().findClass("A").getMethods().get(0).getMethods().get(0)
    }

    private void assertSourceRegion(FixedSourceRegion fixedSourceRegion, StatementInfo statementInfo) {
        assertEquals(fixedSourceRegion.getStartLine(), statementInfo.getStartLine())
        assertEquals(fixedSourceRegion.getStartColumn(), statementInfo.getStartColumn())
        assertEquals(fixedSourceRegion.getEndLine(), statementInfo.getEndLine())
        assertEquals(fixedSourceRegion.getEndColumn(), statementInfo.getEndColumn())
    }

    @Test
    public void testThreadedFlushing() throws Exception {
        JavaInstrumentationConfig config = getInstrConfig(newDbTempFile().getAbsolutePath(), false, false, false)
        config.setFlushPolicy(InstrumentationConfig.THREADED_FLUSHING)
        checkInstrumentation([
                ["class B { public B(int arg) {int i = 0;}}",
                 "class B {" + snifferField + " public B(int arg) {try{RECORDER.inc(0);RECORDER.inc(1);int i = 0;}finally{RECORDER.flushNeeded();}}}"],
            ] as String[][],
            config)
    }

    @Test
    public void testIntervalFlushing() throws Exception {
        JavaInstrumentationConfig config = getInstrConfig(newDbTempFile().getAbsolutePath(), false, false, false)
        config.setFlushPolicy(InstrumentationConfig.INTERVAL_FLUSHING)
        checkInstrumentation([
                ["class B { public B(int arg) {}}",
                 "class B {" + snifferField + " public B(int arg) {try{RECORDER.inc(0);}finally{RECORDER.maybeFlush();}}}"],
            ] as String[][],
            config)

    }

    // tests that "empty" classes don't get a recorder member when they don't need one
    @Test
    public void testDirtyDetection() throws Exception {
        final String recMember = "<REC_MEMBER>"
        checkInstrumentation(recMember, [
                ["class A {}", "class A {}"],
                ["interface A {}", "interface A {}"],
                ["@interface A {}", "@interface A {}"],
                ["enum A {}", "enum A {}"],
                ["enum A { apple,banana,pear }", "enum A { apple,banana,pear }"],
                ["enum A { apple,banana,pear }", "enum A { apple,banana,pear }"],
                // second top-level class.
                ["class A {public A(){}} class B {}","class A {" + recMember + snifferField + "public A(){RECORDER.inc(0);}} class B {}"]
        ] as String[][], true)
    }

    @Test
    public void testDoubleInstrDetection() throws Exception {
        checkInstrumentation([
                [ "class A {}", "class A {}" ], //input is smaller than the marker
                [ "", "" ] // empty source file
        ] as String[][]);
        try {
            checkInstrumentation([
                    [ CloverTokenStreamFilter.MARKER +"*/" ]
            ] as String[][])
            fail("instrumentation marker not detected")
        }
        catch (CloverException e) {

        }
    }

    @Test
    public void testSwitchCaseStatementPositions() throws Exception {

        Clover2Registry registry = performInstrumentation("class A { A() {/*1*/\nswitch(i) {/*2*/\ncase 1:return;/*3*/\ncase 2:return;/*4*/\ndefault:break;/*5*/\n}\n}}")

        FullProjectInfo proj = registry.getProject()
        ClassInfo c = proj.findClass("A")

        for (com.atlassian.clover.api.registry.StatementInfo info : c.getMethods().get(0).getStatements()) {
            assertTrue(info.getStartLine() <= info.getEndLine())
            assertTrue(info.getStartColumn() < info.getEndColumn())
        }
    }

    @Test
    public void testDefaultInitString() throws Exception {
        final File dbDir = new File(workingDir, InstrumentationConfig.DEFAULT_DB_DIR)
        final File dbFile = new File(dbDir, InstrumentationConfig.DEFAULT_DB_FILE)


        String defaultInitStr = dbFile.getAbsolutePath()

        String out = getInstrumentedVersion(null, false, "class B { B(int arg) {}}")

        String expectedCharArray = RecorderInstrEmitter.asUnicodeString(defaultInitStr)

        assertTrue(out.contains(expectedCharArray))
    }

    @Test
    public void testRelativeDefaultInitString() throws Exception {
        String defaultRelInitStr = InstrumentationConfig.DEFAULT_DB_DIR + File.separator + InstrumentationConfig.DEFAULT_DB_FILE


        String out = getInstrumentedVersion(null, true, "class B { B(int arg) {}}")
        String expectedCharArray = RecorderInstrEmitter.asUnicodeString(defaultRelInitStr)

        assertTrue(out.contains(expectedCharArray))
        File file = new File(defaultRelInitStr)
        assertTrue("Could not delete file referenced by default initstring: " + file.getAbsolutePath(), file.delete())
    }

    @Test
    public void testRelativeInitString() throws Exception {
        File coverageDbFile = File.createTempFile(getClass().getName() +"." + name, ".tmp", workingDir)
        coverageDbFile.delete()

        String out = getInstrumentedVersion(coverageDbFile.getPath(), true, "class B { B(int arg) {}}")

        String expectedCharArray = RecorderInstrEmitter.asUnicodeString(coverageDbFile.getPath())

        assertTrue(out.contains(expectedCharArray))
    }

    @Test
    public void testInitString() throws Exception {
        File coverageDbFile = File.createTempFile(getClass().getName() +"." + name, ".tmp", workingDir)
        coverageDbFile.delete()

        String out = getInstrumentedVersion(coverageDbFile.getAbsolutePath(), false, "class B { B(int arg) {}}")

        String expectedCharArray = RecorderInstrEmitter.asUnicodeString(coverageDbFile.getAbsolutePath())

        assertTrue(out.contains(expectedCharArray))
    }

    private void checkStatement(String test, String expected, int instrumentationlevel) throws Exception {
        JavaInstrumentationConfig config = getInstrConfig(newDbTempFile().getAbsolutePath(), false, true, false)
        config.setInstrLevel(instrumentationlevel)
        checkInstrumentation([
            ["class B { private void a(int arg) {" + test + "}}",
             "class B {" + snifferField + " private void a(int arg) {RECORDER.inc(0);" + expected + "}}"]
        ] as String[][], config)
    }

    private void checkStatement(String test, String expected) throws Exception {
        checkStatement(test, expected, InstrumentationLevel.STATEMENT.ordinal())
    }

    // array of {input, expected output} - to prevent the prefix of the string being compared,
    // mark the start point for comparison with ST_POINT
    private void checkInstrumentation(String[][] testcases, boolean testRewriting) throws Exception {
        checkInstrumentation("", testcases, testRewriting)
    }

    private void checkInstrumentation(String[][] testcases) throws Exception {
        checkInstrumentation("", testcases, true)
    }

    // check array of {input, expected output} - replacing the recorder member with recorderStr.
    private void checkInstrumentation(String recorderStr, String[][] testcases, boolean testRewriting) throws Exception {
        for (String[] testcase : testcases) {
            String instr = getInstrumentedVersion(testcase[0], testRewriting)
            checkStringSuffix(recorderStr, CloverTokenStreamFilter.MARKER + testcase[1], instr)
        }
    }

    // check array of {input, expected output} - replacing the recorder member with recorderStr.
    private void checkInstrumentation(String[][] testcases, JavaInstrumentationConfig config) throws Exception {
        for (String[] testcase : testcases) {
            String instr = getInstrumentedVersion(testcase[0], config)
            checkStringSuffix("", CloverTokenStreamFilter.MARKER + testcase[1], instr)
        }
    }

    private static final String SNIFFER_REGEX = CloverNames.CLOVER_PREFIX + "[_0-9]+_TEST_NAME_SNIFFER"
    private static final String RECORDER_REGEX = CloverNames.CLOVER_PREFIX + "[_A-Za-z0-9]+"
    private static final String CLR_REGEX = CloverNames.CLOVER_PREFIX
    private static final String RECORDER_INNER_MEMBER_REGEX = "public static " + CoverageRecorder.class.getName() + " " + RECORDER_REGEX + "=[^;]+;"

    private void checkStringSuffix(String recorder, String s1, String s2) {
        String t2 = s2.replaceAll(SNIFFER_REGEX, "SNIFFER")
                      .replaceAll(RECORDER_INNER_MEMBER_REGEX, recorder)
                      .replaceAll(RECORDER_REGEX, "RECORDER")
                      .replaceAll(CLR_REGEX, "CLR")
        assertThat(t2, equalTo(s1))
    }

    private String getInstrumentedVersion(String input, boolean testRewriting) throws Exception {
        File coverageDbFile = newDbTempFile()
        coverageDbFile.delete()
        return getInstrumentedVersion(coverageDbFile.getAbsolutePath(), false, input, testRewriting)
    }

    private File newDbTempFile() throws IOException {
        File tempFile = File.createTempFile(getClass().getName() + "." + name, ".tmp", workingDir)
        tempFile.delete()
        return tempFile
    }

    private String getInstrumentedVersion(String initString, boolean relativeIS, String input) throws Exception {
        return getInstrumentedVersion(initString, relativeIS, input, true)
    }

    private String getInstrumentedVersion(String initString, boolean relativeIS, String input, boolean testRewriting) throws Exception {
        JavaInstrumentationConfig cfg = getInstrConfig(initString, relativeIS, testRewriting, false)
        return getInstrumentedVersion(input, cfg)
    }

    private String getInstrumentedVersion(final String sourceCode, final JavaInstrumentationConfig cfg) throws Exception {
        final File tempFile = newDbTempFile()
        final StringWriter out = new StringWriter()
        final InstrumentationSource input = new StringInstrumentationSource(tempFile, sourceCode)

        performInstrumentation(cfg, input, out)
        tempFile.delete()

        return out.toString()
    }

    private JavaInstrumentationConfig getInstrConfig(String initString, boolean relativeIS, boolean testRewriting, boolean classInstrStrategy) {
        JavaInstrumentationConfig cfg = new JavaInstrumentationConfig()
        cfg.setDefaultBaseDir(workingDir)
        if (initString != null) {
            cfg.setInitstring(initString)
        }
        cfg.setRelative(relativeIS)
        cfg.setProjectName(name.toString())
        cfg.setClassInstrStragegy(classInstrStrategy)
        cfg.setSourceLevel(SourceLevel.JAVA_8)
        cfg.setReportInitErrors(false)
        cfg.setRecordTestResults(testRewriting)
        cfg.setEncoding("ISO-88591")
        cfg.setInstrumentLambda(LambdaInstrumentation.ALL)
        return cfg
    }


    private void checkMethodMetrics(String methodSrc,int numStatements, int numBranches, int methodComplexity) throws Exception {
        checkMetrics("class A{"+methodSrc+"}", 1, 1, numStatements, numBranches, methodComplexity)
    }

    private void checkMetrics(String src, int numClasses, int numMethods, int numStatements, int numBranches, int totalComplexity) throws Exception {
        checkMetrics(getInstrConfig(newDbTempFile().getAbsolutePath(), false, true, false),
                src, numClasses, numMethods, numStatements, numBranches, totalComplexity)
    }

    private Clover2Registry checkMetrics(JavaInstrumentationConfig instrConfig, String src, int numClasses, int numMethods, int numStatements, int numBranches, int totalComplexity) throws Exception {
        final InstrumentationSource input = new StringInstrumentationSource(newDbTempFile(), src)
        final Clover2Registry registry = performInstrumentation(instrConfig,
                input, new StringWriter())
        final ProjectMetrics pm = (ProjectMetrics) registry.getProject().getMetrics()

        assertEquals("num classes",numClasses, pm.getNumClasses())
        assertEquals("num methods",numMethods, pm.getNumMethods())
        assertEquals("num statements", numStatements, pm.getNumStatements())
        assertEquals("num branches", numBranches, pm.getNumBranches())
        assertEquals("total complexity", totalComplexity, pm.getComplexity())

        return registry
    }

    /**
     * convenience method that instruments but discards the instrumentation output
     */
    private Clover2Registry performInstrumentation(final String sourceCode) throws Exception {
        final InstrumentationSource input = new StringInstrumentationSource(newDbTempFile(), sourceCode)
        return performInstrumentation(getInstrConfig(null, false, true, false), input, new StringWriter())
    }

    private Clover2Registry performInstrumentation(final JavaInstrumentationConfig cfg, final InstrumentationSource input,
                                                   final Writer out) throws Exception {
        final Instrumenter instr = new Instrumenter(cfg)
        instr.startInstrumentation()
        instr.instrument(input, out, null)
        return instr.endInstrumentation()
    }
}
