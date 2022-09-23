package com.atlassian.clover.idea.build.jps;

import com.atlassian.clover.idea.build.InclusionDetector;
import com.atlassian.clover.idea.config.CloverModuleConfig;
import com.atlassian.clover.idea.config.CloverPluginConfig;
import com.atlassian.clover.idea.config.MappedCloverPluginConfig;
import org.jetbrains.jps.devkit.model.JpsPluginModuleProperties;
import org.jetbrains.jps.devkit.model.JpsPluginModuleType;
import org.jetbrains.jps.model.JpsElementFactory;
import org.jetbrains.jps.model.JpsModel;
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.impl.JpsModelImpl;
import org.jetbrains.jps.model.impl.JpsSimpleElementImpl;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.jps.model.java.JpsJavaModuleType;
import org.jetbrains.jps.model.module.JpsModule;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test for {@link JpsProjectInclusionDetector}
 */
public class JpsProjectInclusionDetectorTest {

    protected JpsProject sampleProject;

    @Before
    public void setUp() {
        sampleProject = createSampleProject(createCloverConfig());
    }

    /**
     * Check detection of java vs non-java sources.
     */
    @Test
    public void testProcessFile_JavaVsNonJava() {
        InclusionDetector detector = JpsProjectInclusionDetector.processFile(sampleProject, new File("Project/Foo.groovy"));
        assertNotNull(detector);
        assertTrue(detector.isNotJava());

        detector = JpsProjectInclusionDetector.processFile(sampleProject, new File("Project/Foo.java"));
        assertNotNull(detector);
        assertFalse(detector.isNotJava());
    }

    /**
     * Check detection of whether Clover is disabled. config.isEnabled() && config.isBuildWithClover()
     */
    @Test
    public void testProcessFile_CloverDisabled() {
        // enabled=true, buildWithClover=true ==> clover is enabled
        CloverPluginConfig config = createCloverConfig();
        JpsProject project = createSampleProject(config);
        InclusionDetector detector = JpsProjectInclusionDetector.processFile(project, new File("Project/Foo.java"));
        assertNotNull(detector);
        assertFalse(detector.isCloverDisabled());

        // enabled=false, buildWithClover=true ==> clover is disabled
        config.setEnabled(false);
        detector = JpsProjectInclusionDetector.processFile(project, new File("Project/Foo.java"));
        assertNotNull(detector);
        assertTrue(detector.isCloverDisabled());

        // enabled=true, buildWithClover=false ==> clover is disabled
        config.setEnabled(true);
        config.setBuildWithClover(false);
        detector = JpsProjectInclusionDetector.processFile(project, new File("Project/Foo.java"));
        assertNotNull(detector);
        assertTrue(detector.isCloverDisabled());
    }

    /**
     * Check how exclusion of specific modules works.
     */
    @Test
    public void testProcessFile_ModuleExclusion() {
        JpsProjectInclusionDetector detector;

        // case: find source file in a JAVA_MODULE
        detector = (JpsProjectInclusionDetector) JpsProjectInclusionDetector.processFile(sampleProject,
                new File("Project/ModuleA/src/Foo.java"));
        assertNotNull(detector);
        assertNotNull(detector.getEnclosingModule());
        assertFalse(detector.isModuleNotFound());
        assertFalse(detector.isModuleExcluded());
        assertEquals("ModuleA", detector.getEnclosingModule().getName());

        // case: find source file in a PLUGIN_MODULE
        detector = (JpsProjectInclusionDetector) JpsProjectInclusionDetector.processFile(sampleProject,
                new File("Project/ModuleB/src/Foo.java"));
        assertNotNull(detector);
        assertNotNull(detector.getEnclosingModule());
        assertFalse(detector.isModuleNotFound());
        assertTrue(detector.isModuleExcluded());
        assertEquals("ModuleB", detector.getEnclosingModule().getName());

        // case: find source in a module nested inside other module, module is excluded
        detector = (JpsProjectInclusionDetector) JpsProjectInclusionDetector.processFile(sampleProject,
                new File("Project/ModuleA/ModuleAA/src/Foo.java"));
        assertNotNull(detector);
        assertNotNull(detector.getEnclosingModule());
        assertFalse(detector.isModuleNotFound());
        assertFalse(detector.isModuleExcluded());
        assertEquals("ModuleAA", detector.getEnclosingModule().getName());

        // case: find source in a module nested inside other module, module is not excluded
        detector = (JpsProjectInclusionDetector) JpsProjectInclusionDetector.processFile(sampleProject,
                new File("Project/ModuleA/ModuleAB/src/Foo.java"));
        assertNotNull(detector);
        assertNotNull(detector.getEnclosingModule());
        assertFalse(detector.isModuleNotFound());
        assertTrue(detector.isModuleExcluded());
        assertEquals("ModuleAB", detector.getEnclosingModule().getName());

        // case: source file outside any module
        detector = (JpsProjectInclusionDetector) JpsProjectInclusionDetector.processFile(sampleProject,
                new File("Project/ModuleNotFound/src/Foo.java"));
        assertNotNull(detector);
        assertTrue(detector.isModuleNotFound());
        assertNull(detector.getEnclosingModule());
    }

    /**
     * Check detection of 'app source' / 'test source' folders.
     */
    @Test
    public void testProcessFile_MainVsTestSourceRoots() {
        // modify standard project and disable test source instrumentation
        final CloverPluginConfig cloverPluginConfig = createCloverConfig();
        cloverPluginConfig.setInstrumentTests(false);
        sampleProject.getContainer().setChild(
                CloverJpsProjectConfigurationSerializer.CloverProjectConfigurationRole.INSTANCE,
                new JpsSimpleElementImpl<>(cloverPluginConfig));

        // find regular file in an app source folder, in a nested module, where parent has excluded directory set
        JpsProjectInclusionDetector detector = (JpsProjectInclusionDetector) JpsProjectInclusionDetector.processFile(
                sampleProject, new File("Project/ModuleA/ModuleAA/src/MyFoo.java"));
        assertTrue(detector.isIncluded());
        assertFalse(detector.isInNoninstrumentedTestSources());

        // find file in an test source folder, in a nested module, where parent has excluded directory set
        // out of scope of instrumentation because it's a test source
        detector = (JpsProjectInclusionDetector) JpsProjectInclusionDetector.processFile(
                sampleProject, new File("Project/ModuleA/ModuleAA/test/MyFooTest.java"));
        assertTrue(detector.isInNoninstrumentedTestSources());
        assertFalse(detector.isIncluded());
    }

    /**
     * Check processing of include / exclude patterns.
     */
    @Test
    public void testProcessFile_IncludesExcludes() {
        // case: matches includes = included
        JpsProjectInclusionDetector detector = (JpsProjectInclusionDetector) JpsProjectInclusionDetector.processFile(
                sampleProject, new File("Project/ModuleA/ModuleAA/src/MyIncludedFile.java"));
        assertTrue(detector.isIncluded());

        // case: does not match includes = excluded
        detector = (JpsProjectInclusionDetector) JpsProjectInclusionDetector.processFile(
                sampleProject, new File("Project/ModuleA/ModuleAA/src/NotIncludedFile.java"));
        assertFalse(detector.isIncluded());

        // case: matches includes but also matches excludes = excluded
        detector = (JpsProjectInclusionDetector) JpsProjectInclusionDetector.processFile(
                sampleProject, new File("Project/ModuleA/ModuleAA/src/NotExcludedFile.java"));
        assertFalse(detector.isIncluded());
    }

    /**
     * Creates a sample project with a following structure:
     *
     * <pre>
     *    Project
     *      ModuleA   - JAVA_MODULE
     *        src     - app source root    (path Project/ModuleA/src)
     *        test    - test source root   (path Project/ModuleA/test)
     *        ModuleAA - excluded source root, JAVA_MODULE
     *           src  - app source root    (path Project/ModuleA/ModuleAA/src)
     *           test - test source root   (path Project/ModuleA/ModuleAA/test)
     * ModuleAB - excluded source root, JAVA_MODULE, excluded from instrumentation
     *           src  - app source root    (path Project/ModuleA/ModuleAA/src)
     *           test - test source root   (path Project/ModuleA/ModuleAA/test)
     *      ModuleB   - excluded from instrumentation, PLUGIN_MODULE
     *        src     - app source root
     *        test    - test source root
     * </pre>
     *
     * the project has a following Clover configuration attached:
     * <pre>
     *    clover enabled: true
     *    build with clover enabled: true
     *    includes: **\My*.java
     *    excludes: **\MyExcluded*.java
     *    instrument tests: true
     * </pre>
     *
     * @return JpsProject
     */
    protected JpsProject createSampleProject(CloverPluginConfig cloverPluginConfig) {
        final JpsModel jpsModel = new JpsModelImpl(null);
        final JpsProject jpsProject = jpsModel.getProject();
        final JpsElementFactory elementFactory = JpsElementFactory.getInstance();

        // ModuleA
        final JpsModule moduleA = elementFactory.createModule("ModuleA", JpsJavaModuleType.INSTANCE,
                elementFactory.createDummyElement());
        moduleA.addSourceRoot("file://Project/ModuleA/src", JavaSourceRootType.SOURCE);
        moduleA.addSourceRoot("file://Project/ModuleA/test", JavaSourceRootType.TEST_SOURCE);
        moduleA.getExcludeRootsList().addUrl("file://Project/ModuleA/ModuleAA");
        moduleA.getExcludeRootsList().addUrl("file://Project/ModuleA/ModuleAB");
        jpsProject.addModule(moduleA);

        // ModuleAA
        final JpsModule moduleAA = elementFactory.createModule("ModuleAA", JpsJavaModuleType.INSTANCE,
                elementFactory.createDummyElement());
        moduleAA.addSourceRoot("file://Project/ModuleA/ModuleAA/src", JavaSourceRootType.SOURCE);
        moduleAA.addSourceRoot("file://Project/ModuleA/ModuleAA/test", JavaSourceRootType.TEST_SOURCE);
        jpsProject.addModule(moduleAA);

        // ModuleAB
        final JpsModule moduleAB = elementFactory.createModule("ModuleAB", JpsJavaModuleType.INSTANCE,
                elementFactory.createDummyElement());
        moduleAB.addSourceRoot("file://Project/ModuleA/ModuleAB/src", JavaSourceRootType.SOURCE);
        moduleAB.addSourceRoot("file://Project/ModuleA/ModuleAB/test", JavaSourceRootType.TEST_SOURCE);
        moduleAB.getContainer().setChild(CloverSerializerExtension.CloverModuleConfigurationRole.INSTANCE,
                elementFactory.createSimpleElement(new CloverModuleConfig(true))); // excluded from instrumentation
        jpsProject.addModule(moduleAB);

        // ModuleB
        final JpsModule moduleB = elementFactory.createModule("ModuleB", JpsPluginModuleType.INSTANCE,
                elementFactory.createSimpleElement(
                        new JpsPluginModuleProperties("file://plugin.xml", "file://MANIFEST.MF")));
        moduleB.addSourceRoot("file://Project/ModuleB/src", JavaSourceRootType.SOURCE);
        moduleB.addSourceRoot("file://Project/ModuleB/test", JavaSourceRootType.TEST_SOURCE);
        moduleB.getContainer().setChild(CloverSerializerExtension.CloverModuleConfigurationRole.INSTANCE,
                elementFactory.createSimpleElement(new CloverModuleConfig(true))); // excluded from instrumentation
        jpsProject.addModule(moduleB);


        // add project-level configuration for Clover
        jpsProject.getContainer().setChild(
                CloverJpsProjectConfigurationSerializer.CloverProjectConfigurationRole.INSTANCE,
                new JpsSimpleElementImpl<>(cloverPluginConfig));

        return jpsProject;
    }

    /**
     * @return CloverPluginConfig
     */
    protected CloverPluginConfig createCloverConfig() {
        final CloverPluginConfig config = new MappedCloverPluginConfig();
        config.setEnabled(true);
        config.setBuildWithClover(true);
        config.setIncludes("**/My*.java");
        config.setExcludes("**/MyExcluded*.java");
        config.setInstrumentTests(true);
        return config;
    }
}
