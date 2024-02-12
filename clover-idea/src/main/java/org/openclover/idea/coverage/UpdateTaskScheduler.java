package org.openclover.idea.coverage;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.openclover.idea.util.tasks.CancellableTask;
import org.openclover.idea.util.tasks.CancellableTaskDelegate;

public class UpdateTaskScheduler {
    private final Project project;
    CancellableTaskDelegate pendingCoverageLoadTask;

    private UpdateCancellableTask currentTask;

    public UpdateTaskScheduler(Project project) {
        this.project = project;
    }

    public void scheduleReloadTask(final CancellableTaskDelegate taskDelegate) {
        if (!ApplicationManager.getApplication().isDispatchThread()) {
            ApplicationManager.getApplication().invokeLater(() -> scheduleReloadTask(taskDelegate));
        }

        //reload task takes precedence and makes currently running and pending ones obsolete
        if (currentTask != null) {
            pendingCoverageLoadTask = null;
            currentTask.cancel();
        }

        currentTask = new UpdateCancellableTask(taskDelegate, true);
        currentTask.queue();
    }

    public void scheduleCoverageLoadTask(final CancellableTaskDelegate taskDelegate) {
        if (!ApplicationManager.getApplication().isDispatchThread()) {
            ApplicationManager.getApplication().invokeLater(() -> scheduleCoverageLoadTask(taskDelegate));
        }
        if (currentTask == null) {
            pendingCoverageLoadTask = null;
            currentTask = new UpdateCancellableTask(taskDelegate, false);
            currentTask.queue();
        } else {
            pendingCoverageLoadTask = taskDelegate; // new coverage load request obsoletes any pending ones
        }
    }

    public boolean isReloading() {
        ApplicationManager.getApplication().assertIsDispatchThread();
        return currentTask != null && currentTask.isReload;
    }

    public boolean isLoadingCoverage() {
        ApplicationManager.getApplication().assertIsDispatchThread();
        return currentTask != null && !currentTask.isReload;
    }

    public void cancel() {
        ApplicationManager.getApplication().assertIsDispatchThread();
        pendingCoverageLoadTask = null;
        if (currentTask != null) {
            currentTask.cancel();
        }
    }

    private void schedule(CancellableTask finishedTask) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        if (currentTask == finishedTask) {
            // this may have been called due to cancel in scheduleReloadTask, new task may have been just started
            if (pendingCoverageLoadTask != null) {
                currentTask = new UpdateCancellableTask(pendingCoverageLoadTask, false);
                pendingCoverageLoadTask = null;
                currentTask.queue();
            } else {
                currentTask = null;
            }
        }
    }

    public void restartCurrentTask() {
        if (currentTask != null) {
            currentTask.cancel();
            currentTask = currentTask.copy();
            currentTask.queue();
        }

    }

    private class UpdateCancellableTask extends CancellableTask {
        private final boolean isReload;
        private final CancellableTaskDelegate delegate;
        
        private UpdateCancellableTask(@NotNull CancellableTaskDelegate delegate, boolean isReload) {
            super(project, delegate);
            this.isReload = isReload;
            this.delegate = delegate;
        }

        @Override
        protected void onFinish() {
            schedule(this);
        }

        private UpdateCancellableTask copy() {
            return new UpdateCancellableTask(delegate, isReload);
        }
    }

}
