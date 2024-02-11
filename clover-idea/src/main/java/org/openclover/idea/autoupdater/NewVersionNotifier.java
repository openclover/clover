package org.openclover.idea.autoupdater;

import org.openclover.core.versions.LibraryVersion;
import org.openclover.idea.PluginVersionInfo;
import org.openclover.idea.util.ui.CloverIcons;
import org.openclover.idea.util.l10n.CloverIdeaPluginMessages;
import org.openclover.idea.util.ui.ExceptionDialog;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.WindowManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.JFrame;

public class NewVersionNotifier implements ProjectComponent {
    private final Project project;
    private final AutoUpdateComponent autoUpdateComponent;
    private StatusBarWidget notificationIconWidget;

    public NewVersionNotifier(Project project, AutoUpdateComponent autoUpdate) {
        this.project = project;
        autoUpdateComponent = autoUpdate;
    }

    public void update() {
        if (autoUpdateComponent.showNewVersionAvailable()) {
            addNotification();
        } else {
            removeNotification();
        }
    }

    public void checkNow() {
        final LatestVersionInfo latestVersionInfo;
        try {
            latestVersionInfo = AutoUpdateComponent.checkLatestVersion();
        } catch (Exception e) {
            ExceptionDialog.showOKDialog(
                    null,
                    CloverIdeaPluginMessages.getString("autoupdate.errordownloadingdescriptor"),
                    "",
                    e,
                    CloverIdeaPluginMessages.getString("autoupdate.errordownloadingdescriptor"));
            return;
        }
        if (new LibraryVersion(PluginVersionInfo.RELEASE_NUMBER).compareTo(
                new LibraryVersion(latestVersionInfo.getNumber())) >= 0) {
            Messages.showMessageDialog(
                    project,
                    CloverIdeaPluginMessages.getFormattedString("autoupdate.alreadylatest", PluginVersionInfo.RELEASE_NUMBER),
                    CloverIdeaPluginMessages.getString("autoupdate.updatingplugin"),
                    CloverIcons.CLOVER_BIG);
        } else {
            popNotificationDialog(latestVersionInfo);
        }
    }

    private void popNotificationDialog() {
        final LatestVersionInfo latestVersionInfo = autoUpdateComponent.getLastVersion();
        if (latestVersionInfo != null) {
            autoUpdateComponent.resetLastVersion();
            popNotificationDialog(latestVersionInfo);
        }
    }

    public void popNotificationDialog(LatestVersionInfo latestVersionInfo) {
        final NewVersionDialog dialog = new NewVersionDialog(latestVersionInfo);
        dialog.show();
        switch (dialog.getExitCode()) {
            case NewVersionDialog.OK_EXIT_CODE:
                autoUpdateComponent.performUpdate(latestVersionInfo.getDownloadUrl());
                break;
            case NewVersionDialog.SKIP_VERSION_EXIT_CODE:
                autoUpdateComponent.addIgnoredVersion(latestVersionInfo.getNumber());
                break;
        }
    }

    private void addNotification() {
        final WindowManager windowManager = WindowManager.getInstance();
        final StatusBar statusBar = windowManager.getStatusBar(project);
        if (statusBar != null) {
            // add a widget only if was not added before (as method may be called multiple times)
            if (notificationIconWidget == null) {
                notificationIconWidget = new StatusBarCloverIconWidget(mouseEvent -> popNotificationDialog());
                statusBar.addWidget(notificationIconWidget);
                // force immediate refresh to show new icon
                final JFrame projectFrame = windowManager.getFrame(project);
                if (projectFrame != null) {
                    projectFrame.validate();
                }
            }
        }
    }

    private void removeNotification() {
        final WindowManager windowManager = WindowManager.getInstance();
        final StatusBar statusBar = windowManager.getStatusBar(project);
        if (statusBar != null) {
            if (notificationIconWidget != null) {
                statusBar.removeWidget(notificationIconWidget.ID());
                notificationIconWidget = null;
                // force immediate refresh to not have an empty space after icon removal
                windowManager.getFrame(project).validate();
            }
        }
    }

    @Override
    public void projectOpened() {
        update();
    }

    @Override
    public void projectClosed() {
        removeNotification();
    }

    @Override
    @NotNull
    public String getComponentName() {
        return "Clover New Version Notifier";
    }

    @Override
    public void initComponent() {
    }

    @Override
    public void disposeComponent() {
    }

    public static NewVersionNotifier getInstance(Project project) {
        return project != null ? project.getComponent(NewVersionNotifier.class) : null;
    }
}

