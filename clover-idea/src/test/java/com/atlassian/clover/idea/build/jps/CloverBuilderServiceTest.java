package org.openclover.idea.build.jps;

import org.jetbrains.jps.incremental.ModuleLevelBuilder;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Test for {@link CloverBuilderService}
 */
public class CloverBuilderServiceTest {

    /**
     * @see org.openclover.idea.build.jps.CloverBuilderService#createModuleLevelBuilders()
     */
    @Test
    public void testCreateModuleLevelBuilders() {
        List<? extends ModuleLevelBuilder> moduleLevelBuilders = new CloverBuilderService().createModuleLevelBuilders();
        assertTrue(moduleLevelBuilders.contains(CloverJavaBuilder.getInstance()));
    }
}
