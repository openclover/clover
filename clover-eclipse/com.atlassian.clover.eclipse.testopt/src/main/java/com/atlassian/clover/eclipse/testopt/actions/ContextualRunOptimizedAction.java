package com.atlassian.clover.eclipse.testopt.actions;

import com.atlassian.clover.eclipse.testopt.OptimizedLaunchingConstants;
import com.atlassian.clover.eclipse.core.ui.CloverPluginIcons;
import org.eclipse.debug.ui.actions.ContextualLaunchAction;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import java.util.Map;

import static clover.com.google.common.collect.Maps.newHashMap;

public class ContextualRunOptimizedAction extends ContextualLaunchAction {
    private ResourceManager imageManager;
    private Map images;

    public ContextualRunOptimizedAction() {
        super(OptimizedLaunchingConstants.OPTIMIZED_MODE);
        imageManager = new LocalResourceManager(JFaceResources.getResources());
        images = newHashMap();
        addImageForShortcutCommand(
            "com.atlassian.clover.eclipse.core.launching.LocalJavaApplicationShortcut" + "." + OptimizedLaunchingConstants.OPTIMIZED_MODE,
            "org.eclipse.jdt.debug.ui", "icons/full/etool16/java_app.gif");
        addImageForShortcutCommand(
            "com.atlassian.clover.eclipse.core.launching.LocalJavaAppletShortcut" + "." + OptimizedLaunchingConstants.OPTIMIZED_MODE,
            "org.eclipse.jdt.debug.ui", "icons/full/etool16/java_applet.gif");
        addImageForShortcutCommand(
            "com.atlassian.clover.eclipse.core.launching.WorkbenchPDEShortcut" + "." + OptimizedLaunchingConstants.OPTIMIZED_MODE,
            "org.eclipse.pde.ui", "icons/obj16/eclipse.gif");
        addImageForShortcutCommand(
            "com.atlassian.clover.eclipse.core.launching.JUnitShortcut" + "." + OptimizedLaunchingConstants.OPTIMIZED_MODE,
            "org.eclipse.jdt.junit", "icons/full/obj16/julaunch.gif");
        addImageForShortcutCommand(
            "com.atlassian.clover.eclipse.core.launching.JUnitPDEShortcut" + "." + OptimizedLaunchingConstants.OPTIMIZED_MODE,
            "org.eclipse.pde.ui", "icons/obj16/julaunchpgn.gif");
    }

    private void addImageForShortcutCommand(String name, String bundleName, String resourcePath) {
        final Image image = CloverPluginIcons.grabPluginImage(imageManager, bundleName, resourcePath);
        if (image != null) {
            images.put(name, image);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        imageManager.dispose();
        images.clear();
    }

    @Override
    protected void fillMenu(Menu menu) {
        super.fillMenu(menu);
        MenuItem[] items = menu.getItems();
        for (MenuItem item : items) {
            if (item.getData() instanceof ActionContributionItem) {
                String commandId = ((ActionContributionItem) item.getData()).getAction().getActionDefinitionId();
                if (images.containsKey(commandId)) {
                    item.setImage((Image) images.get(commandId));
                }
            }
        }
    }

    @Override
    public void selectionChanged(IAction action, ISelection iSelection) {
        super.selectionChanged(action, iSelection);
        if (action.isEnabled()) {
            action.setEnabled(ActionUtils.isInCloverNature(iSelection));
        }
    }
}