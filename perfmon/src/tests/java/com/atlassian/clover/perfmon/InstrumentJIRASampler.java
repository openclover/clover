package org.openclover.perfmon;

import org.openclover.core.cfg.instr.java.JavaInstrumentationConfig;
import org.openclover.core.instr.java.Instrumenter;
import org.openclover.runtime.api.CloverException;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

public class InstrumentJIRASampler extends AbstractJIRASamplerClient {
    protected static final String SOURCE_LEVEL = "source.level";
    protected static final String FLUSH_POLICY = "flush.policy";
    protected static final String INSTRUMENT_TEST_SOURCE = "instrument.test.source";
    protected static final String INSTRUMENT_APP_SOURCE = "instrument.app.source";
    protected static final String INSTR_LEVEL = "instr.level";
    protected static final String STORE_DB = "store.db";
    protected static final String INCLUDE_STORE_DURATION = "include.store.duration";
    protected static final String CLEANUP = "cleanup";

    @Override
    public Arguments getDefaultParameters() {
        final Arguments arguments = super.getDefaultParameters();
        arguments.addArgument(INSTRUMENT_APP_SOURCE, Boolean.TRUE.toString());
        arguments.addArgument(INSTRUMENT_TEST_SOURCE, Boolean.TRUE.toString());
        arguments.addArgument(SOURCE_LEVEL, "1.6");
        arguments.addArgument(FLUSH_POLICY, "directed");
        arguments.addArgument(INSTR_LEVEL, "statement");
        arguments.addArgument(INCLUDE_STORE_DURATION, Boolean.TRUE.toString());
        arguments.addArgument(STORE_DB, Boolean.TRUE.toString());
        arguments.addArgument(CLEANUP, Boolean.TRUE.toString());
        return arguments;
    }

    public SampleResult runTest(JavaSamplerContext context) {
        long start = System.currentTimeMillis();
        SampleResult result = new SampleResult();
        File workingDir = null;
        try {
            final File jiraHome = resolveJiraHome(context);
            final File srcDir = resolveSourceDir(jiraHome);
            final File testSrcDir = resolveTestSourceDir(jiraHome);
            workingDir = makeWorkingDir();
            final File cloverDb = newCoverageDbFile(workingDir);
            final File instrDir = newInstrWorkingDir(workingDir);
            instrDir.mkdir();

            final Instrumenter instrumenter = newInstrumenter(context, cloverDb);

            result.sampleStart();

            instrument(context, new InstrumentingFilter(instrumenter, instrDir, Integer.MAX_VALUE, false), srcDir, testSrcDir, cloverDb, instrumenter);

            if (!Boolean.valueOf(context.getParameter(INCLUDE_STORE_DURATION))
                || !Boolean.valueOf(context.getParameter(STORE_DB))) {
                result.sampleEnd();
            }

            if (Boolean.valueOf(context.getParameter(STORE_DB))) {
                instrumenter.endInstrumentation();
            }

            if (Boolean.valueOf(context.getParameter(INCLUDE_STORE_DURATION))
                && Boolean.valueOf(context.getParameter(STORE_DB))) {
                result.sampleEnd();
            }
            
            result.setSuccessful(true);
            return result;
        } catch (Throwable e) {
            return newFailedResult(e, start);
        } finally {
            maybeCleanup(context, workingDir);
        }
    }

    protected File newInstrWorkingDir(File workingDir) {
        return new File(workingDir, "instr");
    }

    protected File newCoverageDbFile(File workingDir) {
        return new File(workingDir, "coverage.db");
    }

    protected File makeWorkingDir() throws IOException {
        return InstrumentationUtils.makeTempDir("clover", "perfmon");
    }

    protected File resolveTestSourceDir(File jiraHome) {
        return new File(new File(jiraHome, "src"), "test");
    }

    protected File resolveSourceDir(File jiraHome) {
        return new File(new File(jiraHome, "src"), "java");
    }

    protected Instrumenter newInstrumenter(JavaSamplerContext context, File cloverDb) throws CloverException {
        final JavaInstrumentationConfig instrConfig = new JavaInstrumentationConfig();
        instrConfig.setRegistryFile(cloverDb);
        instrConfig.setSourceLevel(context.getParameter(SOURCE_LEVEL));
        instrConfig.setFlushPolicyFromString(context.getParameter(FLUSH_POLICY));
        instrConfig.setInstrLevelStrategy(context.getParameter(INSTR_LEVEL));
        final Instrumenter instrumenter = new Instrumenter(instrConfig);
        return instrumenter;
    }

    protected void instrument(JavaSamplerContext context, FilenameFilter filter, File srcDir, File testSrcDir, File cloverDb, Instrumenter instrumenter) throws CloverException {
        System.out.println("Starting instrumentation. Db: " + cloverDb.getAbsolutePath());
        instrumenter.startInstrumentation();

        if (Boolean.valueOf(context.getParameter(INSTRUMENT_APP_SOURCE))) {
            System.out.println("Inspecting " + srcDir + " for app source to instrument");
            srcDir.listFiles(filter);
        }

        if (Boolean.valueOf(context.getParameter(INSTRUMENT_TEST_SOURCE))) {
            System.out.println("Inspecting " + testSrcDir + " for test source to instrument");
            testSrcDir.listFiles(filter);
        }
    }

    protected void maybeCleanup(JavaSamplerContext context, File workingDir) {
        if (workingDir != null && Boolean.valueOf(context.getParameter(CLEANUP))) {
            try {
                InstrumentationUtils.deleteDir(workingDir);
            } catch (Exception e) {
                System.out.println("Failed to remove working directory " + workingDir.getAbsolutePath());
            }
        }
    }
}
