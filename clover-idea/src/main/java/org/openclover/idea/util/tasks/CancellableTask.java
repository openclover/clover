package org.openclover.idea.util.tasks;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openclover.idea.util.MiscUtils;
import org.openclover.runtime.Logger;

public class CancellableTask extends Task.Backgroundable {
    private final CancellableTaskDelegate delegate;
    private ProgressIndicator progressIndicator;
    private boolean alreadyCancelled;

    public CancellableTask(@Nullable final Project project,
                           @NotNull final CancellableTaskDelegate delegate) {
        super(project, delegate.getTitle());
        this.delegate = delegate;
    }

    @Override
    public final void run(@NotNull final ProgressIndicator indicator) {
        setProgressIndicator(indicator);
        try {
            delegate.run(indicator);
            // Note: the IDEA's ProgressManagerImpl does NOT call onSuccess()/onCancel() post-task actions when it runs
            // in unit test mode; for this reason we call onSuccess() manually
            if (isHeadless()) {
                // ensure that we run from a dispatch thread (for unit test mode)
                MiscUtils.invokeLater(this::onSuccess);
            }
        } catch (ProcessCanceledException e) {
            throw e;
        } catch (Exception e) {
            Logger.getInstance().warn("Task finished with an exception", e);
            // ensure that we run from a dispatch thread
            ApplicationManager.getApplication().invokeLater(this::onCancel);
        }
    }

    @Override
    public void onSuccess() {
        delegate.onSuccess();
        onFinish();
    }

    @Override
    public void onCancel() {
        delegate.onCancel();
        onFinish();
    }

    protected void onFinish() {
    }

    protected synchronized void setProgressIndicator(final ProgressIndicator indicator) throws ProcessCanceledException {
        if (alreadyCancelled) {
            throw new ProcessCanceledException();
        }
        progressIndicator = indicator;
    }

    public synchronized void cancel() {
        alreadyCancelled = true;
        if (progressIndicator != null) {
            progressIndicator.cancel();
        }
    }
}
