package com.atlassian.clover.recorder;

import com.atlassian.clover.CloverNames;
import com.atlassian.clover.Logger;
import com_atlassian_clover.TestNameSniffer;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

public class TestNameSnifferHelper {

    /**
     * Find the CloverNames.CLOVER_TEST_NAME_SNIFFER field in the current instance of a test class Return instance
     * assigned to this field if it's a TestNameSniffer or <code>null</code> otherwise.
     *
     * @return TestNameSniffer instance or <code>null</code>
     */
    @Nullable
    public static TestNameSniffer lookupTestSnifferField(Class currentTestClass) {
        try {
            Field sniffer = currentTestClass.getField(CloverNames.CLOVER_TEST_NAME_SNIFFER);
            if (sniffer.getType().isAssignableFrom(TestNameSniffer.class)) {
                return (TestNameSniffer) sniffer.get(null);
            } else {
                Logger.getInstance().debug("Unexpected type of the "
                        + CloverNames.CLOVER_TEST_NAME_SNIFFER + " field: " + sniffer.getType().getName()
                        + " - ignoring. Test name found during instrumentation may differ from the actual name of the test at runtime.");
            }
        } catch (NoSuchFieldException ex) {
            Logger.getInstance().debug("Field " + CloverNames.CLOVER_TEST_NAME_SNIFFER
                            + " was not found in an instance of " + currentTestClass.getName()
                            + ". Test name found during instrumentation may differ from the actual name of the test at runtime.",
                    ex);
        } catch (SecurityException ex) {
            Logger.getInstance().debug("Field " + CloverNames.CLOVER_TEST_NAME_SNIFFER
                            + " couldn't be accessed in an instance of " + currentTestClass.getName()
                            + ". Test name found during instrumentation may differ from the actual name of the test at runtime.",
                    ex);
        } catch (IllegalAccessException ex) {
            Logger.getInstance().debug("Field " + CloverNames.CLOVER_TEST_NAME_SNIFFER
                            + " couldn't be accessed in an instance of " + currentTestClass.getName()
                            + ". Test name found during instrumentation may differ from the actual name of the test at runtime.",
                    ex);
        }

        // error when searching / accesing the field; return null
        return null;
    }

}
