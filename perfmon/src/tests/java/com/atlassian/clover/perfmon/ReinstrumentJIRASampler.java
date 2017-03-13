package com.atlassian.clover.perfmon;

import com.atlassian.clover.instr.java.Instrumenter;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import java.io.File;

public class ReinstrumentJIRASampler extends InstrumentJIRASampler {
    protected static final String REINSTRUMENT_COUNT = "reinstrument.count";

    @Override
    public Arguments getDefaultParameters() {
        final Arguments defaults = super.getDefaultParameters();
        defaults.removeArgument(STORE_DB);
        defaults.removeArgument(INCLUDE_STORE_DURATION);
        defaults.addArgument(REINSTRUMENT_COUNT, "1");
        return defaults;
    }

    @Override
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

            //Instrument everything first
            Instrumenter instrumenter = newInstrumenter(context, cloverDb);
            instrument(context, new InstrumentingFilter(instrumenter, instrDir, Integer.MAX_VALUE, false), srcDir, testSrcDir, cloverDb, instrumenter);
            instrumenter.endInstrumentation();
            instrumenter = null;

            //To minimise GC of the old db in memory. Perhaps a bit of "woo".
            System.gc();

            //Now instrument N source files again to simulate incremental compilation
            result.sampleStart();
            instrumenter = newInstrumenter(context, cloverDb);
            instrument(context, new ReinstrumentingFilter(instrumenter, instrDir, calcNumToReinstrument(context), false), srcDir, testSrcDir, cloverDb, instrumenter);
            instrumenter.endInstrumentation();
            result.sampleEnd();

            result.setSuccessful(true);
            return result;
        } catch (Throwable e) {
            return newFailedResult(e, start);
        } finally {
            maybeCleanup(context, workingDir);
        }
    }

    private int calcNumToReinstrument(JavaSamplerContext context) {
        final String numToReinstrument = context.getParameter(REINSTRUMENT_COUNT);
        try {
            return
            "ALL".equalsIgnoreCase(numToReinstrument)
                ? Integer.MAX_VALUE
                    : "NONE".equalsIgnoreCase(numToReinstrument)
                        ? -1
                        : Integer.parseInt(numToReinstrument);
        } catch (NumberFormatException e) {
            System.out.println("Parameter " + REINSTRUMENT_COUNT + " had invalid value: " + numToReinstrument);
            return 1;
        }
    }
}
