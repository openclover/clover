package com.atlassian.clover.recorder.spock;

import com.atlassian.clover.CloverNames;
import com.atlassian.clover.Logger;
import com_atlassian_clover.SpockFeatureNameSniffer;
import com_atlassian_clover.TestNameSniffer;
import org.jetbrains.annotations.Nullable;
import org.spockframework.runtime.extension.IMethodInterceptor;
import org.spockframework.runtime.extension.IMethodInvocation;

import java.lang.reflect.Field;

/**
 * Feature iteration interceptor which look up for a sniffer field declared in currently
 * executing test class and if such field is an instance of {@link SpockFeatureNameSniffer}
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
            SpockFeatureNameSniffer spockSniffer = lookupSpockSnifferField(invocation.getInstance());
            if (spockSniffer != null) {
                // and call it in order to record current test name
                spockSniffer.intercept(invocation);
            }
        }

        // allow Spock to continue
        invocation.proceed();
    }

    /**
     * Find the CloverNames.CLOVER_TEST_NAME_SNIFFER field in the current instance of a test class
     * Return instance assigned to this field if it's a SpockFeatureNameSniffer or <code>null</code>
     * otherwise.
     *
     * @return SpockFeatureNameSniffer instance or <code>null</code>
     */
    @Nullable
    protected SpockFeatureNameSniffer lookupSpockSnifferField(Object currentTestClass) {
        try {
            Field sniffer = currentTestClass.getClass().getField(CloverNames.CLOVER_TEST_NAME_SNIFFER);
            if (sniffer.getType().isAssignableFrom(TestNameSniffer.class)) {
                Object snifferObj = sniffer.get(null);
                if (snifferObj instanceof SpockFeatureNameSniffer) {
                    return (SpockFeatureNameSniffer)snifferObj;
                } else {
                    // field which was found is not an instance of the spock runner sniffer (maybe a junit4 sniffer?)
                    return null;
                }
            } else {
                Logger.getInstance().debug("Unexpected type of the "
                        + CloverNames.CLOVER_TEST_NAME_SNIFFER + " field: " + sniffer.getType().getName()
                        + " - ignoring. Test name found during instrumentation may differ from the actual name of the test at runtime.");
            }
        } catch (NoSuchFieldException ex) {
            Logger.getInstance().debug("Field " + CloverNames.CLOVER_TEST_NAME_SNIFFER
                    + " was not found in an instance of " + currentTestClass.getClass().getName()
                    + ". Test name found during instrumentation may differ from the actual name of the test at runtime.",
                    ex);
        } catch (SecurityException ex) {
            Logger.getInstance().debug("Field " + CloverNames.CLOVER_TEST_NAME_SNIFFER
                    + " couldn't be accessed in an instance of " + currentTestClass.getClass().getName()
                    + ". Test name found during instrumentation may differ from the actual name of the test at runtime."
                    , ex);
        } catch (IllegalAccessException ex) {
            Logger.getInstance().debug("Field " + CloverNames.CLOVER_TEST_NAME_SNIFFER
                    + " couldn't be accessed in an instance of " + currentTestClass.getClass().getName()
                    + ". Test name found during instrumentation may differ from the actual name of the test at runtime."
                    , ex);
        }

        // error when searching / accesing the field; return null
        return null;
    }
}
