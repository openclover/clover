package org.openclover.eclipse.core.ui.projects;

import org.openclover.eclipse.core.projects.CloverProject;
import org.openclover.eclipse.core.projects.model.DatabaseModel;
import org.openclover.eclipse.core.projects.model.StableDatabaseModel;
import org.openclover.eclipse.core.projects.model.LoadingDatabaseModel;
import org.openclover.eclipse.core.projects.model.LoadedDatabaseModel;

public class DatabaseChangeEvent {
    private final CloverProject project;
    private boolean forWorkspace;
    private boolean stable;
    private boolean substantive;

    public DatabaseChangeEvent(CloverProject project, DatabaseModel oldModel, DatabaseModel newModel) {
        this.project = project;
        this.forWorkspace = project == null;
        this.stable = newModel instanceof StableDatabaseModel;
        this.substantive =
            !(newModel instanceof LoadingDatabaseModel && oldModel instanceof LoadedDatabaseModel);
    }

    public CloverProject getProject() {
        return project;
    }

    public boolean isApplicableTo(CloverProject cloverProject) {
        return forWorkspace || cloverProject == project;
    }

    public boolean isStable() {
        return stable;
    }

    public boolean isForWorkspace() {
        return forWorkspace;
    }

    public boolean isSubstantiveProjectChange() {
        return substantive;
    }
}
