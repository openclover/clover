package com.atlassian.clover.idea.build.jps;

import clover.com.google.common.collect.Lists;
import com.atlassian.clover.Logger;
import com.atlassian.clover.idea.config.IdeaXmlConfigConstants;
import com.atlassian.clover.idea.config.CloverModuleConfig;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.devkit.model.JpsPluginModuleType;
import org.jetbrains.jps.model.JpsElementChildRole;
import org.jetbrains.jps.model.JpsElementFactory;
import org.jetbrains.jps.model.JpsSimpleElement;
import org.jetbrains.jps.model.java.JpsJavaModuleType;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.serialization.JDomSerializationUtil;
import org.jetbrains.jps.model.serialization.JpsGlobalExtensionSerializer;
import org.jetbrains.jps.model.serialization.JpsModelSerializerExtension;
import org.jetbrains.jps.model.serialization.JpsProjectExtensionSerializer;
import org.jetbrains.jps.model.serialization.module.JpsModulePropertiesSerializer;

import java.util.Collections;
import java.util.List;

/**
 * Serializer extension service which returns three serializers:
 * <ul>
 *  <li>for global settings (per IDEA installation) - e.g. license key</li>
 *  <li>for project/workspace settings (per IDEA project) - e.g. initstring</li>
 *  <li>for module settings (per module) - e.g. exclusion from instrumentation</li>
 * </ul>
 */
@SuppressWarnings("Unused") // see META-INF\services
public class CloverSerializerExtension extends JpsModelSerializerExtension {

    private static final Logger LOG = Logger.getInstance();

    /**
     * Marker class used as a key in module.getContainer().getChild()
     */
    public static class CloverModuleConfigurationRole extends JpsElementChildRole<JpsSimpleElement<CloverModuleConfig>> {
        public static final CloverModuleConfigurationRole INSTANCE = new CloverModuleConfigurationRole();
        private CloverModuleConfigurationRole() { }
    }


    @NotNull
    @Override
    public List<? extends JpsGlobalExtensionSerializer> getGlobalExtensionSerializers() {
        return Lists.newArrayList(new CloverJpsGlobalConfigurationSerializer());
    }

    @NotNull
    @Override
    public List<? extends JpsModulePropertiesSerializer<?>> getModulePropertiesSerializers() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public List<? extends JpsProjectExtensionSerializer> getProjectExtensionSerializers() {
        return Lists.newArrayList(new CloverJpsProjectConfigurationSerializer());
    }

    @Override
    public void loadModuleOptions(@NotNull JpsModule module, @NotNull Element rootElement) {
        if (module.getModuleType() instanceof JpsJavaModuleType ||
                module.getModuleType() instanceof JpsPluginModuleType) {
            // search for <component name="Clover"> tag in *.iml
            Element componentElement = JDomSerializationUtil.findComponent(rootElement, IdeaXmlConfigConstants.MODULE_FILE_COMPONENT_NAME);

            // read value
            boolean isExcluded = Boolean.parseBoolean(
                    componentElement != null
                            ? JDOMExternalizerUtil.readField(componentElement, "excluded", "false")
                            : "false");

            // wrap into JpsSimpleElement
            JpsSimpleElement<CloverModuleConfig> simpleElement = JpsElementFactory.getInstance().createSimpleElement(
                    new CloverModuleConfig(isExcluded));

            // attach to module metadata
            module.getContainer().setChild(CloverModuleConfigurationRole.INSTANCE, simpleElement);
            LOG.debug("Clover: attaching module settings (isExcluded=" + isExcluded + ") to module " + module.getName());
        } else {
            LOG.debug("Clover: unsupported module type: " + module.getModuleType() + ", skipping.");
        }
    }
}
