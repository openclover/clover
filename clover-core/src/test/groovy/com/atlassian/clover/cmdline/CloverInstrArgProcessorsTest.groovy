package com.atlassian.clover.cmdline

import com.atlassian.clover.api.command.ArgProcessor
import com.atlassian.clover.cfg.instr.InstrumentationConfig
import com.atlassian.clover.cfg.instr.InstrumentationLevel
import com.atlassian.clover.cfg.instr.MethodContextDef
import com.atlassian.clover.cfg.instr.java.JavaInstrumentationConfig
import com.atlassian.clover.cfg.instr.java.LambdaInstrumentation
import org.hamcrest.Matcher
import org.junit.Test

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.assertThat
import static org.junit.Assert.assertTrue

class CloverInstrArgProcessorsTest {


    @Test
    void processSrcDir() {
        assertConfig(["-s", "/source/dir"],
                CloverInstrArgProcessors.SrcDir,
                { JavaInstrumentationConfig config -> config.getSourceDir() },
                equalTo(new File("/source/dir")))

        assertConfig(["--srcdir", "/source/dir"],
                CloverInstrArgProcessors.SrcDir,
                { JavaInstrumentationConfig config -> config.getSourceDir() },
                equalTo(new File("/source/dir")))
    }

    @Test
    void processDestDir() {
        assertConfig(["-d", "/dest/dir"],
                CloverInstrArgProcessors.DestDir,
                { JavaInstrumentationConfig config -> config.getDestDir() },
                equalTo(new File("/dest/dir")))

        assertConfig(["--destdir", "/dest/dir"],
                CloverInstrArgProcessors.DestDir,
                { JavaInstrumentationConfig config -> config.getDestDir() },
                equalTo(new File("/dest/dir")))
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
        assertConfig(["--source", "1.7"],
                CloverInstrArgProcessors.SourceLevel,
                { JavaInstrumentationConfig config -> config.getSourceLevel() },
                equalTo("1.7"))
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
    void processMethodContextExt() {
        assertConfig(["-mce", "trivialGetter;public.*get.*\\(\\);;;1;1"],
                CloverInstrArgProcessors.MethodContextExt,
                { JavaInstrumentationConfig config -> config.getMethodContexts().get(0) },
                equalTo(
                        new MethodContextDef(
                                "trivialGetter", "public.*get.*\\(\\)",
                                Integer.MAX_VALUE, Integer.MAX_VALUE,
                                1, 1)
                )
        )

        assertConfig(["--methodContextExt", "trivialSetter;public.*set.*;2;1"],
                CloverInstrArgProcessors.MethodContextExt,
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
        String[] argsArray = args.toArray(new String[args.size()]);
        int i = 0;
        if (argProcessor.matches(argsArray, i)) {
            argProcessor.process(argsArray, i, config)
        }
        assertThat(configValueExtractor.call(config), expectedValueMatcher);
    }

}