package org.openclover.eclipse.core.launching.actions;

import org.openclover.eclipse.core.launching.LaunchingConstants;
import org.openclover.eclipse.core.ui.CloverPluginIcons;
import org.eclipse.debug.ui.actions.ContextualLaunchAction;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import java.util.Map;

import static org.openclover.core.util.Maps.newHashMap;

public class ContextualRunWithCloverAction extends ContextualLaunchAction {
    private ResourceManager imageManager;
    private Map<String, Image> images;

    public ContextualRunWithCloverAction() {
        super(LaunchingConstants.CLOVER_MODE);
        imageManager = new LocalResourceManager(JFaceResources.getResources());
        images = newHashMap();
        addImageForShortcutCommand(
            "org.openclover.eclipse.core.launching.LocalJavaApplicationShortcut" + "." + LaunchingConstants.CLOVER_MODE,
            "org.eclipse.jdt.debug.ui", "icons/full/etool16/java_app.gif");
        addImageForShortcutCommand(
            "org.openclover.eclipse.core.launching.LocalJavaAppletShortcut" + "." + LaunchingConstants.CLOVER_MODE,
            "org.eclipse.jdt.debug.ui", "icons/full/etool16/java_applet.gif");
        addImageForShortcutCommand(
            "org.openclover.eclipse.core.launching.WorkbenchPDEShortcut" + "." + LaunchingConstants.CLOVER_MODE,
            "org.eclipse.pde.ui", "icons/obj16/eclipse.gif");
        addImageForShortcutCommand(
            "org.openclover.eclipse.core.launching.JUnitShortcut" + "." + LaunchingConstants.CLOVER_MODE,
            "org.eclipse.jdt.junit", "icons/full/obj16/julaunch.gif");
        addImageForShortcutCommand(
            "org.openclover.eclipse.core.launching.JUnitPDEShortcut" + "." + LaunchingConstants.CLOVER_MODE,
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
                    item.setImage(images.get(commandId));
                }
            }
        }
    }
}
