package org.openclover.idea.config;

import com.intellij.openapi.project.Project;
import org.openclover.core.CloverDatabase;
import org.openclover.core.context.ContextStore;
import org.openclover.idea.ProjectPlugin;

import javax.swing.JPanel;
import java.awt.GridBagLayout;

public class ContextFilterPane extends ConfigPanel {
    private final ContextPane reservedContexts;
    private final IdeaRegexpConfigPanel regexpPanel;

    public ContextFilterPane(Project project) {
        final CloverDatabase cloverDatabase = ProjectPlugin.getPlugin(project).getCoverageManager().getCoverage();
        ContextStore registry = cloverDatabase != null ? cloverDatabase.getContextStore() : null;
        if (registry == null) {
            registry = new ContextStore();
        }
        setName("Contexts");

        setLayout(new GridBagLayout());

        reservedContexts = new ContextPane(registry);
        add(reservedContexts, new GBC(1, 1).setFill(GBC.HORIZONTAL).setWeight(0.01, 0));

        regexpPanel = new IdeaRegexpConfigPanel(project, registry);
        add(regexpPanel, new GBC(1, 2).setFill(GBC.HORIZONTAL).setWeight(0.01, 1));

        add(new JPanel(), new GBC(1, 3).setFill(GBC.BOTH).setWeight(0.0, 1));
    }

    @Override
    public void commitTo(CloverPluginConfig data) {
        reservedContexts.commitTo(data);
        regexpPanel.commitTo(data);
    }

    @Override
    public void loadFrom(CloverPluginConfig data) {
        reservedContexts.loadFrom(data);
        regexpPanel.loadFrom(data);
    }

    @Override
    public String getTitle() {
        return "Context Filters";

    }

    @Override
    public void enableConfig(boolean b) {
        reservedContexts.enableConfig(b);
        regexpPanel.enableConfig(b);
    }
}
