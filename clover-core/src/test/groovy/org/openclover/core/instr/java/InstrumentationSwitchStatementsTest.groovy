package org.openclover.core.instr.java

import com.atlassian.clover.api.registry.ClassInfo
import com.atlassian.clover.registry.Clover2Registry
import com.atlassian.clover.registry.entities.FullProjectInfo
import org_openclover_runtime.CloverVersionInfo
import org.junit.Test

import static org.junit.Assert.assertTrue

class InstrumentationSwitchStatementsTest extends InstrumentationTestBase {

    @Test
    void testSwitchStatement() throws Exception {
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
    void testSwitchCaseStatementPositions() throws Exception {

        Clover2Registry registry = performInstrumentation("class A { A() {/*1*/\nswitch(i) {/*2*/\ncase 1:return;/*3*/\ncase 2:return;/*4*/\ndefault:break;/*5*/\n}\n}}")

        FullProjectInfo proj = registry.getProject()
        ClassInfo c = proj.findClass("A")

        for (com.atlassian.clover.api.registry.StatementInfo info : c.getMethods().get(0).getStatements()) {
            assertTrue(info.getStartLine() <= info.getEndLine())
            assertTrue(info.getStartColumn() < info.getEndColumn())
        }
    }
}
