package com_atlassian_clover;

import com.atlassian.clover.Logger;
import org.spockframework.runtime.extension.IMethodInterceptor;
import org.spockframework.runtime.extension.IMethodInvocation;
import org.spockframework.runtime.model.IterationInfo;

/**
 * Test name sniffer which handles Spock runner callback and records a name of a currently executed test.
 */
public class SpockFeatureNameSniffer extends BaseTestNameSniffer implements IMethodInterceptor {
    /**
     * Intercept current test iteration. Memorize the test name.
     * Important: the iMethodInvocation.proceed() must be called after this method call.
     * @param iMethodInvocation
     * @throws Throwable
     */
    @Override
    public void intercept(IMethodInvocation iMethodInvocation) throws Throwable {
        IterationInfo iterationInfo = iMethodInvocation.getIteration();
        if (iterationInfo != null) {
            Logger.getInstance().debug("SpockFeatureNameSniffer test name=" + iterationInfo.getName()
                    + " method name=" + iMethodInvocation.getMethod().getName());
            setTestName(iterationInfo.getName());
        }
    }
}
