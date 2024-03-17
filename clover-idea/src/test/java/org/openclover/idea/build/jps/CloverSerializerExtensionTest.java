package org.openclover.idea.build.jps;

import org.jdom.Element;
import org.jetbrains.jps.devkit.model.JpsPluginModuleProperties;
import org.jetbrains.jps.devkit.model.JpsPluginModuleType;
import org.jetbrains.jps.model.JpsElementFactory;
import org.jetbrains.jps.model.JpsSimpleElement;
import org.jetbrains.jps.model.java.JpsJavaModuleType;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.serialization.JpsGlobalExtensionSerializer;
import org.jetbrains.jps.model.serialization.JpsProjectExtensionSerializer;
import org.jetbrains.jps.model.serialization.module.JpsModulePropertiesSerializer;
import org.junit.Test;
import org.openclover.idea.config.CloverModuleConfig;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test for {@link CloverSerializerExtension}
 */
public class CloverSerializerExtensionTest {

    /**
     * @see org.openclover.idea.build.jps.CloverSerializerExtension#getGlobalExtensionSerializers()
     */
    @Test
    public void testGetGlobalExtensionSerializers() {
        final List<? extends JpsGlobalExtensionSerializer> serializers =
                new CloverSerializerExtension().getGlobalExtensionSerializers();
        assertEquals(1, serializers.size());
        assertTrue(serializers.get(0) instanceof CloverJpsGlobalConfigurationSerializer);
    }

    /**
     * @see org.openclover.idea.build.jps.CloverSerializerExtension#getModulePropertiesSerializers()
     */
    @Test
    public void testGetModulePropertiesSerializers() {
        final List<? extends JpsModulePropertiesSerializer<?>> serializers =
                new CloverSerializerExtension().getModulePropertiesSerializers();
        assertEquals(0, serializers.size());
    }

    /**
     * @see org.openclover.idea.build.jps.CloverSerializerExtension#getProjectExtensionSerializers()
     */
    @Test
    public void testGetProjectExtensionSerializers() {
        final List<? extends JpsProjectExtensionSerializer> serializers =
                new CloverSerializerExtension().getProjectExtensionSerializers();
        assertEquals(1, serializers.size());
        assertTrue(serializers.get(0) instanceof CloverJpsProjectConfigurationSerializer);
    }

    /**
     * @see CloverSerializerExtension#loadModuleOptions(org.jetbrains.jps.model.module.JpsModule, org.jdom.Element)
     */
    @Test
    public void testLoadModuleOptions_JavaModule() {
        // with true
        JpsSimpleElement<CloverModuleConfig> simpleElement = loadJavaModuleAndGetCloverConfig(true, false);
        assertNotNull(simpleElement.getData());
        assertTrue(simpleElement.getData().isExcluded());

        // with false
        simpleElement = loadJavaModuleAndGetCloverConfig(false, false);
        assertNotNull(simpleElement.getData());
        assertFalse(simpleElement.getData().isExcluded());

        // with null
        simpleElement = loadJavaModuleAndGetCloverConfig(false, true);
        assertNotNull(simpleElement.getData());
        assertFalse(simpleElement.getData().isExcluded());
    }

    /**
     * @see CloverSerializerExtension#loadModuleOptions(org.jetbrains.jps.model.module.JpsModule, org.jdom.Element)
     */
    @Test
    public void testLoadModuleOptions_PluginModule() {
        // with true
        JpsSimpleElement<CloverModuleConfig> simpleElement = loadPluginModuleAndGetCloverConfig(true, false);
        assertNotNull(simpleElement.getData());
        assertTrue(simpleElement.getData().isExcluded());

        // with false
        simpleElement = loadPluginModuleAndGetCloverConfig(false, false);
        assertNotNull(simpleElement.getData());
        assertFalse(simpleElement.getData().isExcluded());

        // with null
        simpleElement = loadPluginModuleAndGetCloverConfig(false, true);
        assertNotNull(simpleElement.getData());
        assertFalse(simpleElement.getData().isExcluded());
    }


    protected JpsModule createJavaModuleStub() {
        return JpsElementFactory.getInstance().createModule("JavaModule", JpsJavaModuleType.INSTANCE,
                JpsElementFactory.getInstance().createDummyElement());
    }

    protected JpsModule createPluginModuleStub() {
        return JpsElementFactory.getInstance().createModule("PluginModule", JpsPluginModuleType.INSTANCE,
                JpsElementFactory.getInstance().createSimpleElement(
                        new JpsPluginModuleProperties("file://plugin.xml", "file://MANIFEST.MF")));
    }

    protected JpsSimpleElement<CloverModuleConfig> loadJavaModuleAndGetCloverConfig(boolean isExcluded, boolean isCloverMissing) {
        final JpsModule javaModule = createJavaModuleStub();
        new CloverSerializerExtension().loadModuleOptions(javaModule, createSampleData("JAVA_MODULE", isExcluded, isCloverMissing));
        return javaModule.getContainer().getChild(CloverSerializerExtension.CloverModuleConfigurationRole.INSTANCE);
    }

    protected JpsSimpleElement<CloverModuleConfig> loadPluginModuleAndGetCloverConfig(boolean isExcluded, boolean isCloverMissing) {
        final JpsModule pluginModule = createPluginModuleStub();
        new CloverSerializerExtension().loadModuleOptions(pluginModule, createSampleData("PLUGIN_MODULE", isExcluded, isCloverMissing));
        return pluginModule.getContainer().getChild(CloverSerializerExtension.CloverModuleConfigurationRole.INSTANCE);
    }

    /**
     * Returns sample data:
     *
     * <pre>
     * &lt;module type="moduleType"&gt;
     *    &lt;component name="Clover"&gt;
     *       &lt;option name="excluded" value="true" /&gt;
     *    &lt;/component&gt;
     * &lt;module&gt;
     * </pre>
     *
     * @return Element
     */
    protected Element createSampleData(String moduleType, boolean isExcluded, boolean isCloverMissing) {
        final Element module = new Element("module").setAttribute("type", moduleType);
        if (!isCloverMissing) {
            final Element component = new Element("component").setAttribute("name", "OpenClover");
            component.addContent(new Element("option").setAttribute("name", "excluded")
                    .setAttribute("value", isExcluded ? "true" : "false"));
            module.addContent(component);
        }
        return module;
    }

}
