package org.openclover.idea.build.jps;

import org.openclover.idea.config.CloverGlobalConfig;
import org.jdom.Element;
import org.jetbrains.jps.model.JpsElement;
import org.jetbrains.jps.model.JpsModel;
import org.jetbrains.jps.model.JpsSimpleElement;
import org.jetbrains.jps.model.impl.JpsModelImpl;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test for {@link CloverJpsGlobalConfigurationSerializer}
 */
public class CloverJpsGlobalConfigurationSerializerTest {

    /**
     * @see CloverJpsGlobalConfigurationSerializer#loadExtension(org.jetbrains.jps.model.JpsGlobal, org.jdom.Element)
     */
    @Test
    public void testLoadExtension() {
        final CloverJpsGlobalConfigurationSerializer serializer = new CloverJpsGlobalConfigurationSerializer();
        final JpsModel jpsModel = new JpsModelImpl(null);

        // put sample data into model
        serializer.loadExtension(jpsModel.getGlobal(), createSampleData());

        // read this data back
        final JpsElement jpsElement = jpsModel.getGlobal().getContainer().getChild(CloverJpsGlobalConfigurationSerializer.CloverGlobalSettingsRole.INSTANCE);
        assertNotNull(jpsElement);
        assertTrue(jpsElement instanceof JpsSimpleElement);

        final CloverGlobalConfig data = ((JpsSimpleElement<CloverGlobalConfig>)jpsElement).getData();
        assertNotNull(data);
    }


    /**
     * Returns a following content:
     *
     * <pre>
     * &lt;component name="Clover"&gt;
     * &lt;/component&gt;
     * </pre>
     * @return Element
     */
    protected Element createSampleData() {
        final Element root = new Element("component").setAttribute("name", "Clover");
        return root;
    }
}
