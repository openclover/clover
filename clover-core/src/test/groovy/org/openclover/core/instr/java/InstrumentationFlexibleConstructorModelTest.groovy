package org.openclover.core.instr.java

import org.junit.Test
import org.openclover.core.api.registry.ClassInfo
import org.openclover.core.api.registry.MethodInfo
import org.openclover.core.cfg.instr.java.SourceLevel
import org.openclover.core.registry.metrics.ProjectMetrics

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

/**
 * Validates the coverage-model content for JEP 513 flexible constructor bodies: constructors with a
 * prologue before super()/this() must register the same methods/statements/branches regardless of
 * source level (only the emitted inc() placement differs), and the explicit invocation must be
 * counted exactly once - none of which the end-to-end test asserts.
 */
class InstrumentationFlexibleConstructorModelTest extends InstrumentationTestBase {

    // two constructors: one with a prologue statement before super(), one with a prologue before this()
    private static final String SRC =
            "class P {\n" +
            "    int y;\n" +
            "    P(int x) { if (x < 0) throw new RuntimeException(); super(); this.y = x; }\n" +
            "    P() { int d = 5; this(d); }\n" +
            "}\n"

    @Test
    void testFlexibleConstructorModel() throws Exception {
        final ClassInfo p = instrumentToRegistry("P.java", SRC, SourceLevel.JAVA_25).getProject().findClass("P")

        // both constructors are registered as methods with well-formed regions
        final List<MethodInfo> ctors = p.getMethods()
        assertEquals("two constructors", 2, ctors.size())
        for (MethodInfo m : ctors) {
            assertTrue("ctor region well-formed", m.getStartLine() <= m.getEndLine())
        }

        // identify each ctor by its statement count
        final MethodInfo ctorWithArg = ctors.find { it.getStatements().size() == 4 }
        final MethodInfo ctorNoArg = ctors.find { it.getStatements().size() == 2 }

        // P(int x): 'if' + 'throw' + 'super()' + 'this.y = x' = 4 statements (super() counted ONCE,
        // otherwise this would be 5), and the prologue 'if' contributes exactly one branch
        assertTrue("P(int x) present with 4 statements", ctorWithArg != null)
        assertEquals("if() adds one branch", 1, ctorWithArg.getBranches().size())

        // P(): 'int d = 5' + 'this(d)' = 2 statements (this() counted once), no branches
        assertTrue("P() present with 2 statements", ctorNoArg != null)
        assertEquals("no branches in P()", 0, ctorNoArg.getBranches().size())
    }

    @Test
    void testConstructorModelParityAcrossSourceLevels() throws Exception {
        // the grammar is version-agnostic; only inc() placement differs between <25 and 25+, so the
        // registered model (classes/methods/statements/branches/complexity) must be identical
        final ProjectMetrics at25 =
                (ProjectMetrics) instrumentToRegistry("P.java", SRC, SourceLevel.JAVA_25).getProject().getMetrics()
        final ProjectMetrics at21 =
                (ProjectMetrics) instrumentToRegistry("P.java", SRC, SourceLevel.JAVA_21).getProject().getMetrics()

        assertEquals("num classes", at25.getNumClasses(), at21.getNumClasses())
        assertEquals("num methods", at25.getNumMethods(), at21.getNumMethods())
        assertEquals("num statements", at25.getNumStatements(), at21.getNumStatements())
        assertEquals("num branches", at25.getNumBranches(), at21.getNumBranches())
        assertEquals("complexity", at25.getComplexity(), at21.getComplexity())
    }
}
