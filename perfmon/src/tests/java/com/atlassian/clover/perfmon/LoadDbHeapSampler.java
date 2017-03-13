package com.atlassian.clover.perfmon;

import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.io.File;

import com.atlassian.clover.CloverDatabase;
import com.atlassian.clover.api.CloverException;
import com.atlassian.clover.recorder.PerTestCoverageStrategy;
import com.atlassian.clover.cfg.StorageSize;

public class LoadDbHeapSampler extends AbstractLoadDbSampler {
    public SampleResult runTest(JavaSamplerContext context) {
        long start = System.currentTimeMillis();
        try {
            System.gc();
            final MemoryUsage beforeUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
            System.out.println("Before load: " + beforeUsage);
            CloverDatabase db = load(context);

            System.gc();
            final MemoryUsage afterUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
            System.out.println("After load: " + afterUsage);

            //Using duration field as record for heap size
            final SampleResult sample = new SampleResult(start, (afterUsage.getUsed() - beforeUsage.getUsed()));
            sample.setSuccessful(true);
            sample.setResponseMessage(
                "Change in heap size: "
                + (afterUsage.getUsed() - beforeUsage.getUsed())
                + "\nRegistry loaded from: "
                + db.getInitstring());

            return sample;
        } catch (Exception e) {
            return newFailedResult(e, start);
        }
    }

    protected CloverDatabase load(JavaSamplerContext context) throws CloverException {
        return PersistenceUtils.sampleLoad(
            new File(context.getParameter(INITSTRING)),
            Boolean.valueOf(context.getParameter(LOAD_COVERAGE)),
            PerTestCoverageStrategy.valueOf(context.getParameter(PER_TEST_MEMORY_STRATEGY)),
            StorageSize.fromString(context.getParameter(PER_TEST_STORAGE_SIZE))).second;
    }
}
