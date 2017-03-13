package com.atlassian.clover.eclipse.testopt;

import com.atlassian.clover.api.CloverException;
import com.atlassian.clover.eclipse.core.projects.CloverProject;
import com.atlassian.clover.registry.Clover2Registry;
import com.atlassian.clover.optimization.LocalSnapshotOptimizer;
import com.atlassian.clover.optimization.Messages;
import com.atlassian.clover.api.optimization.Optimizable;
import com.atlassian.clover.optimization.OptimizationSession;

import com.atlassian.clover.api.optimization.OptimizationOptions;
import com.atlassian.clover.optimization.Snapshot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CloveredOptimizedLauncherDelegate extends JUnitLaunchConfigurationDelegate {


    @Override
    protected IMember[] evaluateTests(ILaunchConfiguration configuration, IProgressMonitor monitor) throws CoreException {
        final IMember[] allTests = super.evaluateTests(configuration, monitor);
        final IJavaProject project = getJavaProject(configuration);
        OptimizationSession[] sessionHolder = new OptimizationSession[] {null};
        if (project == null) {
            TestOptimizationPlugin.logWarning("Cannot optimize: unable to retrieve IJavaProject for launch configuration "
                    + configuration.getName());
            reportSavings(null);
            return allTests;
        } else {
            OptimizationOptions.Builder optionsTemplate = createOptionsTemplate(configuration);
            optionsTemplate.optimizableName("class");
            final IMember[] result = optimize(allTests, project, optionsTemplate, sessionHolder);
            reportSavings(sessionHolder[0]);
            if (result.length != 0) {
                return result;
            } else {
                final String msg = TestOptimizationPluginMessages.getString("launch.optimized.notestsfound");
                final IPreferenceStore pluginPreferences = TestOptimizationPlugin.getDefault().getPreferenceStore();
                if (pluginPreferences.getBoolean(TestOptimizationPlugin.SHOW_NO_TESTS_FOUND_DIALOG)) {
                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            MessageDialogWithToggle mdwt =
                                    MessageDialogWithToggle.openInformation(null,
                                                                            TestOptimizationPluginMessages.getPluginName(),
                                                                            msg,
                                                                            TestOptimizationPluginMessages.getString("launch.optimized.notestsfound.ask"),
                                                                            true, null, null);
                            pluginPreferences.setValue(TestOptimizationPlugin.SHOW_NO_TESTS_FOUND_DIALOG, mdwt.getToggleState());
                        }
                    });
                }
                final IStatus status = new Status(IStatus.INFO, TestOptimizationPlugin.ID, msg);
                throw new CoreException(status);
            }
        }
    }

    private OptimizationOptions.Builder createOptionsTemplate(ILaunchConfiguration configuration) {
        final IPreferenceStore defaults = TestOptimizationPlugin.getDefault().getPreferenceStore();
        boolean useDefaults;
        try {
            useDefaults = configuration.getAttribute(TestOptimizationPlugin.USE_DEFAULT_SETTINGS, true);
        } catch (CoreException e) {
            useDefaults = true;
            TestOptimizationPlugin.logWarning("Problem retrieving launch configuration attribute", e);
        }
        final OptimizationOptions.Builder builder = new OptimizationOptions.Builder();
        try {
            final boolean defaultDiscardStale = defaults.getBoolean(TestOptimizationPlugin.DISCARD_STALE_SNAPSHOTS);
            final boolean discardStale = useDefaults ? defaultDiscardStale
                    : configuration.getAttribute(TestOptimizationPlugin.DISCARD_STALE_SNAPSHOTS, defaultDiscardStale);

            final int defaultDiscardAge = defaults.getInt(TestOptimizationPlugin.DISCARD_STALE_SNAPSHOTS_AGE);
            final int discardAge;
            if (discardStale) {
                discardAge = useDefaults ? defaultDiscardAge
                        : configuration.getAttribute(TestOptimizationPlugin.DISCARD_STALE_SNAPSHOTS_AGE, defaultDiscardAge);
            } else {
                discardAge = Integer.MAX_VALUE;
            }
            builder.maxCompilesBeforeStaleSnapshot(discardAge);
        } catch (CoreException e) {
            TestOptimizationPlugin.logWarning("Problem retrieving launch configuration attribute", e);
        }

        try {
            final boolean defaultMinimizeTests = defaults.getBoolean(TestOptimizationPlugin.MINIMIZE_TESTS);
            final boolean minimizeTests = useDefaults ? defaultMinimizeTests
                    : configuration.getAttribute(TestOptimizationPlugin.MINIMIZE_TESTS, defaultMinimizeTests);
            builder.minimize(minimizeTests);
        } catch (CoreException e) {
            TestOptimizationPlugin.logWarning("Problem retrieving launch configuration attribute", e);
        }

        try {
            final String defaultReorderingStr = defaults.getString(TestOptimizationPlugin.TEST_REORDERING);
            final String reorderingStr = useDefaults ? defaultReorderingStr
                    : configuration.getAttribute(TestOptimizationPlugin.TEST_REORDERING, defaultReorderingStr);
            final OptimizationOptions.TestSortOrder reordering = OptimizationOptions.TestSortOrder.valueOf(reorderingStr);
            builder.reorder(reordering);
        } catch (CoreException e) {
            TestOptimizationPlugin.logWarning("Problem retrieving launch configuration attribute", e);
        } catch (IllegalArgumentException e) {
            TestOptimizationPlugin.logWarning("Illegal test optimization parameter value", e);
        }

        return builder;
    }


    @Override
    public IVMRunner getVMRunner(ILaunchConfiguration configuration, String mode) throws CoreException {
        TestOptimizationPlugin.getDefault().setLastLaunchConfiguration(configuration);
        return super.getVMRunner(configuration, ILaunchManager.RUN_MODE);
    }

    private IMember[] optimize(IMember[] allTests, IJavaProject project, OptimizationOptions.Builder optionsTemplate, OptimizationSession[] sessionHolder) {
        final OptimizationOptions options;
        try {
            final CloverProject cloverProject = CloverProject.getFor(project);
            if (cloverProject == null) {
                TestOptimizationPlugin.logWarning("Cannot optimize: Clover not enabled for project " + project.getElementName());
                return allTests;
            }

            final String initString = cloverProject.deriveInitString();

            options = optionsTemplate
                    .initString(initString)
                    .snapshot(Snapshot.fileForInitString(initString)).build();

        } catch (CoreException e) {
            TestOptimizationPlugin.logWarning("Cannot optimize: unable to retrieve Clover settings for project " + project.getElementName(), e);
            return allTests;
        }
        TestOptimizationPlugin.logDebug("Optimizing with options: " + options.toString());

        final Snapshot snapshot = loadSnapshot(options);
        if (snapshot == null) {
            return allTests;
        }
        
        final Clover2Registry registry = loadRegistry(options);
        if (registry == null) {
            return allTests;
        }

        final Collection<IMemberAdapter> input = new ArrayList<IMemberAdapter>(allTests.length);
        for (IMember test : allTests) {
            input.add(new IMemberAdapter(test));
        }

        final LocalSnapshotOptimizer optimizer = new LocalSnapshotOptimizer(snapshot, registry, options);
        if (optimizer.canOptimize()) {
            sessionHolder[0] = new OptimizationSession(options);
            final List<IMemberAdapter> optimized = optimizer.optimize(input, sessionHolder[0]);

            final IMember[] optimizedMembers = new IMember[optimized.size()];
            int idx = 0;
            for (IMemberAdapter optimizable : optimized) {
                optimizedMembers[idx++] = optimizable.getIMember();
            }
            return optimizedMembers;

        } else {
            TestOptimizationPlugin.logWarning("Cannot optimize: " + optimizer.cannotOptimizeCause());
            return allTests;
        }
    }

    private Snapshot loadSnapshot(OptimizationOptions options) {
        if (options.getSnapshotFile() == null) {
            TestOptimizationPlugin.logWarning("Cannot optimize: no snapshot file defined");
            return null;
        }

        final Snapshot snapshot = Snapshot.loadFrom(options.getSnapshotFile());
        if (snapshot == null) {
            TestOptimizationPlugin.logInfo("Cannot optimize: Cannot load snapshot file from " +
                    options.getSnapshotFile());
            return null;
        }
        if (snapshot.isTooStale(options.getMaxCompilesBeforeStaleSnapshot())) {
            TestOptimizationPlugin.logInfo("Cannot optimize: Maximum count of compilations before snapshot stale exceeded (" +
                    options.getMaxCompilesBeforeStaleSnapshot() + ")");
            snapshot.delete();
            return null;
        } else {
            return snapshot;
        }
    }
    
    private Clover2Registry loadRegistry(OptimizationOptions options) {
        if (options.getInitString() == null) {
            TestOptimizationPlugin.logWarning("Cannot optimize: initstring not defined");
            return null;
        }
        try {
            Clover2Registry registry = Clover2Registry.fromFile(new File(options.getInitString()));
            if (registry == null) {
                TestOptimizationPlugin.logWarning("Cannot optimize: cannot load registry from " +
                        options.getInitString());
            }
            return registry;
        } catch (CloverException e) {
            TestOptimizationPlugin.logWarning(Messages.noOptimizationBecauseOfException(e));
            return null;
        }
    }

    private void reportSavings(OptimizationSession session) {
        TestOptimizationPlugin.getDefault().notifyOptimizationSessionListener(session);
    }
}

class IMemberAdapter implements Optimizable {
    private final IMember iMember;

    public IMemberAdapter(IMember iMember) {
        this.iMember = iMember;
    }

    @Override
    public String getName() {
        if (iMember instanceof IType) {
            return ((IType) iMember).getFullyQualifiedName('.');
        } else {
            TestOptimizationPlugin.logError("Unexpected iMember type " + iMember.getClass().getName() + ": " + iMember.getElementName());
            return iMember.getElementName();
        }
    }

    public IMember getIMember() {
        return iMember;
    }

    @Override
    public String toString() {
        return "Optimizable: " + getName();
    }
}
