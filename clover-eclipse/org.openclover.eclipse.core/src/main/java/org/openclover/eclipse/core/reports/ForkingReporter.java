package org.openclover.eclipse.core.reports;

import org.openclover.eclipse.core.licensing.LicenseUtils;
import com.atlassian.clover.CloverStartup;
import com.atlassian.clover.Logger;

import java.lang.reflect.InvocationTargetException;

public abstract class ForkingReporter {
    public static final String FORKING_REPORTER_PROP = "clover.eclipse.forking.reporter";

    protected abstract int run(String[] args);

    private static ForkingReporter newReporter() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        final String forkingReporter = System.getProperty(FORKING_REPORTER_PROP);
        if (forkingReporter != null) {
            return (ForkingReporter)Class.forName(forkingReporter).newInstance();
        } else {
            throw new IllegalArgumentException("System property \"" + FORKING_REPORTER_PROP + "\" not set, can't generate report");
        }
    }

    public static void main(String[] args) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException, ClassNotFoundException {
        loadLicense();
        System.exit(newReporter().run(args));
    }

    private static void loadLicense() {
        CloverStartup.loadLicense(
            Logger.getInstance(),
            true,
            LicenseUtils.parseInstallDateToken(System.getProperty(LicenseUtils.LICENSE_TOKEN_PROP)));
    }
}
