package com.atlassian.clover.perfmon;

import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;

import java.io.File;

import com.atlassian.clover.optimization.Snapshot;
import com.atlassian.clover.CoverageDataSpec;

public class CreateSnapshotSampler extends AbstractJIRASamplerClient {
    public SampleResult runTest(JavaSamplerContext context) {
        long start = System.currentTimeMillis();
        try {
            final File jiraHome = resolveJiraHome(context);
            final File snaphotFile = File.createTempFile("clover", "snapshot");

            SampleResult result = new SampleResult();
            result.sampleStart();
            Snapshot.generateFor(
                new File(jiraHome, "target" + File.separator + "clover" + File.separator + "database" + File.separator + "clover_coverage.db").getAbsolutePath(),
                snaphotFile.getAbsolutePath(),
                new CoverageDataSpec()).store();
            result.sampleEnd();
            result.setSuccessful(true);
            return result;
        } catch (Exception e) {
            return newFailedResult(e, start);
        }
    }
}
