package org.openclover.idea.util.tasks;

public interface ExpirableTaskDelegate extends CancellableTaskDelegate {
    boolean shouldProceed();

}
