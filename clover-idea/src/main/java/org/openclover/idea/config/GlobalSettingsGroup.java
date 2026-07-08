package org.openclover.idea.config;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.components.panels.VerticalBox;
import org.openclover.idea.autoupdater.AutoUpdateComponent;
import org.openclover.idea.junit.config.TestOptimizationGlobalSettings;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.GridBagLayout;

/**
 * Single "OpenClover (global settings)" page. It composes the Auto Update and Test Optimization
 * settings onto one page (previously two almost empty child pages) and forwards the settings
 * lifecycle (isModified/apply/reset) to each of them. Each delegate keeps its own persistence
 * backend, so no configuration is merged - the page only aggregates the UI and lifecycle calls.
 */
public class GlobalSettingsGroup implements Configurable {

    private final Configurable autoUpdate = AutoUpdateComponent.getInstance().getConfigurable();
    private final Configurable testOptimization = TestOptimizationGlobalSettings.getInstance();

    private JComponent rootComponent;

    @Override
    public String getDisplayName() {
        return "OpenClover (global settings)";
    }

    @Override
    public String getHelpTopic() {
        return null;
    }

    @Override
    public JComponent createComponent() {
        // stack both delegate panels vertically, bottom glue pushes both sections to the top of the page
        final VerticalBox box = new VerticalBox();
        box.add(autoUpdate.createComponent());
        box.add(testOptimization.createComponent());

        rootComponent = new JPanel(new GridBagLayout());
        rootComponent.add(box, new GBC(0, 0).setWeight(1, 0).setFill(GBC.HORIZONTAL));
        rootComponent.add(new JPanel(), new GBC(0, 1).setWeight(1, 1).setFill(GBC.BOTH));
        return rootComponent;
    }

    @Override
    public boolean isModified() {
        return autoUpdate.isModified() || testOptimization.isModified();
    }

    @Override
    public void apply() throws ConfigurationException {
        autoUpdate.apply();
        testOptimization.apply();
    }

    @Override
    public void reset() {
        autoUpdate.reset();
        testOptimization.reset();
    }

    @Override
    public void disposeUIResources() {
        autoUpdate.disposeUIResources();
        testOptimization.disposeUIResources();
        rootComponent = null;
    }
}
