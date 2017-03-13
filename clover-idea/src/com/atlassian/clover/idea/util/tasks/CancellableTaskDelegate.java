package com.atlassian.clover.idea.util.tasks;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Progressive;
import org.jetbrains.annotations.NotNull;

public interface CancellableTaskDelegate extends Progressive {
    String getTitle();

    @Override
    void run(@NotNull ProgressIndicator progressIndicator);

    void onSuccess();

    void onCancel();
}
