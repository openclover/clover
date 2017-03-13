package com.atlassian.clover.idea;

import com.atlassian.clover.idea.config.CloverGlobalConfig;
import com.atlassian.clover.util.ClassPathUtil;
import com.intellij.testFramework.IdeaTestCase;

import java.io.File;
import java.util.Date;

public class CloverPluginTest extends IdeaTestCase {
    private static final long ONE_HOUR = 3600L * 1000L;
    private static final long FIVE_SECONDS = 5L * 1000L;
    private static final long THIRTY_DAYS = 30L * 24L * 3600L * 1000L;

    public void testCalculateInstallDate() {
        // reset jar timestamp to current system time (as it could have been changed in other tests)
        final long now = System.currentTimeMillis();
        final File cloverJar = new File(ClassPathUtil.getCloverJarPath());
        cloverJar.setLastModified(now);

        final long classDirTs = cloverJar.lastModified();
        final CloverPlugin plugin = new CloverPlugin();

        // test: installation date is not set (-1) so return eariest of:
        //  * System#currentTimeMillis()
        //  * jar (or class dir) modification time stamp
        assertEquals(classDirTs, plugin.calculateInstallDate(new CloverGlobalConfig("", -1L)));
        assertEquals(classDirTs, plugin.calculateInstallDate(new CloverGlobalConfig("", Long.MAX_VALUE))); // ???

        // test: installation date is set in configuration (now - 1 hour) so it shall use this date instead of
        // current system time or jar (or class dir) time stamp
        final long olderValid = classDirTs - ONE_HOUR;
        assertEquals(olderValid, plugin.calculateInstallDate(new CloverGlobalConfig("", olderValid)));

        plugin.disposeComponent();
    }

    /**
     * Test for {@link CloverPlugin#loadState(com.atlassian.clover.idea.config.CloverGlobalConfig)}. Check if current
     * system time will be used in case when installation date is not set.
     */
    public void testLoadState_InstallationDateNotSet() {
        final CloverPlugin plugin = new CloverPlugin();

        // touch clover.jar or clover-core/target/classes to ensure that file timestamp is newer so that the
        // current system time shall be taken to calculate installation date when loading empty license
        long currentTime = System.currentTimeMillis();
        new File(ClassPathUtil.getCloverJarPath()).setLastModified(currentTime + 10000);

        // simulate loading state from serialized xml file (method is normally called from IDEA), use empty config
        plugin.loadState(new CloverGlobalConfig());

        // how to check if current time was used? plugin.loadState() should not take too much, 5 sec margin is enough
        assertTrue("Expected to have current system time ~ '" + new Date(currentTime) + "' (" + currentTime
                + ") but it was '" + new Date(plugin.getInstallDate()) + "' (" + plugin.getInstallDate() + ")",
                plugin.getInstallDate() >= currentTime && plugin.getInstallDate() <= currentTime + FIVE_SECONDS  );

        plugin.disposeComponent();
    }

    /**
     * Test for {@link CloverPlugin#loadState(com.atlassian.clover.idea.config.CloverGlobalConfig)}. Check if JAR file time
     * stamp will be used in case when installation from configuration file is later than file time stamp.
     */
    public void testLoadState_CorrectInstallDate() {
        final CloverPlugin plugin = new CloverPlugin();
        final long classDirTs = new File(ClassPathUtil.getCloverJarPath()).lastModified();

        // simulate loading state from serialized xml file (method is normally called from IDEA)
        // use configuration in which installation date is newer than jar modification date
        plugin.loadState(new CloverGlobalConfig("license-key", classDirTs + ONE_HOUR));

        // date from config > jar modification date? probably someone is cheating, use the jar's date
        assertEquals("Expected to have JAR time stamp " + new Date(classDirTs) + " but it was " + new Date(plugin.getInstallDate()),
                classDirTs, plugin.getInstallDate());

        plugin.disposeComponent();
    }

    /**
     * Test for {@link CloverPlugin#loadState(com.atlassian.clover.idea.config.CloverGlobalConfig)}. Case when no
     * correction of installation date is performed.
     */
    public void testLoadState_InstallDateFromConfig() {
        final CloverPlugin plugin = new CloverPlugin();
        final long classDirTs = new File(ClassPathUtil.getCloverJarPath()).lastModified();
        final long configTs = classDirTs - ONE_HOUR;

        // simulate loading state from serialized xml file (method is normally called from IDEA)
        // using configuration where installation date is older than jar modification date
        plugin.loadState(new CloverGlobalConfig("license-key", configTs));

        // date from config <= jar modification date? it's ok, use it
        assertEquals("Expected to have date '" + new Date(configTs) + "' (" + configTs
                + ") but it was '" + new Date(plugin.getInstallDate()) + "' (" + plugin.getInstallDate() + ")",
                configTs, plugin.getInstallDate());

        plugin.disposeComponent();
    }

}
