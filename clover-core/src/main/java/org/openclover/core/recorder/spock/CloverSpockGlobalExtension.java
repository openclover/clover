package org.openclover.core.recorder.spock;

import org.spockframework.runtime.extension.IGlobalExtension;
import org.spockframework.runtime.model.FeatureInfo;
import org.spockframework.runtime.model.SpecInfo;

/**
 * Spock framework extension which registers an interceptor for feature test iterations
 */
public class CloverSpockGlobalExtension implements IGlobalExtension {
    @Override
    public void visitSpec(SpecInfo specInfo) {
        CloverSpockIterationInterceptor methodInterceptor = new CloverSpockIterationInterceptor();
        for (FeatureInfo featureInfo : specInfo.getAllFeatures()) {
            featureInfo.addIterationInterceptor(methodInterceptor);
        }
    }

    // @Override since Spock 1.0
    public void start() {

    }

    // @Override since Spock 1.0
    public void stop() {

    }
}
