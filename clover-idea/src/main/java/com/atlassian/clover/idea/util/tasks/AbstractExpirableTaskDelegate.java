package com.atlassian.clover.idea.util.tasks;

/**
 *
 */
public abstract class AbstractExpirableTaskDelegate extends AbstractCancellableTaskDelegate implements ExpirableTaskDelegate {
    public AbstractExpirableTaskDelegate(String title) {
        super(title);
    }
}
