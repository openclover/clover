package org.openclover.eclipse.core.views.widgets;

import org.openclover.eclipse.core.ui.CloverPluginIcons;
import org.openclover.eclipse.core.ui.GLH;
import org.openclover.eclipse.core.ui.SwtUtils;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.ui.projects.DatabaseChangeEvent;
import org.openclover.eclipse.core.ui.projects.DatabaseChangeListener;
import org.openclover.eclipse.core.upgrade.hooks.ConfigUninstaller;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Control;

import java.util.Set;

import static org.openclover.util.Sets.newHashSet;

public class ViewAlertContainer extends Composite implements DatabaseChangeListener {

    private Composite alerts;
    private Composite content;
    private Alert hookUninstallLink;

    public ViewAlertContainer(Composite parent) {
        super(parent, SWT.NONE);
        setLayout(new GLH(1, false).marginHeight(0).marginWidth(0).verticalSpacing(0).getGridLayout());

        alerts = new Composite(this, SWT.NONE);
        alerts.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        alerts.setLayout(new GLH(1, false).marginHeight(0).marginWidth(0).verticalSpacing(0).getGridLayout());

        hookUninstallLink =
            new Alert(
                AlertStyle.WARNING,
                alerts,
                false,
                "Clover has been upgraded but needs your attention.",
                "You have upgraded from a previous version of Clover which required modification to Eclipse's config.ini file. " +
                "These modifications are no longer required. <a>Clover can undo these changes for you</a>.");
        hookUninstallLink.addExplanationLinkListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                if (new ConfigUninstaller().run()) {
                    updateHookUninstallAlert();
                }
            }
        });
    }

    public void setContent(SashForm content) {
        this.content = content;
    }

    @Override
    public void databaseChanged(DatabaseChangeEvent event) {
        if (event.isForWorkspace()) {
            updateLinks(false);
        }
    }

    private void updateHookUninstallAlert() {
        if (new ConfigUninstaller().isHookInstalled()) {
            SwtUtils.gridDataFor(hookUninstallLink).exclude = false;
        } else {
            SwtUtils.gridDataFor(hookUninstallLink).exclude = true;
        }
        hookUninstallLink.setVisible(!SwtUtils.gridDataFor(hookUninstallLink).exclude);
    }

    public void updateLinks() {
        updateLinks(true);
    }

    private void updateLinks(boolean onInit) {
        run(() -> {
            updateHookUninstallAlert();
            updateContainerVisibility();
        }, onInit);
    }

    private void updateContainerVisibility() {
        SwtUtils.gridDataFor(alerts).exclude =
            SwtUtils.gridDataFor(hookUninstallLink).exclude;
        alerts.setVisible(!SwtUtils.gridDataFor(alerts).exclude);
        getParent().layout(true, true);
    }

    private void run(Runnable work, boolean onInit) {
        if (onInit) {
            work.run();
        } else {
            Display.getDefault().syncExec(work);
        }
    }

    private enum AlertStyle {
        INFO(IStatus.INFO, "icons/full/obj16/info_tsk.gif") {
            @Override
            public Color getColor(Device device, ResourceManager resourceManager) {
                try {
                    return resourceManager.createColor(new RGB(199, 235, 242));
                } catch (Exception e) {
                    CloverPlugin.logError("Unable to create info colour", e);
                    return device.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
                }
            }
        },
        WARNING(IStatus.WARNING, "icons/full/obj16/warn_tsk.gif") {
            @Override
            public Color getColor(Device device, ResourceManager resourceManager) {
                return device.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
            }
        },
        ERROR(IStatus.ERROR, "icons/full/obj16/error_tsk.gif") {
            @Override
            public Color getColor(Device device, ResourceManager resourceManager) {
                return device.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
            }
        };

        private final int status;
        private final String iconPath;

        private AlertStyle(int status, String iconPath) {
            this.status = status;
            this.iconPath = iconPath;
        }
        
        public abstract Color getColor(Device device, ResourceManager resourceManager);

        public int getStatus() {
            return status;
        }

        public Image getIcon(ResourceManager imageManager) {
            return CloverPluginIcons.grabPluginImage(
                    imageManager,
                    "org.eclipse.ui",
                    iconPath);
        }
    }

    private class Alert extends Composite {
        private final Link link;
        private final Label icon;
        private final ResourceManager imageManager;
        private final ViewAlertContainer.AlertPopup alertPopup;
        private final boolean dismissable;

        private Alert(AlertStyle style, Composite parent, boolean dismissable, String text, String explanation) {
            super(parent, SWT.BORDER);
            this.dismissable = dismissable;
            setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            setLayout(new GridLayout(dismissable ? 3 : 2, false));
            
            imageManager = new LocalResourceManager(JFaceResources.getResources());

            Color background = style.getColor(getShell().getDisplay(), imageManager);
            setBackground(background);

            icon = new Label(this, SWT.CENTER);
            icon.setImage(style.getIcon(imageManager));
            icon.setLayoutData(new GridData());
            icon.setBackground(background);

            link = new Link(this, SWT.NONE);
            setText(text, explanation);
            link.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            link.setBackground(background);

            alertPopup = new AlertPopup(getShell(), explanation);
            link.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent selectionEvent) {
                    final Point cursorLocation = Display.getDefault().getCursorLocation();
                    alertPopup.open(
                        new Point(cursorLocation.x + 20, cursorLocation.y));
                }
            });

            if (dismissable) {
                final Label closeLabel = new Label(this, SWT.NONE);
                SwtUtils.gridDataFor(closeLabel).horizontalAlignment = GridData.END;
                closeLabel.setImage(
                    CloverPluginIcons.grabPluginImage(
                        imageManager,
                        "org.eclipse.ui",
                        "icons/full/elcl16/close_view.gif"));
                closeLabel.setBackground(background);
                closeLabel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseUp(MouseEvent mouseEvent) {
                        SwtUtils.gridDataFor(Alert.this).exclude = true;
                        Alert.this.setVisible(false);
                        updateContainerVisibility();
                    }
                });
            }
        }

        private void setText(String linkText, String explanation) {
            if (explanation != null && explanation.length() != 0) {
                link.setText(String.format("%s <a>More...</a>", linkText));
            } else {
                link.setText(linkText);
            }
        }

        public void addExplanationLinkListener(SelectionListener listener) {
            alertPopup.addLinkListener(listener);
        }

        @Override
        public void dispose() {
            super.dispose();
            imageManager.dispose();
        }
    }

    private class AlertPopup extends PopupDialog {
        private final String text;
        private Link link;
        private Point location;
        private Set<SelectionListener> pendingListeners = newHashSet();

        public AlertPopup(Shell parent, String text) {
            super(parent, INFOPOPUP_SHELLSTYLE, true, false, false, false, false, null, null);
            this.text = text;
        }

        @Override
        protected Control createDialogArea(Composite parent) {
            //Use an inner composite to give some margin
            Composite container = new Composite(parent, SWT.NONE);
            container.setLayout(new GridLayout(1, false));

            link = new Link(container, SWT.NONE);
            link.setText(text);
            SwtUtils.gridDataFor(link).widthHint = 350;
            for (SelectionListener pendingListener : pendingListeners) {
                link.addSelectionListener(pendingListener);
            }
            return link;
        }

        public void addLinkListener(SelectionListener listener) {
            if (link != null) {
                link.addSelectionListener(listener);
            } else {
                pendingListeners.add(listener);
            }
        }

        public void open(Point location) {
            this.location = location;
            open();
        }

        @Override
        protected Point getInitialLocation(Point point) {
            return location == null ? point : location;
        }
    }
}
