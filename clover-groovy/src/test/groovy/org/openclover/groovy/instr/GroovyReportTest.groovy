package org.openclover.groovy.instr

import groovy.transform.CompileStatic
import groovy.xml.XmlParser

@CompileStatic
class GroovyReportTest extends TestBase {

    GroovyReportTest(String testName) {
        super(testName)
    }

    GroovyReportTest(String methodName, String specificName, File groovyAllJar, List<File> additionalGroovyJars) {
        super(methodName, specificName, groovyAllJar, additionalGroovyJars)
    }

    void testReportsOnFourClassTypes() {
        instrumentAndCompileWithGrover(
                ["FooEnum.groovy"      :
                         """
                public enum FooEnum {
                }
              """,
                 "FooClass.groovy"     :
                         """
                public class FooClass {
                  public static void main(String[] args) {}
                }
            """,
                 "FooInterface.groovy" :
                         """
                public interface FooInterface {
                }
            """,
                 "FooAnnotation.groovy":
                         """
                public @interface FooAnnotation {
                }
            """
                ])

        runWithAsserts("FooClass")

        reportAndAssert() { File dir ->
            assertXHTMLFile(dir, "default-pkg/FooEnum.html") &&
                    assertXHTMLFile(dir, "default-pkg/FooClass.html") &&
                    assertXHTMLFile(dir, "default-pkg/FooInterface.html") &&
                    assertXHTMLFile(dir, "default-pkg/FooAnnotation.html") &&
                    true
        }
    }

    void testStillReportOnEnumMethods() {
        instrumentAndCompileWithGrover(
                ["Foo.groovy":
                         """
                public enum Foo {
                    A, B;

                    public static void main(String[] args) {
                        println A
                        println B
                    }
                }
              """])

        runWithAsserts("Foo")

        reportAndAssert() { File dir ->
            assertXHTMLFile(dir, "default-pkg/Foo.html") { Node page ->
                def line5 = ((NodeList) page.depthFirst())["tr"]
                        .find { Object obj ->
                            Node it = (Node) obj
                            println it.attribute('id');
                            it.attribute('id') == 'l5'
                        }

                if (line5 == null) return false

                Node tdCell = (Node) line5["td"]
                        .find { Object obj ->
                            Node it = (Node) obj
                            println it.attribute('class')
                            it.attribute('class') == 'coverageCount Good missedByTest'
                        }
                NodeList spans = (NodeList) tdCell["span"]
                Node firstSpan = (Node) spans[0]
                firstSpan.attribute('title').toString().contains("method entered 1 time.")
            }
        }
    }

    void testReportsOnDollarSlashyStrings() {
        //A 1.8 sanity test to ensure our lexer doesn't break on 1.8 syntax
        instrumentAndCompileWithGrover(
                ["Foo.groovy":
                         """
                def name = 'Michael'
                def date = new Date()

                def dollarSlashy = \$/
                    Hello \$name,
                    today we're \${date}
                    \$ dollar-sign
                    \$\$ dollar-sign
                    \\ backslash
                    / slash
                    \$/ slash
                /\$

                println dollarSlashy
            """])

        runWithAsserts("Foo")

        reportAndAssert() { File dir -> assertXHTMLFile(dir, "default-pkg/Foo.html") }
    }

    static boolean assertXHTMLFile(File dir, String path, Closure<Boolean> assertion = { true }) {
        new File(dir, path).with { File it ->
            assertTrue(it.exists())
            final parser = new XmlParser(false, false)
            parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            parser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
            assertion.call(parser.parse(it))
        }
    }

    private void reportAndAssert(Closure<Boolean> assertion) {
        def reportDir = new File(groverConfigDir, "report")
        def result = launchJava("""

                -Dclover.logging.level=verbose
                -Djava.io.tmpdir=${System.getProperty("java.io.tmpdir")}
                -Djava.awt.headless=true
                -classpath ${calcReportClasspath([workingDir, calcRepkgJar()].findAll { it != null }.toList())}
                com.atlassian.clover.reporters.html.HtmlReporter -i ${db.absolutePath} -o ${reportDir.with { it.getParentFile(); it }.absolutePath}
            """)
        assertEquals "exit code=${result.getExitCode()}", 0, result.getExitCode()
        def stdErr = filterOutTimesWarning(result.getStdErr())
        assertTrue "stderr = ${stdErr}", stdErr == null || stdErr.length() == 0
        assertTrue assertion.call(reportDir)
    }

    protected String calcReportClasspath(List<File> others = []) {
        return (others + cloverLibs + [cloverCoreClasses, cloverRuntimeClasses, groverClasses, servicesFolder])
                .collect { File it -> it.absolutePath }
                .findAll { it != null }.join(File.pathSeparator)
    }

    String filterOutTimesWarning(String input) {
        def warning = "Warning: the fonts \"Times\" and \"Lucida Bright\" are not available for the Java logical font \"Serif\", which may have unexpected appearance or behavior. Re-enable the \"Times\" font to remove this warning.\n"
        input?.replace(warning, "")
    }
}
