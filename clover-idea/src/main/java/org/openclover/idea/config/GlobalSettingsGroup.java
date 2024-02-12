package org.openclover.idea.config;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableGroup;
import com.intellij.openapi.options.UnnamedConfigurableGroup;
import org.openclover.idea.autoupdater.AutoUpdateComponent;
import org.openclover.idea.junit.config.TestOptimizationGlobalSettings;

public class GlobalSettingsGroup extends UnnamedConfigurableGroup implements ConfigurableGroup, Configurable {

    @Override
    public String getDisplayName() {
        return getShortName(); 
    }

    @Override
    public String getHelpTopic() {
        return null;
    }

    // used on IDEA8.0+
    @Override
    public Configurable[] getConfigurables() {
        return new Configurable[] {
                TestOptimizationGlobalSettings.getInstance(),
                AutoUpdateComponent.getInstance().getConfigurable()

        };
    }

    @Override
    public String getShortName() {
        //TODO depracated since 15
        return "Clover (global settings)";
    }
}
