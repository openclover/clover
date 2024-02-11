package com.atlassian.clover.perfmon;

import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;

import java.io.File;

import org.openclover.runtime.api.CloverException;
import com.atlassian.clover.recorder.PerTestCoverageStrategy;
import com.atlassian.clover.CloverDatabase;
import com.atlassian.clover.util.collections.Pair;
import com.atlassian.clover.cfg.StorageSize;

public class LoadDbSampler extends AbstractLoadDbSampler {
    public SampleResult runTest(JavaSamplerContext context) {
        long start = System.currentTimeMillis();
        try {
            return load(context).first;
        } catch (Exception e) {
            return newFailedResult(e, start);
        }
    }

    protected Pair<SampleResult, CloverDatabase> load(JavaSamplerContext context) throws CloverException {
        return PersistenceUtils.sampleLoad(
            new File(context.getParameter(INITSTRING)),
            Boolean.valueOf(context.getParameter(LOAD_COVERAGE)),
            PerTestCoverageStrategy.valueOf(context.getParameter(PER_TEST_MEMORY_STRATEGY)),
            StorageSize.fromString(context.getParameter(PER_TEST_STORAGE_SIZE)));
    }
}
