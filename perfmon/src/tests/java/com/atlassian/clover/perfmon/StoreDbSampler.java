package com.atlassian.clover.perfmon;

import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;

import java.io.File;

public class StoreDbSampler extends AbstractDbPersistenceSampler {
    public SampleResult runTest(JavaSamplerContext context) {
        long start = System.currentTimeMillis();
        try {
            return PersistenceUtils.sampleSave(new File(context.getParameter(INITSTRING)));
        } catch (Exception e) {
            return newFailedResult(e, start);
        }
    }
}