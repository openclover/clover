package org.openclover.perfmon;

import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.openclover.runtime.api.CloverException;
import org.openclover.core.CloverDatabase;
import org.openclover.core.CoverageData;
import org.openclover.core.registry.entities.TestCaseInfo;
import org.openclover.core.util.collections.Pair;

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
