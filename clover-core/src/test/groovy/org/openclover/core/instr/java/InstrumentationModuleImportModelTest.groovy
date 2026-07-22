package org.openclover.core.instr.java

import org.junit.Test
import org.openclover.core.cfg.instr.java.SourceLevel
import org.openclover.core.registry.metrics.ProjectMetrics

import static org.junit.Assert.assertEquals

/**
 * Validates that JEP 511 module import declarations are model-neutral: they are not instrumented and
 * must add no classes/methods/statements/branches to the registry, and the ordinary 'module.*'-prefixed
 * import must be treated exactly like any other import.
 */
class InstrumentationModuleImportModelTest extends InstrumentationTestBase {

    // the type declaration whose model must be unaffected by whatever import precedes it
    private static final String BODY = "class C { void m() { int x = 1; if (x > 0) x++; } }\n"

    private void assertSameModelAs(String withImport) throws Exception {
        final ProjectMetrics baseline =
                (ProjectMetrics) instrumentToRegistry("C.java", BODY, SourceLevel.JAVA_25).getProject().getMetrics()
        final ProjectMetrics actual =
                (ProjectMetrics) instrumentToRegistry("C.java", withImport + BODY, SourceLevel.JAVA_25).getProject().getMetrics()

        assertEquals("num classes", baseline.getNumClasses(), actual.getNumClasses())
        assertEquals("num methods", baseline.getNumMethods(), actual.getNumMethods())
        assertEquals("num statements", baseline.getNumStatements(), actual.getNumStatements())
        assertEquals("num branches", baseline.getNumBranches(), actual.getNumBranches())
        assertEquals("complexity", baseline.getComplexity(), actual.getComplexity())
    }

    @Test
    void testModuleImportAddsNoModelElements() throws Exception {
        // JEP 511: 'import module <name>;'
        assertSameModelAs("import module java.base;\n")
    }

    @Test
    void testOrdinaryModulePrefixImportUnaffected() throws Exception {
        // ordinary dotted import whose first segment is the contextual keyword 'module'
        assertSameModelAs("import module.foo.Bar;\n")
    }
}
