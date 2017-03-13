package com.atlassian.clover.idea.build.jps;

import com.atlassian.clover.idea.config.IdeaXmlConfigConstants;
import com.atlassian.clover.idea.util.jdom.JDOMExternUtil;
import com.atlassian.clover.idea.config.CloverPluginConfig;
import com.atlassian.clover.idea.config.MappedCloverPluginConfig;
import com.intellij.openapi.diagnostic.Logger;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.JpsElementChildRole;
import org.jetbrains.jps.model.JpsElementFactory;
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.JpsSimpleElement;
import org.jetbrains.jps.model.serialization.JpsProjectExtensionSerializer;

/**
 * Serialization of project settings. Clover stores them in project's workspace file (*.iws).
 */
public class CloverJpsProjectConfigurationSerializer extends JpsProjectExtensionSerializer {

    /**
     * Helper class used as a key for data stored in project context
     * @see org.jetbrains.jps.model.JpsProject#getContainer()
     */
    public static class CloverProjectConfigurationRole extends JpsElementChildRole<JpsSimpleElement<CloverPluginConfig>> {
        public static final CloverProjectConfigurationRole INSTANCE = new CloverProjectConfigurationRole();
    }

    private final Logger LOG = Logger.getInstance(CloverJpsProjectConfigurationSerializer.class.getName());

    /**
     * Create serializer which will parse {@link IdeaXmlConfigConstants#WORKSPACE_FILE_CLOVER_COMPONENT_NAME}
     * tag from {@link #WORKSPACE_FILE}
     */
    public CloverJpsProjectConfigurationSerializer() {
        super(WORKSPACE_FILE, IdeaXmlConfigConstants.WORKSPACE_FILE_CLOVER_COMPONENT_NAME);
    }

    /**
     * Parse configuration like:
     * <pre>
     * &lt;component name="CloverPlugin" class="com.atlassian.clover.idea.config.IdeaCloverConfig">
     *   &lt;loadPerTestData>true&lt;/loadPerTestData>
     *   &lt;projectRebuild class="com.atlassian.clover.idea.config.ProjectRebuild">ASK&lt;/projectRebuild>
     *   ...
     *   &lt;highlightCovered>true&lt;/highlightCovered>
     * &lt;component>
     * </pre>
     *
     * Method calls JDOMExternUtil.writeTo() which uses a trick that it puts child nodes from Element
     * as properties into IdeaCloverConfig.
     *
     * @param jpsProject   project into which configuration is put
     * @param componentTag input data to be parsed
     */
    @Override
    public void loadExtension(@NotNull JpsProject jpsProject, @NotNull Element componentTag) {
        // using MappedCloverPluginConfig instead of IdeaCloverConfig as we don't need all settings and we avoid
        // dependencies to modules outside jps-builders
        CloverPluginConfig projectConfig = new MappedCloverPluginConfig();
        try {
            // convert Element to IdeaCloverConfig
            JDOMExternUtil.readTo(componentTag, projectConfig);

            // put configuration under the JpsProject
            jpsProject.getContainer().setChild(
                    CloverProjectConfigurationRole.INSTANCE,
                    JpsElementFactory.getInstance().createSimpleElement(projectConfig));
        } catch (Exception ex) {
            LOG.error(ex);
        }

    }

    @Override
    public void saveExtension(@NotNull JpsProject jpsProject, @NotNull Element componentTag) {
        // not needed
    }
}
