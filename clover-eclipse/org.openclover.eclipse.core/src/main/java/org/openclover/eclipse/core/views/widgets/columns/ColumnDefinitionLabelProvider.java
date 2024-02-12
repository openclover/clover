package org.openclover.eclipse.core.views.widgets.columns;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.services.IDisposable;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.ui.CloverPluginIcons;
import org.openclover.eclipse.core.views.ColumnDefinition;

public class ColumnDefinitionLabelProvider implements ITableLabelProvider, IDisposable {
    private ResourceManager imageManager;
    private Image customColumnImage;
    private Image builtinColumnImage;
    private Image builtinLockedColumnImage;
    private Image lockedColumnOverlayImage;
    private Image builtinColumnOverlayImage;

    public ColumnDefinitionLabelProvider() {
        imageManager = new LocalResourceManager(JFaceResources.getResources());
        try {
            customColumnImage =
                CloverPluginIcons.grabPluginImage(
                    imageManager,
                    "org.eclipse.ui.views",
                    "icons/full/eview16/prop_ps.gif");
            if (customColumnImage == null) {
                CloverPlugin.logWarning("Unable to find column icon");
            }
        } catch (Exception e) {
            CloverPlugin.logError("Error loading column icon", e);
        }

        if (customColumnImage != null) {
            try {
                builtinColumnOverlayImage = CloverPlugin.getImage(CloverPluginIcons.CLOVERED_OVERLAY_ICON);
                if (builtinColumnOverlayImage == null) {
                    CloverPlugin.logWarning("Unable to find builtin overlay icon");
                } else {
                    builtinColumnImage = imageManager.createImage(
                        new CompositeImageDescriptor() {
                            @Override
                            protected void drawCompositeImage(int width, int height) {
                                drawImage(customColumnImage.getImageData(), 0, 0);
                                drawImage(builtinColumnOverlayImage.getImageData(), 0, 0);
                            }

                            @Override
                            protected Point getSize() {
                                return new Point(
                                    customColumnImage.getBounds().width,
                                    customColumnImage.getBounds().height);
                            }
                        });
                }
            } catch (Exception e) {
                CloverPlugin.logError("Error loading builtin column icon", e);
            }

            try {
                lockedColumnOverlayImage = CloverPlugin.getImage(CloverPluginIcons.LOCKED_OVERLAY_ICON);
                if (builtinColumnOverlayImage != null && lockedColumnOverlayImage != null) {
                    builtinLockedColumnImage = imageManager.createImage(
                        new CompositeImageDescriptor() {
                            @Override
                            protected void drawCompositeImage(int width, int height) {
                                drawImage(customColumnImage.getImageData(), 0, 0);
                                drawImage(builtinColumnOverlayImage.getImageData(), 0, 0);
                                drawImage(lockedColumnOverlayImage.getImageData(), 0, 0);
                            }

                            @Override
                            protected Point getSize() {
                                return new Point(
                                    customColumnImage.getBounds().width,
                                    customColumnImage.getBounds().height);
                            }
                        });
                }
            } catch (Exception e) {
                CloverPlugin.logError("Error loading locked & builtin column icon", e);
            }
        }
    }

    @Override
    public String getColumnText(Object object, int col) {
        return ((ColumnDefinition)object).getTitle();
    }

    @Override
    public Image getColumnImage(Object object, int col) {
        ColumnDefinition columnDef = (ColumnDefinition) object;
        if (columnDef.isCustom()) {
            return customColumnImage;
        } else {
            if (columnDef.isLocked()) {
                return builtinLockedColumnImage;
            } else {
                return builtinColumnImage;
            }
        }
    }

    @Override
    public void dispose() {
        imageManager.dispose();
    }

    @Override
    public boolean isLabelProperty(Object object, String name) {
        return false;
    }

    @Override
    public void addListener(ILabelProviderListener listener) {

    }

    @Override
    public void removeListener(ILabelProviderListener listener) {

    }
}
