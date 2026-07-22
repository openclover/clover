package org.openclover.core.instr.java

import org.junit.Test
import org.openclover.core.api.registry.MethodInfo
import org.openclover.core.cfg.instr.java.SourceLevel
import org.openclover.core.registry.Clover2Registry

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

/**
 * Back-fills the OC-229 (Java 21) pattern-matching work with coverage-model assertions - the existing
 * JavaSyntax21CompilationTest only checks instrumented-source text and runtime output. Here we assert
 * how switch patterns and record deconstruction map onto branches/statements: pattern binding
 * variables and record components add no branches, while a 'when' guard does.
 */
class InstrumentationPatternMatchingModelTest extends InstrumentationTestBase {

    private MethodInfo methodOf(Clover2Registry registry, String className, String methodName) {
        return registry.getProject().findClass(className).getMethods().find { it.getSignature().getName() == methodName }
    }

    /**
     * A switch over patterns with a type-pattern case, a guarded 'when' case and a 'case null'. The
     * binding variable must NOT introduce a branch, but the 'when' guard must - proven by removing the
     * guard from an otherwise identical switch and asserting the branch count drops by exactly one.
     */
    @Test
    void testSwitchPatternModel() throws Exception {
        final String guarded =
                "class S {\n" +
                "    String m(Object o) {\n" +
                "        return switch (o) {\n" +
                "            case Integer i when i > 0 -> \"pos\";\n" +
                "            case Integer i -> \"int\";\n" +
                "            case null -> \"null\";\n" +
                "            default -> \"other\";\n" +
                "        };\n" +
                "    }\n" +
                "}\n"
        final String unguarded =
                "class S {\n" +
                "    String m(Object o) {\n" +
                "        return switch (o) {\n" +
                "            case Integer i -> \"int\";\n" +
                "            case null -> \"null\";\n" +
                "            default -> \"other\";\n" +
                "        };\n" +
                "    }\n" +
                "}\n"

        final MethodInfo guardedM = methodOf(instrumentToRegistry("S.java", guarded, SourceLevel.JAVA_21), "S", "m")
        final MethodInfo unguardedM = methodOf(instrumentToRegistry("S.java", unguarded, SourceLevel.JAVA_21), "S", "m")

        // the only difference between the two sources is the 'when' guard, so it must account for
        // exactly one extra branch - the type-pattern bindings ('Integer i') contribute none
        assertEquals("the 'when' guard adds exactly one branch",
                unguardedM.getBranches().size() + 1, guardedM.getBranches().size())
        assertTrue("guard raises complexity", guardedM.getComplexity() > unguardedM.getComplexity())
    }

    /**
     * A record deconstruction pattern in 'instanceof' must not branch-instrument its components: the
     * only branch is the 'instanceof' itself, identical to a plain type pattern.
     */
    @Test
    void testRecordDeconstructionModel() throws Exception {
        final String recordPattern =
                "class R {\n" +
                "    record Point(int x, int y) {}\n" +
                "    int m(Object o) {\n" +
                "        if (o instanceof Point(int x, int y)) return x + y;\n" +
                "        return -1;\n" +
                "    }\n" +
                "}\n"
        final String simplePattern =
                "class R {\n" +
                "    record Point(int x, int y) {}\n" +
                "    int m(Object o) {\n" +
                "        if (o instanceof Point p) return 1;\n" +
                "        return -1;\n" +
                "    }\n" +
                "}\n"

        final MethodInfo recordM = methodOf(instrumentToRegistry("R.java", recordPattern, SourceLevel.JAVA_21), "R", "m")
        final MethodInfo simpleM = methodOf(instrumentToRegistry("R.java", simplePattern, SourceLevel.JAVA_21), "R", "m")

        // the record deconstruction 'Point(int x, int y)' branches exactly like 'Point p' - components add none
        assertEquals("record components add no branches",
                simpleM.getBranches().size(), recordM.getBranches().size())
        assertEquals("a single instanceof branch", 1, recordM.getBranches().size())
    }
}
