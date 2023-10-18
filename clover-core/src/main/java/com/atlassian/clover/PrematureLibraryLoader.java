package com.atlassian.clover;

import clover.org.jfree.chart.ChartFactory;
import clover.org.jfree.chart.ChartUtilities;
import clover.org.jfree.chart.JFreeChart;
import clover.org.jfree.chart.plot.PlotOrientation;
import clover.org.jfree.data.xy.XYSeriesCollection;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Executes operations that should ensure certain libraries are loaded early on in the build.
 * This is necessary because on XP has a bug where reads on Stdin (ie System.in.read())
 * will block library loading. This is not normally a problem because most loads occur at the
 * beginning of a build anyway. Ant reads stdin via the StreamPumper thread.
 * <ul>
 *   <li><a href="https://weblogs.java.net/blog/kohsuke/archive/2009/09/28/reading-stdin-may-cause-your-jvm-hang">reading-stdin-may-cause-your-jvm-hang</a></li>
 *   <li><a href="https://connect.microsoft.com/VisualStudio/feedback/ViewFeedback.aspx?FeedbackID=94701">FeedbackID=94701</a></li>
 * </ul>
 **/
public class PrematureLibraryLoader {
    public static final AtomicBoolean DONE = new AtomicBoolean(false);

    /**
     * Attempts to load the libraries by executing various harmless actions. Every effort is made
     * to load each library even in the face of unexpected errors
     */
    public static void doOnce() {
        if (isWindows()) {
            if (DONE.compareAndSet(false, true)) {
                try {
                    class NullOutputStream extends OutputStream {
                        @Override
                        public void write(int b) throws IOException { /*NO-OP*/ }
                    }
    
                    //libraries: awt, fontmanager, jpeg, cmm
                    Logger.getInstance().debug("Causing libraries \"awt\", \"fontmanager\" to be initialized");
                    final JFreeChart chart = ChartFactory.createHistogram("", "", "", new XYSeriesCollection(), PlotOrientation.VERTICAL, false, true, false);
                    try {
                        Logger.getInstance().debug("Causing library \"jpg\" to be initialized");
                        ChartUtilities.writeChartAsJPEG(new NullOutputStream(), chart, 1, 1);
                        Logger.getInstance().debug("Causing library \"cmm\" to be initialized");
                        ChartUtilities.writeChartAsPNG(new NullOutputStream(), chart, 1, 1);
                    } catch (Exception e) {
                        //Ignore
                    }
                    AccessController.doPrivileged(new PrivilegedAction<Void>() {
                        @Override
                        public Void run() {
                            try {
                                //libraries: security (secure random)
                                File tempFile = null;
                                try {
                                    Logger.getInstance().debug("Causing library \"security\" to be initialized");
                                    tempFile = File.createTempFile("clover", "init");
                                } catch (IOException e) {
                                    //Ignore
                                }
                                try {
                                    //libraries: nio, net
                                    if (tempFile != null) {
                                        Logger.getInstance().debug("Causing libraries \"nio\" and \"net\" to be initialized");
                                        new RandomAccessFile(tempFile.getAbsolutePath(), "r").getChannel();
                                    }
                                } finally {
                                    if (tempFile != null) {
                                        tempFile.delete();
                                    }
                                }
                            } catch (IOException e) {
                                //Ignore
                            }
                            return null;
                        }
                    });
                } catch (Exception e) {
                    Logger.getInstance().warn("Failed to prematurely load security, nio, net, awt, fontmanager, jpeg or cmm libraries", e);
                }
            }
        } else {
            Logger.getInstance().debug("Not preloading libraries as os.name does not start with 'windows'");
        }
    }

    private static boolean isWindows() {
        String osName = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() { return System.getProperty("os.name"); }
        });
        return osName != null && osName.toLowerCase().indexOf("windows") == 0;
    }
}
