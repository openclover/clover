package org.openclover.core.instr.java

import org.junit.Test
import org.openclover.core.api.registry.ClassInfo
import org.openclover.core.api.registry.MethodInfo
import org.openclover.core.api.registry.ProjectInfo
import org.openclover.core.api.registry.StatementInfo
import org.openclover.core.cfg.instr.java.SourceLevel
import org.openclover.core.registry.Clover2Registry
import org.openclover.core.registry.metrics.ProjectMetrics
import org.openclover.core.api.registry.HasMetricsFilter

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

/**
 * Validates the coverage-model (registry) content produced for a JEP 512 compact source file: the
 * synthesized implicit class, its name/package/source range, and that every top-level member is
 * registered under it - none of which the end-to-end JavaSyntax25CompilationTest asserts.
 */
class InstrumentationCompactSourceFileModelTest extends InstrumentationTestBase {

    /**
     * A single compact source file exercised end-to-end at the model level: a field, a helper method
     * and an instance main(). One sample, many assertions - the whole point of a compact file is that
     * all these top-level members collapse into one implicit class.
     */
    @Test
    void testImplicitClassModel() throws Exception {
        final String src =
                "String greeting = \"Hello\";\n" +          // line 1 - field, first member
                "int doubled(int n) {\n" +                  // line 2
                "    return n * 2;\n" +                      // line 3
                "}\n" +                                      // line 4
                "void main() {\n" +                          // line 5
                "    System.out.println(greeting);\n" +      // line 6
                "    System.out.println(doubled(21));\n" +   // line 7
                "}\n"                                         // line 8 - last member end

        final Clover2Registry registry = instrumentToRegistry("Widget.java", src, SourceLevel.JAVA_25)
        final ProjectInfo project = registry.getProject()

        // exactly one class is registered, and it IS the implicit class
        final List<ClassInfo> classes = project.getClasses(HasMetricsFilter.ACCEPT_ALL)
        assertEquals("one implicit class only", 1, classes.size())
        final ClassInfo implicitClass = classes.get(0)

        // name is derived from the file base name (Widget.java -> Widget), unnamed package, ordinary class
        assertEquals("Widget", implicitClass.getName())
        assertEquals(implicitClass, project.findClass("Widget"))
        assertTrue("implicit class is in the unnamed package", implicitClass.getPackage().isDefault())
        assertFalse(implicitClass.isInterface())
        assertFalse(implicitClass.isEnum())
        assertFalse(implicitClass.isAnnotationType())

        // source range: starts at the first member and spans the whole file
        assertEquals("class starts at first member line", 1, implicitClass.getStartLine())
        assertTrue("class start col is sane", implicitClass.getStartColumn() >= 1)
        assertEquals("class ends at last member end line", 8, implicitClass.getEndLine())
        assertTrue("class region is well-formed", implicitClass.getStartLine() <= implicitClass.getEndLine())

        // both top-level methods belong to the implicit class (and nothing is orphaned)
        final List<MethodInfo> methods = implicitClass.getMethods()
        assertEquals("two top-level methods", 2, methods.size())
        final Map<String, MethodInfo> byName = methods.collectEntries { [(it.getSignature().getName()): it] }
        assertTrue("doubled() registered", byName.containsKey("doubled"))
        assertTrue("main() registered", byName.containsKey("main"))

        // method regions & statement counts
        final MethodInfo doubled = byName.get("doubled")
        final MethodInfo main = byName.get("main")
        assertEquals("doubled has one statement", 1, doubled.getStatements().size())
        assertEquals("main has two statements", 2, main.getStatements().size())
        for (MethodInfo m : methods) {
            assertTrue("method region well-formed", m.getStartLine() <= m.getEndLine())
            for (StatementInfo s : m.getStatements()) {
                assertTrue("stmt region well-formed", s.getStartLine() <= s.getEndLine())
            }
        }
    }

    /**
     * The implicit class name mirrors javac: the file name minus the ".java" extension, verbatim
     * (javac rejects a base name that is not a legal identifier, so no mangling is applied).
     */
    @Test
    void testImplicitClassNameDerivedFromDifferentFileNames() throws Exception {
        final String src = "void main() { System.out.println(\"hi\"); }\n"

        assertEquals("Foo", instrumentToRegistry("Foo.java", src, SourceLevel.JAVA_25)
                .getProject().getClasses(HasMetricsFilter.ACCEPT_ALL).get(0).getName())
        assertEquals("A\$b_c", instrumentToRegistry("A\$b_c.java", src, SourceLevel.JAVA_25)
                .getProject().getClasses(HasMetricsFilter.ACCEPT_ALL).get(0).getName())
    }

    /**
     * A compact file may mix members with a top-level type declaration (JLS 7.3). The declared type
     * must be a MEMBER class nested under the implicit class, with its own methods.
     */
    @Test
    void testMemberTypeNestedUnderImplicitClass() throws Exception {
        final String src =
                "static String tag(String s) { return \"<\" + s + \">\"; }\n" +
                "void main() { Box b = new Box(2); System.out.println(b.count()); }\n" +
                "class Box {\n" +
                "    private final int n;\n" +
                "    Box(int n) { this.n = n; }\n" +
                "    int count() { return n; }\n" +
                "}\n"

        final Clover2Registry registry = instrumentToRegistry("Outer.java", src, SourceLevel.JAVA_25)
        final ProjectInfo project = registry.getProject()

        // two classes are registered: the implicit 'Outer' and its member class - whose qualified name
        // 'Outer.Box' encodes that it is nested under the implicit class (containment is expressed via
        // the qualified name at instrumentation time)
        final List<ClassInfo> classes = project.getClasses(HasMetricsFilter.ACCEPT_ALL)
        assertEquals("implicit class + member class", 2, classes.size())

        final ClassInfo implicitClass = project.findClass("Outer")
        assertEquals("Outer", implicitClass.getName())
        final ClassInfo box = classes.find { it != implicitClass }
        assertEquals("member class is nested under the implicit class", "Outer.Box", box.getName())

        // the implicit class owns the two top-level methods, but NOT Box's members
        final List<String> implicitMethodNames = implicitClass.getMethods().collect { it.getSignature().getName() }
        assertTrue(implicitMethodNames.contains("tag"))
        assertTrue(implicitMethodNames.contains("main"))
        assertFalse("count() is not a method of the implicit class", implicitMethodNames.contains("count"))

        // the member class owns count()
        final List<String> boxMethodNames = box.getMethods().collect { it.getSignature().getName() }
        assertTrue("member class owns count()", boxMethodNames.contains("count"))
    }

    /**
     * A compact source file and the same members wrapped in an explicit class must produce an
     * identical model shape (class/method/statement/branch counts).
     */
    @Test
    void testCompactFileParityWithExplicitClass() throws Exception {
        final String members =
                "int doubled(int n) { return n * 2; }\n" +
                "int classify(int n) { if (n > 0) return 1; return -1; }\n"

        final String compact = members
        final String explicit = "class Widget {\n" + members + "}\n"

        final ProjectMetrics compactMetrics =
                (ProjectMetrics) instrumentToRegistry("Widget.java", compact, SourceLevel.JAVA_25).getProject().getMetrics()
        final ProjectMetrics explicitMetrics =
                (ProjectMetrics) instrumentToRegistry("Widget.java", explicit, SourceLevel.JAVA_25).getProject().getMetrics()

        assertEquals("num classes", explicitMetrics.getNumClasses(), compactMetrics.getNumClasses())
        assertEquals("num methods", explicitMetrics.getNumMethods(), compactMetrics.getNumMethods())
        assertEquals("num statements", explicitMetrics.getNumStatements(), compactMetrics.getNumStatements())
        assertEquals("num branches", explicitMetrics.getNumBranches(), compactMetrics.getNumBranches())
        assertEquals("complexity", explicitMetrics.getComplexity(), compactMetrics.getComplexity())
    }
}
