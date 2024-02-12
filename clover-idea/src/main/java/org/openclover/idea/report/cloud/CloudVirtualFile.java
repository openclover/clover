package org.openclover.idea.report.cloud;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.idea.HasMetricsListener;
import org.openclover.idea.ProjectPlugin;
import org.openclover.idea.coverage.CoverageManager;
import org.openclover.idea.util.vfs.DummyVirtualFile;

import java.util.Collection;

import static org.openclover.core.util.Lists.newArrayList;

public class CloudVirtualFile extends DummyVirtualFile {
    private static final Key<CloudVirtualFile> FILE_KEY = Key.create(CloudVirtualFile.class.getName());

    private final CoverageManager coverageManager;
    private HasMetrics currentlySelectedElement;

    private CloudVirtualFile(Project project) {
        super("Cloud Report");
        coverageManager = ProjectPlugin.getPlugin(project).getCoverageManager();
    }

    public CoverageManager getCoverageManager() {
        return coverageManager;
    }

    /**
     * Return an already open instance of CloudVirtualFile that is associated with the project or create new one.
     *
     * @param project current project
     * @return existing or new instance of CloudVirtualFile
     */
    public static CloudVirtualFile getInstance(Project project) {
        CloudVirtualFile vf = project.getUserData(FILE_KEY);
        if (vf == null) {
            vf = new CloudVirtualFile(project);
            project.putUserData(FILE_KEY, vf);
        }

        return vf;
    }

    private final Collection<HasMetricsListener> listeners = newArrayList();

    public void addHasMetricsListener(HasMetricsListener listener) {
        listeners.add(listener);
    }

    public void removeHasMetricsListener(HasMetricsListener listener) {
        listeners.remove(listener);
    }

    private void notify(HasMetrics metrics) {
        for (HasMetricsListener listener : listeners) {
            listener.valueChanged(metrics);
        }
    }

    public synchronized void setSelectedElement(HasMetrics element) {
        currentlySelectedElement = element;
        notify(element);
    }

    public synchronized HasMetrics getSelectedElement() {
        return currentlySelectedElement;
    }
}