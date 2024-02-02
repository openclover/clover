package org.openclover.idea.actions;

import org.openclover.idea.IProjectPlugin;
import org.openclover.idea.ProjectPlugin;
import com.atlassian.clover.optimization.Snapshot;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

import java.io.File;

public class CleanSnapshotAction extends AnAction {

    private File findSnapshotFile(AnActionEvent e) {
        final IProjectPlugin plugin = ProjectPlugin.getPlugin(e);
        if (plugin == null) {
            return null;
        }

        return Snapshot.fileForInitString(plugin.getCoverageManager().getCoverageDatabasePath());
    }

    @Override
    public void update(AnActionEvent e) {
        final File snapshotFile = findSnapshotFile(e);
        e.getPresentation().setVisible(snapshotFile != null && snapshotFile.exists());
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        final File snapshotFile = findSnapshotFile(e);
        if (snapshotFile != null && snapshotFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            snapshotFile.delete();
        }
    }
}
