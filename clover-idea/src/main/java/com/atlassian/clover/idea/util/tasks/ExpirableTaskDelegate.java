package com.atlassian.clover.idea.util.tasks;

public interface ExpirableTaskDelegate extends CancellableTaskDelegate {
    boolean shouldProceed();

}
