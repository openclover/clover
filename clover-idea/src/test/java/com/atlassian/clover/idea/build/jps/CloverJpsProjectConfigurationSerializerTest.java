package com.atlassian.clover.idea.build.jps;

import com.atlassian.clover.idea.config.CloverPluginConfig;
import com.atlassian.clover.idea.config.ProjectRebuild;
import com.atlassian.clover.idea.config.regexp.Regexp;
import org.jdom.Element;
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.JpsSimpleElement;
import org.jetbrains.jps.model.impl.JpsModelImpl;
import org.jetbrains.jps.model.impl.JpsProjectImpl;
import org.junit.Test;
import org.hamcrest.core.IsCollectionContaining;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test for {@link CloverJpsProjectConfigurationSerializer}
 */
public class CloverJpsProjectConfigurationSerializerTest {

    /**
     * @see CloverJpsProjectConfigurationSerializer#loadExtension(org.jetbrains.jps.model.JpsProject, org.jdom.Element)
     */
    @Test
    public void testLoadExtension() {
        final CloverJpsProjectConfigurationSerializer serializer = new CloverJpsProjectConfigurationSerializer();
        final JpsProject jpsProject = new JpsProjectImpl(new JpsModelImpl(null), null);

        // serialize data
        serializer.loadExtension(jpsProject, createSampleData());

        // fetch from project configuration and validate
        final JpsSimpleElement<CloverPluginConfig> data = jpsProject.getContainer().getChild(CloverJpsProjectConfigurationSerializer.CloverProjectConfigurationRole.INSTANCE);
        assertNotNull(data);

        // test: we check only values related with instrumentation; ui stuff like colors etc is not
        // being verified as we don't need them during build
        final CloverPluginConfig config = data.getData();
        assertEquals(true, config.isLoadPerTestData());
        assertEquals(ProjectRebuild.ALWAYS, config.getProjectRebuild());
        assertEquals(false, config.isRelativeInitString());

        assertEquals(false, config.isPeriodicRefresh());
        assertEquals(CloverPluginConfig.DIRECTED_FLUSHING, config.getFlushPolicy());
        assertEquals("1.3", config.getLanguageLevel());
        assertEquals("myclover.db", config.getManualInitString());
        assertEquals(5000, config.getFlushInterval());
        assertEquals(true, config.isEnabled());
        assertEquals(null, config.getExcludes());

        assertEquals(null, config.getIncludes());
        assertEquals("", config.getContextFilterSpec());
        assertEquals(false, config.getUseGeneratedInitString());

        final Regexp methodRegExp = createRegExp(0, null, ".*appendMoney.*", true, "method_appendMoney", true);
        final Regexp statementRegExp = createRegExp(1, null, "if.*", true, "stmt_if", false);
        assertEquals(2, config.getRegexpContexts().size());
        assertThat(config.getRegexpContexts(), IsCollectionContaining.hasItem(methodRegExp));
        assertThat(config.getRegexpContexts(), IsCollectionContaining.hasItem(statementRegExp));

        assertEquals("0 days", config.getSpan());
        assertEquals(".clover\\coverage.db", config.getGeneratedInitString());

        // we need to check two extra UI things, which affects the instrumentation:
        // state of the "Instrument test source folders ..." from project settings
        assertEquals(true, config.isInstrumentTests());
        // state of the "Build with Clover" button from top menu (used to quickly disable clover)
        assertEquals(true, config.isBuildWithClover());
    }

    private Regexp createRegExp(int type, String validationMessage, String regex, boolean enabled, String name,
                                boolean changed) {
        final Regexp ret = new Regexp();
        ret.setType(type);
        ret.setValidationMessage(validationMessage);
        ret.setRegex(regex);
        ret.setEnabled(enabled);
        ret.setName(name);
        ret.setChanged(changed);
        return ret;
    }

    /**
     * Returns sample data:
     * <pre>
     *   <component name="CloverPlugin" class="com.atlassian.clover.idea.config.IdeaCloverConfig">
     *     <loadPerTestData>true</loadPerTestData>
     *     <projectRebuild class="com.atlassian.clover.idea.config.ProjectRebuild">ALWAYS</projectRebuild>
     *     ...
     *   </component>
     * </pre>
     *
     * @return Element
     */
    private Element createSampleData() {
        final Element root = new Element("component").setAttribute("name", "CloverPlugin").setAttribute("class", "com.atlassian.clover.idea.config.IdeaCloverConfig");

        root.addContent(new Element("loadPerTestData").addContent("true"));
        root.addContent(new Element("projectRebuild").setAttribute("class", "com.atlassian.clover.idea.config.ProjectRebuild").addContent("ALWAYS"));
        root.addContent(new Element("filteredHighlight").addContent("-16751002"));
        root.addContent(new Element("relativeInitString").addContent("false"));
        root.addContent(new Element("buildWithClover").addContent("true"));
        root.addContent(new Element("lastProjectConfigTabSelected").addContent("0"));
        root.addContent(new Element("periodicRefresh").addContent("false"));
        root.addContent(new Element("notCoveredStripe").addContent("-26215"));
        root.addContent(new Element("flushPolicy").addContent("0"));
        root.addContent(new Element("viewIncludeAnnotation").addContent("true"));
        root.addContent(new Element("alwaysExpandTestClasses").addContent("true"));
        root.addContent(new Element("languageLevel").addContent("1.3"));
        root.addContent(new Element("manualInitString").addContent("myclover.db"));
        root.addContent(new Element("autoRefreshInterval").addContent("2000"));
        root.addContent(new Element("flushInterval").addContent("5000"));
        root.addContent(new Element("showInline").addContent("true"));
        root.addContent(new Element("showSummaryInToolwindow").addContent("false"));
        root.addContent(new Element("autoRefresh").addContent("true"));
        root.addContent(new Element("coveredHighlight").addContent("-6684775"));
        root.addContent(new Element("enabled").addContent("true"));
        root.addContent(new Element("alwaysCollapseTestClasses").addContent("false"));
        root.addContent(new Element("filteredStripe").addContent("-12566464"));
        root.addContent(new Element("excludes").setAttribute("null", "true"));
        root.addContent(new Element("calculateTestCoverage").addContent("true"));
        root.addContent(new Element("instrumentTests").addContent("true"));
        root.addContent(new Element("outOfDateHighlight").addContent("-3381760"));
        root.addContent(new Element("notCoveredHighlight").addContent("-26215"));
        root.addContent(new Element("hideFullyCovered").addContent("false"));
        root.addContent(new Element("cloudReportIncludeSubpkgs").addContent("true"));
        root.addContent(new Element("includes").setAttribute("null", "true"));
        root.addContent(new Element("includePassedTestCoverageOnly").addContent("true"));
        root.addContent(new Element("testViewScope").setAttribute("class", "com.atlassian.clover.idea.config.TestViewScope").addContent("GLOBAL"));
        root.addContent(new Element("testCaseLayout").setAttribute("class", "com.atlassian.clover.idea.config.TestCaseLayout").addContent("PACKAGES"));
        root.addContent(new Element("viewCoverage").addContent("true"));
        root.addContent(new Element("contextFilterSpec"));
        root.addContent(new Element("coveredStripe").addContent("-16711936"));
        root.addContent(new Element("autoViewInCloudReport").addContent("true"));
        root.addContent(new Element("autoScroll").addContent("true"));
        root.addContent(new Element("useGeneratedInitString").addContent("false"));
        root.addContent(new Element("showGutter").addContent("true"));
        root.addContent(new Element("failedCoveredStripe").addContent("-14336"));

        // <regexpContexts class="java.util.ArrayList">
        //    <item class="com.atlassian.clover.idea.config.regexp.Regexp">
        //        <type>0</type>
        //        <validationMessage null="true" />
        //        <regex>.*appendMoney.*</regex>
        //        <enabled>true</enabled>
        //        <name>method_appendMoney</name>
        //        <changed>true</changed>
        //    </item>
        //    <item class="com.atlassian.clover.idea.config.regexp.Regexp">
        //        <type>1</type>
        //        <validationMessage null="true" />
        //        <regex>if.*</regex>
        //        <enabled>true</enabled>
        //        <name>stmt_if</name>
        //        <changed>false</changed>
        //    </item>
        // </regexpContexts>
        root.addContent(
                new Element("regexpContexts").setAttribute("class", "java.util.ArrayList")
                        .addContent(new Element("item").setAttribute("class", "com.atlassian.clover.idea.config.regexp.Regexp")
                                .addContent(new Element("type").addContent("0"))
                                .addContent(new Element("validationMessage").setAttribute("null", "true"))
                                .addContent(new Element("regex").addContent(".*appendMoney.*"))
                                .addContent(new Element("enabled").addContent("true"))
                                .addContent(new Element("name").addContent("method_appendMoney"))
                                .addContent(new Element("changed").addContent("true")))
                        .addContent(new Element("item").setAttribute("class", "com.atlassian.clover.idea.config.regexp.Regexp")
                                .addContent(new Element("type").addContent("1"))
                                .addContent(new Element("validationMessage").setAttribute("null", "true"))
                                .addContent(new Element("regex").addContent("if.*"))
                                .addContent(new Element("enabled").addContent("true"))
                                .addContent(new Element("name").addContent("stmt_if"))
                                .addContent(new Element("changed").addContent("false")))
        );

        root.addContent(new Element("flattenPackages").addContent("true"));
        root.addContent(new Element("autoScrollFromSource").addContent("false"));
        root.addContent(new Element("showErrorMarks").addContent("true"));
        root.addContent(new Element("showTooltips").addContent("true"));
        root.addContent(new Element("showSummaryInToolbar").addContent("true"));
        root.addContent(new Element("span").addContent("0 days"));
        root.addContent(new Element("generatedInitString").addContent(".clover\\coverage.db"));
        root.addContent(new Element("modelScope").setAttribute("class", "com.atlassian.clover.idea.config.ModelScope").addContent("ALL_CLASSES"));
        root.addContent(new Element("outOfDateStripe").addContent("-256"));
        root.addContent(new Element("failedCoveredHighlight").addContent("-13261"));
        root.addContent(new Element("highlightCovered").addContent("true"));

        return root;
    }
}
