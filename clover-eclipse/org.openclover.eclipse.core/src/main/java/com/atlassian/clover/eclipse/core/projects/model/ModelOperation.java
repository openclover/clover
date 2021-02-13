package com.atlassian.clover.eclipse.core.projects.model;

public interface ModelOperation<T> {
    public T run(DatabaseModel model);
}
