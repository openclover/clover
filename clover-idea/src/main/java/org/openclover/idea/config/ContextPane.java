package org.openclover.idea.config;

import org.openclover.core.context.ContextSet;
import org.openclover.core.context.ContextStore;
import org.openclover.core.context.NamedContext;
import org.openclover.idea.util.l10n.CloverIdeaPluginMessages;
import org.openclover.idea.util.ui.RichLabel;
import org.openclover.idea.util.ui.UIUtils;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;
import java.util.Map;

import static org.openclover.core.util.Maps.newHashMap;

/**
 * Simple JPanel that displays a checkbox for each context.
 */
public class ContextPane extends ConfigPanel {
    public static final String PANEL_TITLE = "Built-in Contexts";

    private JPanel checkboxPane = new JPanel();
    private Map<String, JCheckBox> checkboxes = newHashMap();
    private boolean enabled = true;

    private ContextSet filter;
    private ContextStore registry;

    public ContextPane(ContextStore registry, boolean enabled, boolean showDesc) {
        this.enabled = enabled;
        this.registry = registry;

        setupCheckboxes(registry);
        final BorderLayout borderLayout = new BorderLayout();
        borderLayout.setVgap(5);
        setLayout(borderLayout);
        if (showDesc) {
            setBorder(UIUtils.createBorder(getTitle()));

            JLabel instructions = new RichLabel("<html>" + CloverIdeaPluginMessages.BLOCK_CONTEXT_FILTER_INSTRUCTIONS());
            add(instructions, BorderLayout.NORTH);
        }
        add(checkboxPane, BorderLayout.CENTER);
    }

    public ContextPane(ContextStore registry) {
        this(registry, true, true);
    }

    private void setContextFilter(ContextStore registry, ContextSet filter) {
        this.filter = filter;
        this.registry = registry;

        for (Map.Entry<String, JCheckBox> cbEntry : checkboxes.entrySet()) {
            final NamedContext context = registry.getContext(cbEntry.getKey());
            final JCheckBox checkbox = cbEntry.getValue();

            checkbox.setSelected(filter.get(context.getIndex()));
        }
    }


    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Read states of checkboxes and save them as current filter setting (using contexts defined in registry).
     * @return ContextSet
     */
    private ContextSet updateFilterState() {
        ContextSet newFilter = new ContextSet(filter.size());

        for (Map.Entry<String, JCheckBox> cbEntry : checkboxes.entrySet()) {
            final boolean enabled = cbEntry.getValue().isSelected();
            int idx = registry.getContext(cbEntry.getKey()).getIndex();
            newFilter = newFilter.set(idx, enabled); // ContextFilter.set() returns a clone
        }
        filter = newFilter;
        return filter;
    }

    @SuppressWarnings("unchecked")
    private void setupCheckboxes(ContextStore contextRegistry) {
        List<NamedContext> reservedContexts = contextRegistry.getReservedContexts();
        reservedContexts.addAll(contextRegistry.getReservedMethodContexts());

        GridLayout layout = new GridLayout();
        layout.setRows((reservedContexts.size() / 3) + 1);
        checkboxPane.setLayout(layout);
        for (NamedContext namedContext : reservedContexts) {
            if (namedContext.getIndex() == ContextStore.CONTEXT_CLOVER_OFF) {
                continue;
            }
            final String contextName = namedContext.getName();
            String displayedName;
            if (namedContext.getType() == NamedContext.Type.BLOCK) {
                displayedName = CloverIdeaPluginMessages.RESERVED_BLOCK_CONTEXT(contextName);
            } else {
                displayedName = CloverIdeaPluginMessages.RESERVED_REGEX_CONTEXT(contextName);
            }
            JCheckBox checkBox = new JCheckBox(displayedName, false);
            checkBox.setEnabled(enabled);
            checkboxes.put(contextName, checkBox);
            checkboxPane.add(checkBox);
        }
    }

    public void setContextFilterSpec(String spec) {
        final ContextSet filter = registry.createContextSetFilter(spec);
        setContextFilter(registry, filter);
    }

    public String getContextFilterSpec() {
        return registry.getContextsAsString(updateFilterState());
    }

    @Override
    public void commitTo(CloverPluginConfig data) {
        final String spec = getContextFilterSpec();
        data.setContextFilterSpec(spec);
    }

    @Override
    public void loadFrom(CloverPluginConfig data) {
        final String spec = data.getContextFilterSpec();
        setContextFilterSpec(spec);
    }

    @Override
    public String getTitle() {
        return "Built-in context filters";
    }

    @Override
    public void enableConfig(boolean b) {
        super.enableConfig(b);

        this.enabled = b;

        for (JCheckBox checkBox : checkboxes.values()) {
            checkBox.setEnabled(enabled);
        }

        setEnabled(b);
    }

}
