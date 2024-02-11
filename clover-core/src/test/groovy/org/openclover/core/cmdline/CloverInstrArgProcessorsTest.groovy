package org.openclover.core.cmdline

import com.atlassian.clover.api.command.ArgProcessor
import com.atlassian.clover.cfg.instr.InstrumentationConfig
import com.atlassian.clover.cfg.instr.InstrumentationLevel
import com.atlassian.clover.cfg.instr.MethodContextDef
import com.atlassian.clover.cfg.instr.java.JavaInstrumentationConfig
import com.atlassian.clover.cfg.instr.java.LambdaInstrumentation
import com.atlassian.clover.cfg.instr.java.SourceLevel
import com.atlassian.clover.cmdline.CloverInstrArgProcessors
import com.atlassian.clover.instr.java.JavaTypeContext
import com.atlassian.clover.instr.tests.TestDetector
import org.hamcrest.Matcher
import org.junit.Test

import static org.hamcrest.CoreMatchers.*
import static org.hamcrest.MatcherAssert.assertThat
import static org.junit.Assert.assertTrue

class CloverInstrArgProcessorsTest {


    @Test
    void processSrcDir() {
        assertConfig(["-s", "/source/dir"],
                CloverInstrArgProcessors.SrcDir,
                { JavaInstrumentationConfig config -> config.getSourceDir() },
                equalTo(new File("/source/dir").absoluteFile))

        assertConfig(["--srcdir", "/source/dir"],
                CloverInstrArgProcessors.SrcDir,
                { JavaInstrumentationConfig config -> config.getSourceDir() },
                equalTo(new File("/source/dir").absoluteFile))
    }

    @Test
    void processDestDir() {
        assertConfig(["-d", "/dest/dir"],
                CloverInstrArgProcessors.DestDir,
                { JavaInstrumentationConfig config -> config.getDestDir() },
                equalTo(new File("/dest/dir").absoluteFile))

        assertConfig(["--destdir", "/dest/dir"],
                CloverInstrArgProcessors.DestDir,
                { JavaInstrumentationConfig config -> config.getDestDir() },
                equalTo(new File("/dest/dir").absoluteFile))
    }

    @Test
    void processInitstring() {
        assertConfig(["-i", "openclover.db"],
                CloverInstrArgProcessors.InitString,
                { JavaInstrumentationConfig config -> config.getInitString() },
                equalTo("openclover.db"))

        assertConfig(["--initstring", "openclover.db"],
                CloverInstrArgProcessors.InitString,
                { JavaInstrumentationConfig config -> config.getInitString() },
                equalTo("openclover.db"))
    }

    @Test
    void processDistributedCoverage() {
        assertConfig(["-dc", "ON"],
                CloverInstrArgProcessors.DistributedCoverage,
                {
                    JavaInstrumentationConfig config -> config.getDistributedConfigString()
                },
                allOf(
                        containsString("name=clover.tcp.server"),
                        containsString("host=localhost"),
                        containsString("port=1198"),
                        containsString("timeout=5000"),
                        containsString("numClients=0"),
                        containsString("retryPeriod=1000")
                )
        )

        assertConfig(["--distributedCoverage", "name=name;host=host;port=80;timeout=123;numClients=2"],
                CloverInstrArgProcessors.DistributedCoverage,
                {
                    JavaInstrumentationConfig config -> config.getDistributedConfigString()
                },
                allOf(
                        containsString("name=name"),
                        containsString("host=host"),
                        containsString("port=80"),
                        containsString("timeout=123"),
                        containsString("numClients=2"),
                        containsString("retryPeriod=1000")
                )
        )
    }

    @Test
    void processRelative() {
        assertConfig([""],
                CloverInstrArgProcessors.Relative,
                { JavaInstrumentationConfig config -> config.isRelative() },
                equalTo(false))

        assertConfig(["--relative"],
                CloverInstrArgProcessors.Relative,
                { JavaInstrumentationConfig config -> config.isRelative() },
                equalTo(true))
    }

    @Test
    void processFlushPolicy() {
        assertConfig(["-p", "directed"],
                CloverInstrArgProcessors.FlushPolicy,
                { JavaInstrumentationConfig config -> config.getFlushPolicy() },
                equalTo(InstrumentationConfig.DIRECTED_FLUSHING))

        assertConfig(["-p", "interval"],
                CloverInstrArgProcessors.FlushPolicy,
                { JavaInstrumentationConfig config -> config.getFlushPolicy() },
                equalTo(InstrumentationConfig.INTERVAL_FLUSHING))

        assertConfig(["--flushpolicy", "threaded"],
                CloverInstrArgProcessors.FlushPolicy,
                { JavaInstrumentationConfig config -> config.getFlushPolicy() },
                equalTo(InstrumentationConfig.THREADED_FLUSHING))
    }

    @Test
    void processFlushInterval() {
        assertConfig(["-f", "100"],
                CloverInstrArgProcessors.FlushInterval,
                { JavaInstrumentationConfig config -> config.getFlushInterval() },
                equalTo(100))

        assertConfig(["--flushinterval", "200"],
                CloverInstrArgProcessors.FlushInterval,
                { JavaInstrumentationConfig config -> config.getFlushInterval() },
                equalTo(200))
    }


    @Test
    void processEncoding() {
        assertConfig(["-e", "UTF8"],
                CloverInstrArgProcessors.Encoding,
                { JavaInstrumentationConfig config -> config.getEncoding() },
                equalTo("UTF8"))

        assertConfig(["--encoding", "UTF8"],
                CloverInstrArgProcessors.Encoding,
                { JavaInstrumentationConfig config -> config.getEncoding() },
                equalTo("UTF8"))
    }

    @Test
    void processInstrStrategy() {
        assertConfig(["--instrumentation", "field"],
                CloverInstrArgProcessors.InstrStrategy,
                { JavaInstrumentationConfig config -> config.isClassInstrStrategy() },
                equalTo(false))

        assertConfig(["--instrumentation", "class"],
                CloverInstrArgProcessors.InstrStrategy,
                { JavaInstrumentationConfig config -> config.isClassInstrStrategy() },
                equalTo(true))
    }

    @Test
    void processInstrLevel() {
        assertConfig(["--instrlevel", "statement"],
                CloverInstrArgProcessors.InstrLevel,
                { JavaInstrumentationConfig config -> config.getInstrLevel() },
                equalTo(InstrumentationLevel.STATEMENT.ordinal()))

        assertConfig(["--instrlevel", "method"],
                CloverInstrArgProcessors.InstrLevel,
                { JavaInstrumentationConfig config -> config.getInstrLevel() },
                equalTo(InstrumentationLevel.METHOD.ordinal()))
    }

    @Test
    void processInstrLambda() {
        assertConfig(["--instrlambda", "none"],
                CloverInstrArgProcessors.InstrLambda,
                { JavaInstrumentationConfig config -> config.getInstrumentLambda() },
                equalTo(LambdaInstrumentation.NONE))

        assertConfig(["--instrlambda", "expression"],
                CloverInstrArgProcessors.InstrLambda,
                { JavaInstrumentationConfig config -> config.getInstrumentLambda() },
                equalTo(LambdaInstrumentation.EXPRESSION))

        assertConfig(["--instrlambda", "all"],
                CloverInstrArgProcessors.InstrLambda,
                { JavaInstrumentationConfig config -> config.getInstrumentLambda() },
                equalTo(LambdaInstrumentation.ALL))
    }

    @Test
    void processSourceLevel() {
        assertConfig(["--source", "1.8"],
                CloverInstrArgProcessors.SourceLevelArg,
                { JavaInstrumentationConfig config -> config.getSourceLevel() },
                equalTo(SourceLevel.JAVA_8))
    }

    @Test
    void processRecordTestResults() {
        assertConfig(["--recordTestResults", "true"],
                CloverInstrArgProcessors.RecordTestResults,
                { JavaInstrumentationConfig config -> config.isRecordTestResults() },
                equalTo(true))
    }

    @Test
    void processDontQualifyJavaLang() {
        assertConfig([""],
                CloverInstrArgProcessors.DontQualifyJavaLang,
                { JavaInstrumentationConfig config -> config.getJavaLangPrefix() },
                equalTo("java.lang."))

        assertConfig(["--dontFullyQualifyJavaLang"],
                CloverInstrArgProcessors.DontQualifyJavaLang,
                { JavaInstrumentationConfig config -> config.getJavaLangPrefix() },
                equalTo(""))
    }

    @Test
    void processMethodContext() {
        assertConfig(["-mc", "getter=get.*"],
                CloverInstrArgProcessors.MethodContext,
                { JavaInstrumentationConfig config -> config.getMethodContexts().get(0).getRegexp() },
                equalTo("get.*"))

        assertConfig(["--methodContext", "setter=set.*"],
                CloverInstrArgProcessors.MethodContext,
                { JavaInstrumentationConfig config -> config.getMethodContexts().get(0).getRegexp() },
                equalTo("set.*"))
    }

    @Test
    void processMethodWithMetricsContext() {
        assertConfig(["-mmc", "trivialGetter;public.*get.*\\(\\);;;1;1"],
                CloverInstrArgProcessors.MethodWithMetricsContext,
                { JavaInstrumentationConfig config -> config.getMethodContexts().get(0) },
                equalTo(
                        new MethodContextDef(
                                "trivialGetter", "public.*get.*\\(\\)",
                                Integer.MAX_VALUE, Integer.MAX_VALUE,
                                1, 1)
                )
        )

        assertConfig(["--methodWithMetricsContext", "trivialSetter;public.*set.*;2;1"],
                CloverInstrArgProcessors.MethodWithMetricsContext,
                { JavaInstrumentationConfig config -> config.getMethodContexts().get(0) },
                equalTo(
                        new MethodContextDef(
                                "trivialSetter", "public.*set.*",
                                2, 1,
                                Integer.MAX_VALUE, Integer.MAX_VALUE)
                )
        )
    }

    @Test
    void processStatementContext() {
        assertConfig(["-sc", "logger=log\\.debug"],
                CloverInstrArgProcessors.StatementContext,
                { JavaInstrumentationConfig config -> config.getStatementContexts().get(0).getRegexp() },
                equalTo("log\\.debug"))

        assertConfig(["--statementContext", "print=out\\.println"],
                CloverInstrArgProcessors.StatementContext,
                { JavaInstrumentationConfig config -> config.getStatementContexts().get(0).getRegexp() },
                equalTo("out\\.println"))
    }

    @Test
    void processTestSourceRoot() {
        [ "-tsr", "--testSourceRoot" ].each {
            assertConfig([it, "src/test/java"],
                    CloverInstrArgProcessors.TestSourceRoot,
                    { JavaInstrumentationConfig config -> config.getTestDetector() },
                    allOf(
                            TestDetectorMatchers.fileMatches("src/test/java/Test.java"),
                            not(TestDetectorMatchers.fileMatches("src/main/java/Test.java"))
                    ))
        }
    }

    @Test
    void processTestSourceIncludes() {
        [ "-tsi", "--testSourceIncludes" ].each {
            assertConfig([it, "**/*Test.java,*/*TestSuite.java,**/*Any*"],
                    CloverInstrArgProcessors.TestSourceIncludes,
                    { JavaInstrumentationConfig config -> config.getTestDetector() },
                    allOf(
                            TestDetectorMatchers.fileMatches("this/is/a/Test.java"),
                            TestDetectorMatchers.fileMatches("this/is/a/SecondTest.java"),
                            not(TestDetectorMatchers.fileMatches("this/is/not/a/ThirdTestCase.java")),
                            TestDetectorMatchers.fileMatches("main/TestSuite.java"),
                            not(TestDetectorMatchers.fileMatches("main/not/a/test/TestSuite.java")),
                            TestDetectorMatchers.fileMatches("also/WithAnyGroovy.groovy"),
                            TestDetectorMatchers.fileMatches("Any"),
                            not(TestDetectorMatchers.fileMatches("Else"))
                    ))
        }
    }

    @Test
    void processTestSourceExcludes() {
        [ "-tse", "--testSourceExcludes" ].each {
            assertConfig([it, "**/*Test.java,*/*TestSuite.java,**/*Any*"],
                    CloverInstrArgProcessors.TestSourceExcludes,
                    { JavaInstrumentationConfig config -> config.getTestDetector() },
                    allOf(
                            not(TestDetectorMatchers.fileMatches("this/is/a/Test.java")),
                            not(TestDetectorMatchers.fileMatches("this/is/a/SecondTest.java")),
                            TestDetectorMatchers.fileMatches("this/is/not/a/ThirdTestCase.java"),
                            not(TestDetectorMatchers.fileMatches("main/TestSuite.java")),
                            TestDetectorMatchers.fileMatches("main/not/a/test/TestSuite.java"),
                            not(TestDetectorMatchers.fileMatches("also/WithAnyGroovy.groovy")),
                            not(TestDetectorMatchers.fileMatches("Any")),
                            TestDetectorMatchers.fileMatches("Else")
                    ))
        }
    }

    @Test
    void processTestSourceClass() {
        [ "-tsc", "--testSourceClass" ].each {
            // class name
            assertConfig([it, "Simple.*"],
                    CloverInstrArgProcessors.TestSourceClass,
                    { JavaInstrumentationConfig config -> config.getTestDetector() },
                    allOf(
                            TestDetectorMatchers.classMatches("Simple", null, null, null, null),
                            TestDetectorMatchers.classMatches("SimpleClass", null, null, null, null),
                            not(TestDetectorMatchers.classMatches("ComplexClass", null, null, null, null))
                    ))

            // package name
            assertConfig([it, ";com\\.acme\\..*"],
                    CloverInstrArgProcessors.TestSourceClass,
                    { JavaInstrumentationConfig config -> config.getTestDetector() },
                    allOf(
                            TestDetectorMatchers.classMatches(null, "com.acme.abc", null, null, null),
                            not(TestDetectorMatchers.classMatches(null, "com.acme", null, null, null)),
                            not(TestDetectorMatchers.classMatches(null, "prefix.com.acme.abc", null, null, null))
                    ))

            // annotation
            assertConfig([it, ";;Test;;"],
                    CloverInstrArgProcessors.TestSourceClass,
                    { JavaInstrumentationConfig config -> config.getTestDetector() },
                    allOf(
                            TestDetectorMatchers.classMatches(null, null, "Test", null, null),
                            not(TestDetectorMatchers.classMatches(null, null, "Testing", null, null))
                    ))

            // superclass
            assertConfig([it, ";;;TestCase;"],
                    CloverInstrArgProcessors.TestSourceClass,
                    { JavaInstrumentationConfig config -> config.getTestDetector() },
                    allOf(
                            TestDetectorMatchers.classMatches(null, null, null, "TestCase", null),
                            not(TestDetectorMatchers.classMatches(null, null, null, "TestSuite", null))
                    ))

            // javadoc
            assertConfig([it, ";;;;contract"],
                    CloverInstrArgProcessors.TestSourceClass,
                    { JavaInstrumentationConfig config -> config.getTestDetector() },
                    allOf(
                            TestDetectorMatchers.classMatches(null, null, null, null, "contract"),
                            not(TestDetectorMatchers.classMatches(null, null, null, null, "assuming"))
                    ))

            // combination of all above
            assertConfig([it, "Simple.*;com\\.acme\\..*;Test;TestCase;contract"],
                    CloverInstrArgProcessors.TestSourceClass,
                    { JavaInstrumentationConfig config -> config.getTestDetector() },
                    allOf(
                            TestDetectorMatchers.classMatches("Simple", "com.acme.abc", "Test", "TestCase", "contract"),
                            not(TestDetectorMatchers.classMatches("Complex", "com.acme.abc", "Test", "TestCase", "contract")),
                            not(TestDetectorMatchers.classMatches("Simple", "xyz.com.acme.abc", "Test", "TestCase", "contract")),
                            not(TestDetectorMatchers.classMatches("Simple", "com.acme.abc", "TestRun", "TestCase", "contract")),
                            not(TestDetectorMatchers.classMatches("Simple", "com.acme.abc", "Test", "TestSuite", "contract")),
                            not(TestDetectorMatchers.classMatches("Simple", "com.acme.abc", "Test", "TestCase", "assuming")),
                    ))
        }
    }

    @Test
    void processTestSourceMethod() {
        [ "-tsm", "--testSourceMethod" ].each {
            // method name
            assertConfig([it, "test.*"],
                    CloverInstrArgProcessors.TestSourceMethod,
                    { JavaInstrumentationConfig config -> config.getTestDetector() },
                    allOf(
                            TestDetectorMatchers.methodMatches("testAbc", null, "void", null),
                            TestDetectorMatchers.methodMatches("testDef", null, "void", null),
                            not(TestDetectorMatchers.methodMatches("notTestGhi", null, "void", null))
                    ))

            // annotation
            assertConfig([it, ";Test"],
                    CloverInstrArgProcessors.TestSourceMethod,
                    { JavaInstrumentationConfig config -> config.getTestDetector() },
                    allOf(
                            TestDetectorMatchers.methodMatches(null, "Test", "void", null),
                            TestDetectorMatchers.methodMatches("testAbc", "Test", "void", null),
                            not(TestDetectorMatchers.methodMatches(null, "NotTest", "void", null))
                    ))

            // return type
            assertConfig([it, ";;void"],
                    CloverInstrArgProcessors.TestSourceMethod,
                    { JavaInstrumentationConfig config -> config.getTestDetector() },
                    allOf(
                            TestDetectorMatchers.methodMatches(null, null, "void", null),
                            TestDetectorMatchers.methodMatches("testAbc", "Test", "void", null),
                            not(TestDetectorMatchers.methodMatches(null, null, "int", null))
                    ))

            // null return type which means a constructor - always false as we cannot instrument a constructor as a test method
            assertConfig([it, "Abc;;"],
                    CloverInstrArgProcessors.TestSourceMethod,
                    { JavaInstrumentationConfig config -> config.getTestDetector() },
                    allOf(
                            not(TestDetectorMatchers.methodMatches("Abc", null, null, null))
                    ))

            // javadoc tag
            assertConfig([it, ";;;junit"],
                    CloverInstrArgProcessors.TestSourceMethod,
                    { JavaInstrumentationConfig config -> config.getTestDetector() },
                    allOf(
                            TestDetectorMatchers.methodMatches(null, null, "void", "junit"),
                            TestDetectorMatchers.methodMatches("testAbc", "Test", "void", "junit"),
                            not(TestDetectorMatchers.methodMatches(null, null, "void", "testng"))
                    ))

            // all of the above
            assertConfig([it, "test.*;Test;void;junit"],
                    CloverInstrArgProcessors.TestSourceMethod,
                    { JavaInstrumentationConfig config -> config.getTestDetector() },
                    allOf(
                            TestDetectorMatchers.methodMatches("testAbc", "Test", "void", "junit"),
                            not(TestDetectorMatchers.methodMatches("notTest", "Test", "void", "junit")),
                            not(TestDetectorMatchers.methodMatches("testAbc", "NotTest", "void", "junit")),
                            not(TestDetectorMatchers.methodMatches("testAbc", "Test", "int", "junit")),
                            not(TestDetectorMatchers.methodMatches("testAbc", "Test", "void", "testng"))
                    ))
        }
    }

    /**
     * Test combination of all four matchers at once, especially that method matchers are dependent on parent class matchers.
     */
    @Test
    void processTestSourceIncludesExcludesClassesMethods() {
        // some helper contexts to pass assertions if we're not interested in this aspect
        TestDetector.TypeContext typeContext = new JavaTypeContext([test: [""] ], null, "default-pkg", "IT", null)
        TestDetector.SourceContext sourceContext = new SimpleFileSourceContext("com/acme/include/AnyOne.java")

        assertConfig(
                [
                        "-tsi",
                        "**/include/*One.java,**/include/two/**.java",
                        "-tse",
                        "**/include/exclude/**.java,**/deprecated/**.java",
                        "-tsc",
                        ";com.acme.*;TestSuite;SuperClass;",
                        "-tsm",
                        "test.*;;void;",
                        "-tsm",
                        ";Test;;",
                        "-tsc",
                        ".*IT;;;;test",
                        "-tsm",
                        ";;;test"
                ],
                [
                        CloverInstrArgProcessors.TestSourceIncludes,
                        CloverInstrArgProcessors.TestSourceExcludes,
                        CloverInstrArgProcessors.TestSourceClass,
                        CloverInstrArgProcessors.TestSourceMethod
                ],
                {
                    JavaInstrumentationConfig config -> config.getTestDetector()
                },
                allOf(
                        // "-tsi" and "-tse"
                        TestDetectorMatchers.fileMatches("com/acme/include/One.java", typeContext),
                        TestDetectorMatchers.fileMatches("com/acme/include/two/Two.java", typeContext),
                        not(TestDetectorMatchers.fileMatches("com/acme/include/exclude/Excluded.java", typeContext)),
                        not(TestDetectorMatchers.fileMatches("deprecated/Deprecated.java", typeContext)),

                        // "-tsc" ";com.acme.*;TestSuite;SuperClass;"
                        TestDetectorMatchers.classMatches("Any", "com.acme.include", "TestSuite", "SuperClass", null, sourceContext),
                        not(TestDetectorMatchers.classMatches("Any", "com.acme.not", "TestSuite", null, null, sourceContext)),

                        // "-tsc" ".*IT;;;;test"
                        TestDetectorMatchers.classMatches("TwoIT", "com.acme.include.two", null, null, "test", sourceContext),
                        not(TestDetectorMatchers.classMatches("TwoTest", "com.acme.notincluded.two", null, null, "util", sourceContext)),

                        // "-tsm" "test.*;;void;"
                        TestDetectorMatchers.methodMatches("testSomething", null, "void", null, sourceContext),
                        // "-tsm" ";Test;;"
                        TestDetectorMatchers.methodMatches("anyMethod", "Test", "int", "", sourceContext),
                        // "-tsm" ";;;test"
                        TestDetectorMatchers.methodMatches("anyMethod", null, "int", "test", sourceContext),
                ))
    }

    @Test
    void processVerbose() {
        assertTrue(CloverInstrArgProcessors.Verbose.matches(["-v"] as String[], 0))
        assertTrue(CloverInstrArgProcessors.Verbose.matches(["--verbose"] as String[], 0))
    }

    @Test
    void processJavaSourceFile() {
        assertConfig(["Foo.java"],
                CloverInstrArgProcessors.JavaSourceFile,
                { JavaInstrumentationConfig config -> config.getSourceFiles().get(0) },
                equalTo("Foo.java"))

        assertConfig(["Foo.groovy"],
                CloverInstrArgProcessors.JavaSourceFile,
                { JavaInstrumentationConfig config -> config.getSourceFiles().size() },
                equalTo(0))
    }

    private static <T> void assertConfig(List<String> args,
                                         ArgProcessor<JavaInstrumentationConfig> argProcessor,
                                         Closure<Object> configValueExtractor,
                                         Matcher<T> expectedValueMatcher) {
        JavaInstrumentationConfig config = new JavaInstrumentationConfig()
        config.setSourceDir(File.createTempDir())

        String[] argsArray = args.toArray(new String[args.size()])
        int i = 0
        if (argProcessor.matches(argsArray, i)) {
            argProcessor.process(argsArray, i, config)
        }
        assertThat(configValueExtractor.call(config), expectedValueMatcher)
    }

    private static <T> void assertConfig(List<String> args,
                                         List<ArgProcessor<JavaInstrumentationConfig>> argProcessors,
                                         Closure<Object> configValueExtractor,
                                         Matcher<T> expectedValueMatcher) {
        JavaInstrumentationConfig config = new JavaInstrumentationConfig()
        config.setSourceDir(File.createTempDir())

        String[] argsArray = args.toArray(new String[args.size()])

        int i = 0
        while (i < args.size()) {
            for (ArgProcessor<JavaInstrumentationConfig> argProcessor : argProcessors) {
                if (argProcessor.matches(argsArray, i)) {
                    i = argProcessor.process(argsArray, i, config)
                }
            }
            i++
        }

        assertThat(configValueExtractor.call(config), expectedValueMatcher)
    }
}