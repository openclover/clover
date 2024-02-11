package com.atlassian.clover.perfmon;

import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.openclover.runtime.api.CloverException;
import com.atlassian.clover.CloverDatabase;
import com.atlassian.clover.CoverageData;
import com.atlassian.clover.registry.entities.TestCaseInfo;
import com.atlassian.clover.util.collections.Pair;

public class QueryPerTestCoverageHitsSampler extends LoadDbSampler {
    @Override
    protected Pair<SampleResult, CloverDatabase> load(JavaSamplerContext context) throws CloverException {
        final Pair<SampleResult, CloverDatabase> result = super.load(context);

        SampleResult sample = new SampleResult();
        sample.sampleStart();

        final CoverageData coverageData = result.second.getCoverageData();
        for (TestCaseInfo tci : coverageData.getTests()) {
            coverageData.getHitsFor(tci);
        }

        sample.sampleEnd();
        sample.setSuccessful(true);
        
        return Pair.of(sample, result.second);
    }
}
