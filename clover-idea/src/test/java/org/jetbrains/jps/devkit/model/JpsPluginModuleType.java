package org.jetbrains.jps.devkit.model;

import org.jetbrains.jps.model.JpsDummyElement;
import org.jetbrains.jps.model.ex.JpsElementTypeWithDummyProperties;
import org.jetbrains.jps.model.module.JpsModuleType;

/**
 * Test-only stand-in for IntelliJ DevKit's JPS class of the same fully-qualified name.
 * DevKit is no longer bundled in any IntelliJ IDEA distribution the Gradle build targets
 * (2024–2026), so the real {@code org.jetbrains.jps.devkit.model.JpsPluginModuleType} is not
 * available on the test classpath.
 * <p>
 * See https://plugins.jetbrains.com/plugin/22851-plugin-devkit
 */
public final class JpsPluginModuleType extends JpsElementTypeWithDummyProperties
        implements JpsModuleType<JpsDummyElement> {

    public static final JpsPluginModuleType INSTANCE = new JpsPluginModuleType();

    private JpsPluginModuleType() {
    }
}
