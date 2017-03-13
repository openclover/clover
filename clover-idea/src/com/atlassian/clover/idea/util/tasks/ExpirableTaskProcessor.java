package com.atlassian.clover.idea.util.tasks;

import clover.org.apache.commons.collections.list.SetUniqueList;
import com.atlassian.clover.Logger;
import com.atlassian.clover.idea.util.MiscUtils;
import clover.com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;

import static clover.com.google.common.collect.Lists.newLinkedList;

public class ExpirableTaskProcessor {
    @SuppressWarnings({"unchecked"})
    private final List<ExpirableTaskDelegate> taskDelegateQueue = SetUniqueList.decorate(newLinkedList());
    private Task workerTask;
    private Boolean shouldBeHeadless;

    public ExpirableTaskProcessor() {
    }

    @VisibleForTesting
    ExpirableTaskProcessor(Boolean shouldBeHeadless) {
        this.shouldBeHeadless = shouldBeHeadless;
    }

    public static ExpirableTaskProcessor getInstance() {
        return ServiceManager.getService(ExpirableTaskProcessor.class);
    }

    public void queue(ExpirableTaskDelegate taskDelegate) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        synchronized (taskDelegateQueue) {
            taskDelegateQueue.add(0, taskDelegate);
        }
        schedule();
    }

    private void schedule() {
        if (workerTask == null && !taskDelegateQueue.isEmpty()) {
            workerTask = new WorkerTask();
            workerTask.queue();
        }
    }

    private class WorkerTask extends CancellableTask {
        public WorkerTask() {
            super(null, new WorkerTaskDelegate());
        }

        @Override
        public boolean isHeadless() {
            return shouldBeHeadless == null ? super.isHeadless() : shouldBeHeadless;
        }
    }

    private class WorkerTaskDelegate extends AbstractCancellableTaskDelegate {
        public WorkerTaskDelegate() {
            super("Clover UI update");
        }

        @Nullable
        private ExpirableTaskDelegate fetchNextTask() {
            synchronized (taskDelegateQueue) {
                final Iterator<ExpirableTaskDelegate> taskIterator = taskDelegateQueue.iterator();
                while (taskIterator.hasNext()) {
                    final ExpirableTaskDelegate nextTaskDelegate = taskIterator.next();
                    taskIterator.remove();
                    if (nextTaskDelegate.shouldProceed()) {
                        return nextTaskDelegate;
                    }
                }
            }
            return null;
        }

        @Override
        public void run(@NotNull final ProgressIndicator progressIndicator) {

            for(ExpirableTaskDelegate td = fetchNextTask(); td != null; td = fetchNextTask()) {
                final ExpirableTaskDelegate taskDelegate = td;

                progressIndicator.checkCanceled();
                progressIndicator.setText("Clover calculating test contribution coverage, about "
                        + taskDelegateQueue.size() + " more items left.");

                try {
                    taskDelegate.run(progressIndicator);
                } catch (Exception e) {
                    if (!(e instanceof ProcessCanceledException)) {
                        Logger.getInstance().warn("Clover UI update task threw an exception", e);
                    }
                    // run task cancellation in the UI thread
                    MiscUtils.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            taskDelegate.onCancel();
                        }
                    });
                    continue;
                }

                progressIndicator.checkCanceled();
                // run task success in the UI thread
                MiscUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        taskDelegate.onSuccess();
                    }
                });
            }

            // no more tasks in the queue, release the worker
            workerTask = null;
        }

        @Override
        public void onSuccess() {
        }

        @Override
        public void onCancel() {
            // abandon all pending tasks and release the worker
            synchronized (taskDelegateQueue) {
                taskDelegateQueue.clear();
                workerTask = null;
            }
        }
    }
}
