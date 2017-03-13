package com.atlassian.clover.eclipse.testopt.actions;

import com.atlassian.clover.eclipse.testopt.OptimizedLaunchingConstants;
import com.atlassian.clover.eclipse.testopt.TestOptimizationPlugin;
import com.atlassian.clover.eclipse.testopt.TestOptimizationPluginMessages;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.actions.AbstractLaunchToolbarAction;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;

import java.util.Arrays;
import java.util.List;

import static clover.com.google.common.collect.Lists.newArrayList;

public class OptimizedLaunchToolbarAction extends AbstractLaunchToolbarAction {
    public OptimizedLaunchToolbarAction() {
        super(OptimizedLaunchingConstants.OPTIMIZED_LAUNCH_GROUP_ID);
    }

    @Override
    public void selectionChanged(IAction iAction, ISelection iSelection) {
        super.selectionChanged(iAction, iSelection);
        iAction.setEnabled(getLastLaunch() != null || ActionUtils.isInCloverNature(iSelection));
    }

    @Override
    protected ILaunchConfiguration[] getHistory() {
        final ILaunchConfiguration[] historyArray = super.getHistory();
        final ILaunchConfiguration lastConfiguration = TestOptimizationPlugin.getDefault().getLastLaunchConfiguration();
        if (lastConfiguration == null || (historyArray.length > 0 && historyArray[0] == lastConfiguration)) {
            return historyArray;
        }
        
        final List<ILaunchConfiguration> history = newArrayList(historyArray);
        history.remove(lastConfiguration);
        history.add(0, lastConfiguration);
        return history.toArray(new ILaunchConfiguration[history.size()]);
    }
    
    @Override
    protected ILaunchConfiguration getLastLaunch() {
        final ILaunchConfiguration lastConfiguration = TestOptimizationPlugin.getDefault().getLastLaunchConfiguration();
        return lastConfiguration != null ? lastConfiguration : super.getLastLaunch();
    }
    
    @Override
    public void run(IAction action) {
        // we don't want the Context Launch even if it is enabled by the user
        ILaunchConfiguration configuration = getLastLaunch();
        if (configuration == null) {
            DebugUITools.openLaunchConfigurationDialogOnGroup(DebugUIPlugin.getShell(), new StructuredSelection(), getLaunchGroupIdentifier());
        } else {
            DebugUITools.launch(configuration, getMode());
        }
    }
    
    @Override
    protected void updateTooltip() {
        ILaunchConfiguration configuration = getLastLaunch();
        final String message;
        if (configuration == null) {
            message = TestOptimizationPluginMessages.getString("launch.optimized.toolbar.label");
        } else {
            message = TestOptimizationPluginMessages.getFormattedString("launch.optimized.toolbar.tooltip", configuration.getName());
        }
        getAction().setToolTipText(message);
    }
}