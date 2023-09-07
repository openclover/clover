package com.atlassian.clover.idea.junit.config;

import junit.framework.TestCase;
import org.jdom.Element;
import com.atlassian.clover.api.optimization.OptimizationOptions;


public class OptimizedConfigurationSettingsTest extends TestCase {
    public void testSerialization() throws Exception {
        Element e = new Element("element");

        OptimizedConfigurationSettings orig = new OptimizedConfigurationSettings();
        orig.setDiscardSnapshots(true);
        orig.setCompilesBeforeStaleSnapshot(123);
        orig.setMinimize(true);
        orig.setReorder(OptimizationOptions.TestSortOrder.FAILFAST);

        orig.writeExternal(e);

        OptimizedConfigurationSettings copy = new OptimizedConfigurationSettings();
        copy.readExternal(e);
        assertEquals(orig.isDiscardSnapshots(), copy.isDiscardSnapshots());
        assertEquals(orig.getCompilesBeforeStaleSnapshot(), copy.getCompilesBeforeStaleSnapshot());
        assertEquals(orig.isMinimize(), copy.isMinimize());
        assertEquals(orig.getReorder(), copy.getReorder());

        orig.setDiscardSnapshots(false);
        orig.setCompilesBeforeStaleSnapshot(0);
        orig.setMinimize(false);
        orig.setReorder(OptimizationOptions.TestSortOrder.RANDOM);

        e = new Element("element");
        orig.writeExternal(e);

        copy = new OptimizedConfigurationSettings();
        copy.readExternal(e);
        assertEquals(orig.isDiscardSnapshots(), copy.isDiscardSnapshots());
        assertEquals(orig.getCompilesBeforeStaleSnapshot(), copy.getCompilesBeforeStaleSnapshot());
        assertEquals(orig.isMinimize(), copy.isMinimize());
        assertEquals(orig.getReorder(), copy.getReorder());

    }
}
