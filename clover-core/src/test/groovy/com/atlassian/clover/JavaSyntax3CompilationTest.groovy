package com.atlassian.clover

import com.atlassian.clover.registry.entities.FullClassInfo
import com.atlassian.clover.registry.entities.FullFileInfo
import com.atlassian.clover.registry.entities.LineInfo
import com.atlassian.clover.registry.entities.FullProjectInfo
import org.apache.tools.ant.util.JavaEnvUtils
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull

/**
 * The purpose of this test is to
 * a) make sure the code compiles under JDK1.3 or later
 * b) make sure that when that code is instrumented, it still compiles
 */
class JavaSyntax3CompilationTest extends JavaSyntaxCompilationTestBase {

    @Before
    void setUp() {
        setUpProject()
    }

    @Test
    void testCompilation_13() {
        final File srcDir = new File(mTestcasesSrcDir, "javasyntax1.3")
        compileSources(srcDir, JavaEnvUtils.JAVA_1_8)
    }

    /**
     * Test java 1.3 language features and how Clover handles them.
     * @throws Exception
     */
    @Test
    void testInstrumentationAndCompilation_13() throws Exception {
        final File srcDir = new File(mTestcasesSrcDir, "javasyntax1.3")
        instrumentAndCompileSources(srcDir, JavaEnvUtils.JAVA_1_8)

        // execute instrumented code
        String[] testCaseMainClasses = [ "simple.ALittleOfEverything" ]
        executeMainClasses(testCaseMainClasses)

        // assert metrics
        assertMethodCoverage("simple.ALittleOfEverything", 8, 1)
        assertMethodCoverage("simple.ALittleOfEverything", 16)
        assertStatementCoverage("simple.ALittleOfEverything", 12, 1)
    }

    @Test
    void testColumnAlignment() throws Exception {
        final File srcDir = new File(mTestcasesSrcDir, "javasyntax1.3")
        instrumentAndCompileSources(srcDir, JavaEnvUtils.JAVA_1_8)

        String[] testCaseMainClasses = [ "simple.ColumnAlignment" ]
        executeMainClasses(testCaseMainClasses)
        FullProjectInfo model = getModel()
        FullClassInfo c = (FullClassInfo)model.findClass(testCaseMainClasses[0])
        assertNotNull("no such class " + testCaseMainClasses[0], testCaseMainClasses[0])

        //Some of these start/ends are rather unexpected - documenting this as a starting point
        //for future modifications

        FullFileInfo fi = (FullFileInfo)c.getContainingFile()
        LineInfo[] li = fi.getLineInfo(false, false)
        //                 A..............
        //6: ^class ColumnAlignment$
        //    B
        //22:^} $
        assertEquals(14, li[6].getClassStarts()[0].getStartColumn())
        assertEquals(22, li[6].getClassStarts()[0].getEndLine())
        assertEquals(2, li[6].getClassStarts()[0].getEndColumn())

        //                           A....
        //8: ^    private static class Inner$
        //    ...B
        //11:^    }$
        assertEquals(23, li[8].getClassStarts()[0].getStartColumn())
        assertEquals(11, li[8].getClassStarts()[0].getEndLine())
        assertEquals(3, li[8].getClassStarts()[0].getEndColumn())

        //            A.....B
        //10:^        { int i; i = 0; }$
        assertEquals(5, li[10].getStatements()[0].getStartColumn())
        assertEquals(10, li[10].getStatements()[0].getEndLine())
        assertEquals(11, li[10].getStatements()[0].getEndColumn())

        //                   A.....B
        //10:^        { int i; i = 0; }$
        assertEquals(12, li[10].getStatements()[1].getStartColumn())
        assertEquals(10, li[10].getStatements()[1].getEndLine())
        assertEquals(18, li[10].getStatements()[1].getEndColumn())

        //      A.....................................
        //13:^    static void main(String[] args)$
        //    ...B
        //21:^    }$
        assertEquals(2, li[13].getMethodStarts()[0].getStartColumn())
        assertEquals(21, li[13].getMethodStarts()[0].getEndLine())
        assertEquals(3, li[13].getMethodStarts()[0].getEndColumn())

        //          A..........B
        //15:^        int p = 10;$
        assertEquals(3, li[15].getStatements()[0].getStartColumn())
        assertEquals(15, li[15].getStatements()[0].getEndLine())
        assertEquals(14, li[15].getStatements()[0].getEndColumn())

        //              A...........B
        //16:^        if (p % 2 == 0) {$
        assertEquals(6, li[16].getBranches()[0].getStartColumn())
        assertEquals(16, li[16].getBranches()[0].getEndLine())
        assertEquals(18, li[16].getBranches()[0].getEndColumn())

        //              A...B
        //17:^            p++
        assertEquals(4, li[17].getStatements()[0].getStartColumn())
        assertEquals(17, li[17].getStatements()[0].getEndLine())
        assertEquals(8, li[17].getStatements()[0].getEndColumn())

        //              A...B
        //19:^            p--
        assertEquals(4, li[19].getStatements()[0].getStartColumn())
        assertEquals(19, li[19].getStatements()[0].getEndLine())
        assertEquals(8, li[19].getStatements()[0].getEndColumn())
    }

}
