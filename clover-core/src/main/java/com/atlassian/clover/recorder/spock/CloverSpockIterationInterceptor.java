package com.atlassian.clover.recorder.spock;

import org.openclover.runtime.Logger;
import org.openclover.runtime.recorder.TestNameSnifferHelper;
import com_atlassian_clover.TestNameSniffer;
import org.spockframework.runtime.extension.IMethodInterceptor;
import org.spockframework.runtime.extension.IMethodInvocation;
import org.spockframework.runtime.model.IterationInfo;

/**
 * Feature iteration interceptor which look up for a sniffer field declared in currently
 * executing test class and if such field is an instance of {@link TestNameSniffer}
 * it forwards a call to it.
 */
public class CloverSpockIterationInterceptor implements IMethodInterceptor {

    @Override
    public void intercept(IMethodInvocation invocation) throws Throwable {
        // only iterations are intercepted, ignore the rest
        if (invocation.getIteration() != null) {
            Logger.getInstance().debug("CloverSpockIterationInterceptor: Spock is about to call: \""
                    + invocation.getFeature().getName()
                    + "\" (" + invocation.getFeature().getFeatureMethod().getReflection().toString() + ")");

            // find spock sniffer instance in a currently executed test class
            TestNameSniffer spockSniffer = TestNameSnifferHelper.lookupTestSnifferField(invocation.getInstance().getClass());
            if (spockSniffer != null) {
                // and call it in order to record current test name
                IterationInfo iterationInfo = invocation.getIteration();
                if (iterationInfo != null) {
                    Logger.getInstance().debug("CloverSpockIterationInterceptor test name=" + iterationInfo.getName()
                            + " display name=" + iterationInfo.getDisplayName());
                    spockSniffer.setTestName(iterationInfo.getDisplayName());
                }
            }
        }

        // allow Spock to continue
        invocation.proceed();
    }

}
