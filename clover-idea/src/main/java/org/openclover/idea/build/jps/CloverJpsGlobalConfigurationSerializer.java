package org.openclover.idea.build.jps;

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.JpsElement;
import org.jetbrains.jps.model.JpsElementChildRole;
import org.jetbrains.jps.model.JpsElementFactory;
import org.jetbrains.jps.model.JpsGlobal;
import org.jetbrains.jps.model.JpsSimpleElement;
import org.jetbrains.jps.model.serialization.JpsGlobalExtensionSerializer;
import org.openclover.idea.config.CloverGlobalConfig;
import org.openclover.idea.config.IdeaXmlConfigConstants;

/**
 * Serialization of settings from global configuration files (i.e. stored per IDEA installation).
 * See "@State" annotation in org.openclover.idea.CloverPlugin
 */
public class CloverJpsGlobalConfigurationSerializer extends JpsGlobalExtensionSerializer {

    /**
     * Helper class used as a key for data stored in project context
     * @see org.jetbrains.jps.model.JpsProject#getContainer()
     */
    public static class CloverGlobalSettingsRole extends JpsElementChildRole<JpsElement> {
        public static final CloverGlobalSettingsRole INSTANCE = new CloverGlobalSettingsRole();
    }

    /**
     * Create serializer which will parse {@link IdeaXmlConfigConstants#OTHER_XML_FILE_COMPONENT_NAME}
     * tag from other.xml file
     */
    public CloverJpsGlobalConfigurationSerializer() {
        super("other.xml", IdeaXmlConfigConstants.OTHER_XML_FILE_COMPONENT_NAME);
    }

    /**
     * Parse content like:
     * <pre>
     * &lt;component name="Clover"&gt;
     * ...
     * &lt;/component&gt;
     * </pre>
     *
     * @param jpsGlobal
     * @param componentTag
     */
    @Override
    public void loadExtension(@NotNull JpsGlobal jpsGlobal, @NotNull Element componentTag) {
        // the CloverPlugin implements PersistentStateComponent<Element>
        CloverGlobalConfig data = new CloverGlobalConfig();
        JpsSimpleElement<CloverGlobalConfig> wrappedData = JpsElementFactory.getInstance().createSimpleElement(data);
        jpsGlobal.getContainer().setChild(CloverGlobalSettingsRole.INSTANCE, wrappedData);
    }

    @Override
    public void saveExtension(@NotNull JpsGlobal jpsGlobal, @NotNull Element componentTag) {
        // not needed
    }
}
