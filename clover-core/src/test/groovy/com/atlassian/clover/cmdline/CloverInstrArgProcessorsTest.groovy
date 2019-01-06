package com.atlassian.clover.cmdline

import com.atlassian.clover.api.command.ArgProcessor
import com.atlassian.clover.cfg.instr.InstrumentationConfig
import com.atlassian.clover.cfg.instr.InstrumentationLevel
import com.atlassian.clover.cfg.instr.java.JavaInstrumentationConfig
import com.atlassian.clover.cfg.instr.java.LambdaInstrumentation
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

class CloverInstrArgProcessorsTest {


    @Test
    void processSrcDir() {
        assertConfig([ "-s", "/source/dir" ],
                CloverInstrArgProcessors.SrcDir,
                { JavaInstrumentationConfig config -> config.getSourceDir() },
                new File("/source/dir"))

        assertConfig([ "--srcdir", "/source/dir" ],
                CloverInstrArgProcessors.SrcDir,
                { JavaInstrumentationConfig config -> config.getSourceDir() },
                new File("/source/dir"))
    }

    @Test
    void processDestDir() {
        assertConfig([ "-d", "/dest/dir" ],
                CloverInstrArgProcessors.DestDir,
                { JavaInstrumentationConfig config -> config.getDestDir() },
                new File("/dest/dir"))

        assertConfig([ "--destdir", "/dest/dir" ],
                CloverInstrArgProcessors.DestDir,
                { JavaInstrumentationConfig config -> config.getDestDir() },
                new File("/dest/dir"))
    }

    @Test
    void processInitstring() {
        assertConfig([ "-i", "openclover.db" ],
                CloverInstrArgProcessors.InitString,
                { JavaInstrumentationConfig config -> config.getInitString() },
                "openclover.db")

        assertConfig([ "--initstring", "openclover.db" ],
                CloverInstrArgProcessors.InitString,
                { JavaInstrumentationConfig config -> config.getInitString() },
                "openclover.db")
    }

    @Test
    void processDistributedCoverage() {
        assertConfig([ "-dc", "ON" ],
                CloverInstrArgProcessors.DistributedCoverage,
                { JavaInstrumentationConfig config -> config.getDistributedConfigString() },
                "name=clover.tcp.server;host=localhost;port=1198;timeout=5000;numClients=0;retryPeriod=1000")

        assertConfig([ "--distributedCoverage", "name=name;host=host;port=80;timeout=123;numClients=2" ],
                CloverInstrArgProcessors.DistributedCoverage,
                { JavaInstrumentationConfig config -> config.getDistributedConfigString() },
                "name=name;host=host;port=80;timeout=123;numClients=2;retryPeriod=1000")
    }

    @Test
    void processRelative() {
        assertConfig([ "" ],
                CloverInstrArgProcessors.Relative,
                { JavaInstrumentationConfig config -> config.isRelative() },
                false)

        assertConfig([ "--relative" ],
                CloverInstrArgProcessors.Relative,
                { JavaInstrumentationConfig config -> config.isRelative() },
                true)
    }

    @Test
    void processFlushPolicy() {
        assertConfig([ "-p", "directed" ],
                CloverInstrArgProcessors.FlushPolicy,
                { JavaInstrumentationConfig config -> config.getFlushPolicy() },
                InstrumentationConfig.DIRECTED_FLUSHING)

        assertConfig([ "-p", "interval" ],
                CloverInstrArgProcessors.FlushPolicy,
                { JavaInstrumentationConfig config -> config.getFlushPolicy() },
                InstrumentationConfig.INTERVAL_FLUSHING)

        assertConfig([ "--flushpolicy", "threaded" ],
                CloverInstrArgProcessors.FlushPolicy,
                { JavaInstrumentationConfig config -> config.getFlushPolicy() },
                InstrumentationConfig.THREADED_FLUSHING)
    }

    @Test
    void processFlushInterval() {
        assertConfig([ "-f", "100" ],
                CloverInstrArgProcessors.FlushInterval,
                { JavaInstrumentationConfig config -> config.getFlushInterval() },
                100)

        assertConfig([ "--flushinterval", "200" ],
                CloverInstrArgProcessors.FlushInterval,
                { JavaInstrumentationConfig config -> config.getFlushInterval() },
                200)
    }


    @Test
    void processEncoding() {
        assertConfig([ "-e", "UTF8" ],
                CloverInstrArgProcessors.Encoding,
                { JavaInstrumentationConfig config -> config.getEncoding() },
                "UTF8")

        assertConfig([ "--encoding", "UTF8" ],
                CloverInstrArgProcessors.Encoding,
                { JavaInstrumentationConfig config -> config.getEncoding() },
                "UTF8")
    }

    @Test
    void processInstrStrategy() {
        assertConfig([ "--instrumentation", "field" ],
                CloverInstrArgProcessors.InstrStrategy,
                { JavaInstrumentationConfig config -> config.isClassInstrStrategy() },
                false)

        assertConfig([ "--instrumentation", "class" ],
                CloverInstrArgProcessors.InstrStrategy,
                { JavaInstrumentationConfig config -> config.isClassInstrStrategy() },
                true)
    }

    @Test
    void processInstrLevel() {
        assertConfig([ "--instrlevel", "statement" ],
                CloverInstrArgProcessors.InstrLevel,
                { JavaInstrumentationConfig config -> config.getInstrLevel() },
                InstrumentationLevel.STATEMENT.ordinal())

        assertConfig([ "--instrlevel", "method" ],
                CloverInstrArgProcessors.InstrLevel,
                { JavaInstrumentationConfig config -> config.getInstrLevel() },
                InstrumentationLevel.METHOD.ordinal())
    }

    @Test
    void processInstrLambda() {
        assertConfig([ "--instrlambda", "none" ],
                CloverInstrArgProcessors.InstrLambda,
                { JavaInstrumentationConfig config -> config.getInstrumentLambda() },
                LambdaInstrumentation.NONE)

        assertConfig([ "--instrlambda", "expression" ],
                CloverInstrArgProcessors.InstrLambda,
                { JavaInstrumentationConfig config -> config.getInstrumentLambda() },
                LambdaInstrumentation.EXPRESSION)

        assertConfig([ "--instrlambda", "all" ],
                CloverInstrArgProcessors.InstrLambda,
                { JavaInstrumentationConfig config -> config.getInstrumentLambda() },
                LambdaInstrumentation.ALL)
    }

    @Test
    void processSourceLevel() {
        assertConfig([ "--source", "1.7" ],
                CloverInstrArgProcessors.SourceLevel,
                { JavaInstrumentationConfig config -> config.getSourceLevel() },
                "1.7")
    }

    @Test
    void processRecordTestResults() {
        assertConfig([ "--recordTestResults", "true" ],
                CloverInstrArgProcessors.RecordTestResults,
                { JavaInstrumentationConfig config -> config.isRecordTestResults() },
                true)
    }

    @Test
    void processDontQualifyJavaLang() {
        assertConfig([ "" ],
                CloverInstrArgProcessors.DontQualifyJavaLang,
                { JavaInstrumentationConfig config -> config.getJavaLangPrefix() },
                "java.lang.")

        assertConfig([ "--dontFullyQualifyJavaLang" ],
                CloverInstrArgProcessors.DontQualifyJavaLang,
                { JavaInstrumentationConfig config -> config.getJavaLangPrefix() },
                "")
    }

    @Test
    void processMethodContext() {
        assertConfig([ "-mc", "getter=get.*" ],
                CloverInstrArgProcessors.MethodContext,
                { JavaInstrumentationConfig config -> config.getMethodContexts().get(0).getRegexp() },
                "get.*")

        assertConfig([ "--methodContext", "setter=set.*" ],
                CloverInstrArgProcessors.MethodContext,
                { JavaInstrumentationConfig config -> config.getMethodContexts().get(0).getRegexp() },
                "set.*")
    }

    @Test
    void processStatementContext() {
        assertConfig([ "-sc", "logger=log\\.debug" ],
                CloverInstrArgProcessors.StatementContext,
                { JavaInstrumentationConfig config -> config.getStatementContexts().get(0).getRegexp() },
                "log\\.debug")

        assertConfig([ "--statementContext", "print=out\\.println" ],
                CloverInstrArgProcessors.StatementContext,
                { JavaInstrumentationConfig config -> config.getStatementContexts().get(0).getRegexp() },
                "out\\.println")
    }

    @Test
    void processVerbose() {
        assertTrue(CloverInstrArgProcessors.Verbose.matches(["-v"] as String[], 0))
        assertTrue(CloverInstrArgProcessors.Verbose.matches(["--verbose"] as String[], 0))
    }

    @Test
    void processJavaSourceFile() {
        assertConfig([ "Foo.java" ],
                CloverInstrArgProcessors.JavaSourceFile,
                { JavaInstrumentationConfig config -> config.getSourceFiles().get(0) },
                "Foo.java")

        assertConfig([ "Foo.groovy" ],
                CloverInstrArgProcessors.JavaSourceFile,
                { JavaInstrumentationConfig config -> config.getSourceFiles().size() },
                0)
    }

    private static assertConfig(List<String> args,
                                ArgProcessor<JavaInstrumentationConfig> argProcessor,
                                Closure<Object> configValueExtractor,
                                Object expectedValue) {
        JavaInstrumentationConfig config = new JavaInstrumentationConfig()
        String[] argsArray = args.toArray(new String[args.size()]);
        int i = 0;
        if (argProcessor.matches(argsArray, i)) {
            argProcessor.process(argsArray, i, config)
        }
        assertEquals(expectedValue, configValueExtractor.call(config));
    }

}