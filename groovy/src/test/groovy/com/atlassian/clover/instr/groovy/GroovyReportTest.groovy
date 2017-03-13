package com.atlassian.clover.instr.groovy

import com.atlassian.clover.test.junit.GroovyVersionStart

class GroovyReportTest extends TestBase {
    public GroovyReportTest(methodName, specificName, groovyAllJar) {
        super(methodName, specificName, groovyAllJar);
    }

    public GroovyReportTest(String testName) {
        super(testName);
    }

    public void testReportsOnFourClassTypes() {
        instrumentAndCompileWithGrover(
            ["FooEnum.groovy":
            """
                public enum FooEnum {
                }
              """,
            "FooClass.groovy":
            """
                public class FooClass {
                  public static void main(String[] args) {}
                }
            """,
            "FooInterface.groovy":
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

        reportAndAssert() { dir ->
            assertXHTMLFile(dir, "default-pkg/FooEnum.html") &&
            assertXHTMLFile(dir, "default-pkg/FooClass.html") &&
            assertXHTMLFile(dir, "default-pkg/FooInterface.html") &&
            assertXHTMLFile(dir, "default-pkg/FooAnnotation.html") &&
            true
        }
    }

    public void testStillReportOnEnumMethods() {
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

        reportAndAssert() { dir ->
            assertXHTMLFile(dir, "default-pkg/Foo.html") { Node page ->
                def line5 = page.depthFirst().tr.find { println it.'@id'; it.'@id' == 'l5'}
                line5 != null && line5.td.find { println it.'@class'; it.'@class' == 'coverageCount Good missedByTest' }.span[0].'@title'.contains("method entered 1 time.")
            }
        }
    }

    @GroovyVersionStart("1.8.0")
    public void testReportsOnDollarSlashyStrings() {
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

        reportAndAssert() { dir ->
            assertXHTMLFile(dir, "default-pkg/Foo.html") &&
            true
        }
    }

    boolean assertXHTMLFile(File dir, String path, Closure assertion = {true}) {
        new File(dir, path).with {
            assertTrue(it.exists())
            final parser = new XmlParser(false, false)
            parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            parser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
            assertion.call(parser.parse(it))
        }
    }

    private void reportAndAssert(Closure assertion) {
        def reportDir = new File(groverConfigDir, "report")
        def result = launchJava("""

                -Dclover.logging.level=verbose
                -Djava.io.tmpdir=${System.getProperty("java.io.tmpdir")}
                -Dclover.license.path=${System.getProperty("clover.license.path")}
                -Djava.awt.headless=true
                -classpath ${calcReportClasspath([workingDir.getAbsolutePath(), calcRepkgJarPath()].findAll { it != null }.toList())}
                com.atlassian.clover.reporters.html.HtmlReporter -i ${db.absolutePath} -o ${reportDir.with { it.getParentFile(); it }.absolutePath}
            """)
        assertEquals "exit code=${result.getExitCode()}", 0, result.getExitCode()
        assertTrue "stderr = ${result.getStdErr()}", result.getStdErr() == null || result.getStdErr().length() == 0
        assertTrue assertion.call(reportDir)
    }

    protected String calcReportClasspath(List others = []) {
        return (others +
            cloverLibs.collect { it.absolutePath } +
            [
                cloverCoreClasses.absolutePath,
                cloverRuntimeClasses.absolutePath,
                groverClasses.absolutePath,
                servicesFolder.absolutePath,
            ]).findAll { it != null }.join(File.pathSeparator)
    }
}
