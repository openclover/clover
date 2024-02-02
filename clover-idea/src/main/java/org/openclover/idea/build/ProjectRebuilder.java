package org.openclover.idea.build;

import com.atlassian.clover.CloverDatabase;
import com.atlassian.clover.context.ContextStore;
import com.atlassian.clover.context.NamedContext;
import org.openclover.idea.ProjectPlugin;
import org.openclover.idea.config.ConfigChangeEvent;
import org.openclover.idea.config.ConfigChangeListener;
import org.openclover.idea.config.IdeaCloverConfig;
import org.openclover.idea.config.ProjectRebuild;
import org.openclover.idea.config.regexp.Regexp;
import org.openclover.idea.feature.FeatureEvent;
import org.openclover.idea.feature.FeatureListener;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;

import java.beans.PropertyChangeEvent;
import java.util.List;

public class ProjectRebuilder implements FeatureListener, ConfigChangeListener {
    private static final Key<ProjectRebuilder> PROJECT_REBUILDER_KEY = Key.create(ProjectRebuilder.class.getName());
    private final Project project;

    private static final String[] REBUILD_TRIGGERS = {
            IdeaCloverConfig.FLUSH_INTERVAL,
            IdeaCloverConfig.FLUSH_POLICY,
            IdeaCloverConfig.RELATIVE_INIT_STRING,
            IdeaCloverConfig.AUTO_INIT_STRING,
            IdeaCloverConfig.MANUAL_INIT_STRING
            //IdeaCloverConfig.BUILD_WITH_CLOVER handled via featureStateChanged() with DB cleanup by rebuildProject(true)
    };

    public ProjectRebuilder(final Project project) {
        this.project = project;
    }

    /**
     * Register ProjectRebuilder for any feature which should trigger a rebuild when changed.
     *
     * @param evt unused - make sure ProjectRebuilder is registered for appropriate features only.
     */
    @Override
    public void featureStateChanged(final FeatureEvent evt) {
        rebuildProject(true);
    }

    /**
     * Register ProjectRebuilder as config change listener.<p>
     * configChange method triggers rebuild after hardcoded change events.
     *
     * @param evt change event
     */
    @Override
    public void configChange(ConfigChangeEvent evt) {
        for (String rebuildTrigger : REBUILD_TRIGGERS) {
            if (evt.hasPropertyChange(rebuildTrigger)) {
                rebuildProject();
                return;
            }
        }
        if (evt.hasPropertyChange(IdeaCloverConfig.REGEXP_CONTEXTS)) {
            // rebuild if custom context have been changed or added (but not removed - no point)
            final CloverDatabase coverage = ProjectPlugin.getPlugin(project).getCoverageManager().getCoverage();
            if (coverage != null) {
                final ContextStore contextStore = coverage.getContextStore();
                final PropertyChangeEvent pc = evt.getPropertyChange(IdeaCloverConfig.REGEXP_CONTEXTS);
                @SuppressWarnings("unchecked")
                final List<Regexp> configuredRegexps = (List<Regexp>) pc.getNewValue();

                for (Regexp configuredRegexp : configuredRegexps) {
                    NamedContext c = contextStore.getContext(configuredRegexp.getName());
                    if (configuredRegexp.isDifferent(c)) {
                        // isDifferent returns true for c == null
                        rebuildProject();
                        return;
                    }
                }
            }
        }
    }

    public static ProjectRebuilder getInstance(Project prj) {
        ProjectRebuilder instance = prj.getUserData(PROJECT_REBUILDER_KEY);
        if (instance == null) {
            instance = new ProjectRebuilder(prj);
            prj.putUserData(PROJECT_REBUILDER_KEY, instance);
        }

        return instance;
    }

    public void rebuildProject() {
        rebuildProject(false);
    }

    public void rebuildProject(boolean cleanDb) {
        final IdeaCloverConfig ideaCloverConfig = ProjectPlugin.getPlugin(project).getConfig();
        final ProjectRebuild current = ideaCloverConfig.getProjectRebuild();

        final boolean rebuild;

        if (current == ProjectRebuild.ASK) {
            RebuildProjectDialog rebuildProjectDialog = new RebuildProjectDialog(project, cleanDb);
            rebuildProjectDialog.show();

            rebuild = rebuildProjectDialog.rebuildRequested();
            if (!rebuildProjectDialog.askNextTime()) {
                ideaCloverConfig.setProjectRebuild(rebuild ? ProjectRebuild.ALWAYS : ProjectRebuild.NEVER);
            }
        } else {
            rebuild = current == ProjectRebuild.ALWAYS;
        }

        if (rebuild) {
            if (cleanDb) {
                ProjectPlugin.getPlugin(project).getCoverageManager().delete();
            }
            CompilerManager.getInstance(project).rebuild(null);
        }

    }
}
