package org.openclover.ci;

import org.openclover.runtime.Logger;
import org.openclover.util.ClassPathUtil;
import org.openclover.runtime.CloverNames;
import com.atlassian.clover.api.ci.Integrator;
import com.atlassian.clover.api.ci.CIOptions;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;

/**
 */
public class AntIntegrator implements Integrator {

    private final boolean isWindows = isWindows();

    private final CIOptions options;

    /**
     * Creates an integrator for Ant, using the given options.
     */
    public AntIntegrator(CIOptions opts) {
        this.options = opts;
    }

    /**
     * Given a list of command line arguments, this method will add clover specific arguments which will enable
     * Clover for any given Ant Build.
     *
     * @param args the list of args to decorate. This list must be arguments to the build tool only, and may
     * not include the actual command that invokes the build too. e.g. {"clean", "test"}. and *not* {"ant", "clean", "test"}
     */
    @Override
    public void decorateArguments(List<String> args) {

        if (options.isFullClean()) {
            args.add(0, "clover.fullclean");
        }
        if (!options.isJson()) {
            args.add("-Dclover.skip.json=true");
        }
        if (!options.isHistorical()) {
            args.add("-Dclover.skip.report=true");            
        }
        if (options.isHistorical()) {
            args.add("-Dclover.skip.current=true");    
        }
        if (options.getHistoryDir() != null) {
            args.add("-Dclover.historydir=" + addQuotesIfNecessary(options.getHistoryDir().getAbsolutePath()));
        }

        args.add("-D" + CloverNames.PROP_CLOVER_OPTIMIZATION_ENABLED + "="+ options.isOptimize());

        final String cloverJarLocation = ClassPathUtil.getCloverJarPath();
        if (cloverJarLocation != null) {
            args.add("-lib");
            args.add("\"" + cloverJarLocation + "\"");
        } else {
            Logger.getInstance().warn("The location of the clover jar could not be determined. Please supply '-lib /path/to/clover.jar' directly.");
        }
        args.add("-listener");
        args.add(AntIntegrationListener.class.getName());
        if (options.getLicenseCert() != null && !options.getLicenseCert().trim().equals("")) {
            args.add("-D" + CloverNames.PROP_LICENSE_CERT + "=" + addQuotesIfNecessary(options.getLicenseCert()));
        }
        if (options.getLicense() != null) {
            args.add("-D" + CloverNames.PROP_LICENSE_PATH + "=" + addQuotesIfNecessary(options.getLicense().getAbsolutePath()));
        }
    }

    /**
     * Don't add quotes on Windows, because it causes problems when passing such -Dname=value args to JVM via exec.
     * Don't add quotes for new versions of Ant either (by default the isPutValuesInQuotes is false)
     * as since Ant 1.9.7 problem of passing args to JVM has been fixed.
     * See CLOV-1956, BAM-10740 and BDEV-11740 for more details.
     */
    private String addQuotesIfNecessary(String input) {
        return isWindows || !options.isPutValuesInQuotes() ? input : '"' + input + '"';
    }

    private static boolean isWindows() {
        final String osName = AccessController.doPrivileged((PrivilegedAction<String>) () -> {
            try {
                return System.getProperty("os.name");
            } catch (SecurityException ex) {
                return null;
            }
        });
        return osName != null && osName.toLowerCase().indexOf("windows") == 0;
    }

}
