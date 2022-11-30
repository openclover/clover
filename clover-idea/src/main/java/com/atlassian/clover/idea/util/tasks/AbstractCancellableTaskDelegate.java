package com.atlassian.clover.idea.util.tasks;

/**
 * A task delegate which can be cancelled and has it's own title. This abstract class provides
 * empty implementation of onCancel method.
 */
public abstract class AbstractCancellableTaskDelegate implements CancellableTaskDelegate {
    private final String title;

    /**
     * Construct task with a title.
     * @param title task title
     */
    public AbstractCancellableTaskDelegate(String title) {
        this.title = title;
    }

    /**
     * Returns task title.
     * @return String
     */
    @Override
    public String getTitle() {
        return title;
    }

    /**
     * The onCancel which makes nothing.
     */
    @Override
    public void onCancel() {
    }
}
