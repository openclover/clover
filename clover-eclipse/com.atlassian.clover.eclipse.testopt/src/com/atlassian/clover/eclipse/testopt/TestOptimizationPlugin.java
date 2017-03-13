package com.atlassian.clover.eclipse.testopt;

import com.atlassian.clover.eclipse.core.CloverPlugin;
import com.atlassian.clover.optimization.OptimizationSession;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;

public class TestOptimizationPlugin extends AbstractUIPlugin {
    public static final String ID = "com.atlassian.clover.eclipse.testopt";
    
    public static final String USE_DEFAULT_SETTINGS = ID + ".USE_DEFAULT_SETTINGS";
    public static final String SHOW_NO_TESTS_FOUND_DIALOG = ID + ".SHOW_NO_TESTS_FOUND_DIALOG";
    public static final String DISCARD_STALE_SNAPSHOTS = ID + ".DISCARD_STALE_SNAPSHOTS";
    public static final String DISCARD_STALE_SNAPSHOTS_AGE = ID + ".DISCARD_STALE_SNAPSHOTS_AGE";
    public static final String MINIMIZE_TESTS = ID + ".MINIMIZE_TESTS";
    public static final String TEST_REORDERING = ID + ".TEST_REORDERING";
    public static final String LAST_LAUNCH_CONFIGURATION = "LAST_LAUNCH_CONFIGURATION";

    public static final String TEST_OPTIMIZATION_ICON = "icons/run_optimized.png";

    private final OptimizedTestRunListener optimizedTestRunListener = new OptimizedTestRunListener();
    
    private ILaunchConfiguration lastLaunchConfiguration;

    private static TestOptimizationPlugin instance;

    public static TestOptimizationPlugin getDefault() {
        return instance;
    }

    public TestOptimizationPlugin() {
        instance = this;
//        log(IStatus.INFO, "Test Optimization plugin constructed");
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        JUnitCore.addTestRunListener(optimizedTestRunListener);
        loadLastLaunchConfiguration();
//        log(IStatus.INFO, "Test Optimization plugin started");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        JUnitCore.removeTestRunListener(optimizedTestRunListener);
        super.stop(context);
//        log(IStatus.INFO, "Test Optimization plugin stopped");
    }

    public static void log(int eclipseLevel, String msg, Throwable t) {
        instance.getLog().log(new Status(eclipseLevel, ID, 0, msg, t));
    }

    public static void log(int eclipseLevel, String msg) {
        log(eclipseLevel, msg, null);
    }

    public static void logVerbose(String message) {
        logVerbose(message, null);
    }

    public static void logVerbose(String message, Throwable t) {
        log(IStatus.OK, message, t);
    }

    public static void logDebug(String message) {
        logDebug(message, null);
    }

    public static void logDebug(String message, Throwable t) {
        log(IStatus.OK, message, t);
    }

    public static void logInfo(String message) {
        logInfo(message, null);
    }

    public static void logInfo(String message, Throwable t) {
        log(IStatus.INFO, message, t);
    }

    public static void logWarning(String message) {
        logWarning(message, null);
    }

    public static void logWarning(String message, Throwable t) {
        log(IStatus.WARNING, message, t);
    }

    public static void logError(String message) {
        logError(message, null);
    }

    public static void logError(String message, Throwable t) {
        log(IStatus.ERROR, message, t);
    }

    public Image getTestOptimizationIcon() {
        ImageRegistry reg = getImageRegistry();
        if (reg.getDescriptor(TEST_OPTIMIZATION_ICON) == null) {
            reg.put(TEST_OPTIMIZATION_ICON, ImageDescriptor.createFromURL(instance.getBundle().getEntry(TEST_OPTIMIZATION_ICON)));
        }
        return reg.get(TEST_OPTIMIZATION_ICON);
    }

    private final Collection<OptimizationSessionListener> optimizationSessionListeners = new CopyOnWriteArraySet<OptimizationSessionListener>();

    public void addOptimizationSessionListener(OptimizationSessionListener listener) {
        optimizationSessionListeners.add(listener);
    }

    public void removeOptimizationSessionListener(OptimizationSessionListener listener) {
        optimizationSessionListeners.remove(listener);
    }

    public void notifyOptimizationSessionListener(OptimizationSession session) {
        for (OptimizationSessionListener listener : optimizationSessionListeners) {
            listener.sessionFinished(session);
        }
    }

    public ILaunchConfiguration getLastLaunchConfiguration() {
        return lastLaunchConfiguration;
    }

    public void setLastLaunchConfiguration(ILaunchConfiguration lastLaunchConfiguration) {
        if (this.lastLaunchConfiguration != lastLaunchConfiguration) {
            this.lastLaunchConfiguration = lastLaunchConfiguration;
            try {
                saveToPreferences(LAST_LAUNCH_CONFIGURATION, lastLaunchConfiguration != null ? lastLaunchConfiguration.getMemento() : null);
            } catch (CoreException e) {
                TestOptimizationPlugin.logError("Cannot store last launch configuration", e);
            } catch (BackingStoreException e) {
                TestOptimizationPlugin.logError("Cannot store last launch configuration", e);
            }
        }
    }

    private void saveToPreferences(final String key, final String value) throws BackingStoreException {
        final Preferences prefs = InstanceScope.INSTANCE.getNode(CloverPlugin.ID);
        prefs.put(key, value);
        prefs.flush();
    }

    private void loadLastLaunchConfiguration() {
        final Preferences prefs = InstanceScope.INSTANCE.getNode(CloverPlugin.ID);
        final String memento = prefs.get(LAST_LAUNCH_CONFIGURATION, "");
        try {
            lastLaunchConfiguration = memento == null  || memento.length() == 0 ? null : DebugPlugin.getDefault().getLaunchManager().getLaunchConfiguration(memento);
        } catch (CoreException e) {
            lastLaunchConfiguration = null;
            TestOptimizationPlugin.logWarning("Cannot read last launch configuration", e);
        }
        
    }

    
}
