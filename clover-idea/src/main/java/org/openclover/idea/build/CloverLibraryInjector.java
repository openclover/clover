package org.openclover.idea.build;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.Library;
import org.openclover.idea.LibrarySupport;
import org.openclover.idea.ProjectPlugin;
import org.openclover.idea.config.ConfigChangeEvent;
import org.openclover.idea.config.ConfigChangeListener;
import org.openclover.idea.config.IdeaCloverConfig;
import org.openclover.idea.util.MiscUtils;

/**
 * Adds or removes the "Clover IDEA Library" from project's modules depending on whether Clover is enabled or not.
 */
public class CloverLibraryInjector implements ConfigChangeListener {

    private final Project project;

    public CloverLibraryInjector(final Project project) {
        this.project = project;
    }

    @Override
    public void configChange(final ConfigChangeEvent evt) {
        if (evt.hasPropertyChange(IdeaCloverConfig.BUILD_WITH_CLOVER)
                || evt.hasPropertyChange(IdeaCloverConfig.ENABLED)) {
            updateModulesDependencies();
        }
    }

    public void updateModulesDependencies() {
        if (isCloverInstrumentationEnabled()) {
            addCloverLibrary(ModuleManager.getInstance(project).getModules());
        } else {
            removeCloverLibrary(ModuleManager.getInstance(project).getModules());
        }
    }

    private void addCloverLibrary(final Module[] modules) {
        final Runnable addLibrary = () -> {
            final Library cloverLibrary = LibrarySupport.getValidatedCloverLibrary();
            boolean isModified = false;
            for (Module module : modules) {
                isModified |= LibrarySupport.addLibraryTo(cloverLibrary, module);
            }
            // force saving project changes into disk (for external build)
            if (isModified) {
                project.save();
            }
        };

        // task should be executed from an event dispatch thread
        // model changes shall be executed inside write action
        if (!ApplicationManager.getApplication().isDispatchThread()) {
            MiscUtils.invokeWriteActionAndWait(addLibrary);
        } else {
            ApplicationManager.getApplication().runWriteAction(addLibrary);
        }
    }

    private void removeCloverLibrary(final Module[] modules) {
        final Runnable removeLibrary = () -> {
            boolean isModified = false;
            for (Module module : modules) {
                isModified |= LibrarySupport.removeCloverLibraryFrom(module);
            }
            // force saving project changes into disk (for external build)
            if (isModified) {
                project.save();
            }
        };

        // task should be executed from an event dispatch thread
        // model changes shall be executed inside write action
        if (!ApplicationManager.getApplication().isDispatchThread()) {
            MiscUtils.invokeWriteActionAndWait(removeLibrary);
        } else {
            ApplicationManager.getApplication().runWriteAction(removeLibrary);
        }
    }

    private boolean isCloverInstrumentationEnabled() {
        return ProjectPlugin.getPlugin(project).getConfig().isEnabled()
                && ProjectPlugin.getPlugin(project).getConfig().isBuildWithClover();
    }
}
