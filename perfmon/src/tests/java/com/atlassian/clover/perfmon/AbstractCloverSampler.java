package org.openclover.perfmon;

import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.samplers.SampleResult;

public abstract class AbstractCloverSampler extends AbstractJavaSamplerClient {
    protected SampleResult newFailedResult(Throwable t, long start) {
        final SampleResult result = new SampleResult();
        result.setSuccessful(false);
        result.setResponseMessage(t.getMessage());
        System.out.println("Failure: " + t);
        t.printStackTrace(System.out);
        return result;
    }
}
