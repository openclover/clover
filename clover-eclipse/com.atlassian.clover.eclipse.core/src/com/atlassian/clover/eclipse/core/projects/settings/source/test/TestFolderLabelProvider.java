package com.atlassian.clover.eclipse.core.projects.settings.source.test;

import com.atlassian.clover.eclipse.core.CloverPlugin;
import com.atlassian.clover.eclipse.core.ui.CloverPluginIcons;
import com.atlassian.clover.eclipse.core.projects.settings.source.InstrumentedSourceFoldersListener;
import com.atlassian.clover.eclipse.core.projects.settings.source.SourceFolderPattern;
import com.atlassian.clover.eclipse.core.projects.settings.source.SourceRootWithPattern;
import org.eclipse.core.commands.common.EventManager;
import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class TestFolderLabelProvider extends EventManager implements ILabelProvider, IColorProvider, InstrumentedSourceFoldersListener {
    private final ImageRegistry imageRegistry;

    private boolean perFolderSelectionEnabled;
    private static final String INSTRUMENTED_ICON = "instrumented";
    private static final String NOT_INSTRUMENTED_ICON = "notinstrumented";

    public TestFolderLabelProvider(boolean perFolderSelectionEnabled) {
        this.perFolderSelectionEnabled = perFolderSelectionEnabled;

        imageRegistry = new ImageRegistry();
        final ImageDescriptor instrumentedFolder = AbstractUIPlugin.imageDescriptorFromPlugin("org.eclipse.jdt.ui", "icons/full/obj16/packagefolder_obj.gif");
        if (instrumentedFolder != null) {
            imageRegistry.put(INSTRUMENTED_ICON, instrumentedFolder);

            final ImageDescriptor notInstrumentedFolderOverlay = CloverPlugin.getImageDescriptor(CloverPluginIcons.CLOVERED_NO_COMPILE_OVERLAY_ICON);
            if (notInstrumentedFolderOverlay != null) {
                imageRegistry.put(NOT_INSTRUMENTED_ICON, new CompositeImageDescriptor() {
                    @Override
                    protected void drawCompositeImage(int width, int height) {
                        drawImage(instrumentedFolder.getImageData(), 0, 0);
                        drawImage(notInstrumentedFolderOverlay.getImageData(), 0, 0);
                    }

                    @Override
                    protected Point getSize() {
                        final ImageData imgData = instrumentedFolder.getImageData();
                        return new Point(imgData.width, imgData.height);
                    }
                });
            }
        }
    }

    @Override
    public void dispose() {
        clearListeners();
        imageRegistry.dispose();
    }

    @Override
    public Image getImage(Object element) {
        SourceFolderPattern sfp = ((SourceRootWithPattern) element).getPattern();
        final boolean disabled = perFolderSelectionEnabled && !sfp.isEnabled();
        return imageRegistry.get(disabled ? NOT_INSTRUMENTED_ICON : INSTRUMENTED_ICON);
    }

    @Override
    public String getText(Object element) {
        SourceFolderPattern sfp = ((SourceRootWithPattern) element).getPattern();
        return sfp.getSrcPath();
    }

    @Override
    public void selectionChanged(SourceRootWithPattern pattern) {
        fireLabelProviderChanged(new LabelProviderChangedEvent(this, pattern));
    }

    @Override
    public void perFolderSelectionEnabled(boolean enabled) {
        perFolderSelectionEnabled = enabled;
        fireLabelProviderChanged(new LabelProviderChangedEvent(this));
    }

    @Override
    public Color getForeground(Object element) {
        SourceFolderPattern sfp = ((SourceRootWithPattern) element).getPattern();
        final boolean disabled = perFolderSelectionEnabled && !sfp.isEnabled();
        return disabled ? Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND) : null;
    }

    @Override
    public Color getBackground(Object element) {
        return null;
    }

    @Override
    public boolean isLabelProperty(Object element, String property) {
        return true;
    }

    @Override
    public void addListener(ILabelProviderListener listener) {
        addListenerObject(listener);
    }

    @Override
    public void removeListener(ILabelProviderListener listener) {
        removeListenerObject(listener);
    }

    protected void fireLabelProviderChanged(final LabelProviderChangedEvent event) {
        for (Object listener : getListeners()) {
            final ILabelProviderListener labelProviderListener = (ILabelProviderListener) listener;
            SafeRunnable.run(new SafeRunnable() {
                @Override
                public void run() {
                    labelProviderListener.labelProviderChanged(event);
                }
            });

        }
    }
}