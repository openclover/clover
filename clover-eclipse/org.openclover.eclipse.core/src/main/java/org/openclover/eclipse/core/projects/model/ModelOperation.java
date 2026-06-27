package org.openclover.eclipse.core.projects.model;

public interface ModelOperation<T> {
    T run(DatabaseModel model);
}
