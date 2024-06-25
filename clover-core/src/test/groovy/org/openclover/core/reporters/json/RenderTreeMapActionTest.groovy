package org.openclover.core.reporters.json

import org.apache.velocity.VelocityContext
import org.junit.Before
import org.junit.Test
import org.openclover.core.TestUtils
import org.openclover.core.api.registry.ClassInfo
import org.openclover.core.api.registry.HasMetrics
import org.openclover.core.api.registry.PackageInfo
import org.openclover.core.api.registry.ProjectInfo
import org.openclover.core.registry.entities.FullClassInfo
import org.openclover.core.registry.metrics.BlockMetrics
import org.openclover.core.api.registry.HasMetricsFilter
import org.openclover.core.registry.metrics.PackageMetrics
import org.openclover.core.registry.metrics.ProjectMetrics
import org.openclover.core.reporters.CloverReportConfig
import org.openclover.core.reporters.Current
import org.openclover.core.reporters.Format

import java.util.concurrent.Callable

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when
import static org.openclover.core.util.Lists.newLinkedList

class RenderTreeMapActionTest {

    File tmpDir

    @Before
    void setUp() throws Exception {
        tmpDir = TestUtils.createEmptyDirFor(getClass())
    }

    @Test
    void testCall() throws Exception {
        final ProjectInfo project = createMockProject()

        final VelocityContext context = new VelocityContext()
        final CloverReportConfig reportConfig = new Current()
        reportConfig.setFormat(Format.DEFAULT_HTML)
        final Callable action = new RenderTreeMapAction(context, tmpDir, project)
        final String json = (String) action.call()

        // quick check of the json object structure
        final JSONObject jsonObj = new JSONObject(json)
        assertEquals("", jsonObj.get("name"))
        assertEquals(20.0d, jsonObj.getJSONObject("data").get('$area'), 0.001d)
        assertEquals(80.0d, jsonObj.getJSONObject("data").get('$color'), 0.001d)
        // check there is one package
        assertEquals(1, jsonObj.getJSONArray("children").length())
        // check there are three classes in that package
        assertEquals(3, jsonObj.getJSONArray("children").getJSONObject(0).getJSONArray("children").length())

        assertNotNull("No json object in velocity context", context.get("json"))
        assertEquals("callback name must match that in treemap.vm","processTreeMapJson", context.get("callback"))
    }

    @Test
    void testGenerateJson() throws Exception {
        final ProjectInfo project = createMockProject()

        final VelocityContext context = new VelocityContext()
        final CloverReportConfig reportConfig = new Current()
        final RenderTreeMapAction action = new RenderTreeMapAction(context, tmpDir, project)
        final String json = action.generateJson(false)

        // quick check of the json object structure
        final JSONObject jsonObj = new JSONObject(json)
        // check there is one package
        final JSONArray pkgs = jsonObj.getJSONArray("children")
        assertEquals(1, pkgs.length())
        // check there are no classes in that package
        assertEquals(0, pkgs.getJSONObject(0).getJSONArray("children").length())

    }
    
    private ProjectInfo createMockProject() {
        // mock a Project
        final ProjectInfo project = mock(ProjectInfo.class)
        ProjectMetrics projMetrics = new ProjectMetrics(project)
        projMetrics.setNumStatements(20)
        projMetrics.setNumCoveredStatements(16)
        // mock the project with placeholder metrics
        when(project.getMetrics()).thenReturn(projMetrics)
        when(project.getName()).thenReturn("")

        // mock a package
        final PackageInfo pkgInfo = mock(PackageInfo.class)
        when(pkgInfo.getName()).thenReturn("com.test.pkg")
        PackageMetrics pkgMetrics = new PackageMetrics(pkgInfo)

        pkgMetrics.setNumStatements(20)
        pkgMetrics.setNumCoveredStatements(16)
        when(pkgInfo.getMetrics()).thenReturn(pkgMetrics)
        when((List)project.getAllPackages()).thenReturn([ pkgInfo ])


        // list of 3 child classes, for package
        final List<ClassInfo> classes = newLinkedList()
        mockHasMetrics(classes, mock(FullClassInfo.class), "TestClass1", 4, 2)
        mockHasMetrics(classes, mock(FullClassInfo.class), "TestClass2", 6, 4)
        mockHasMetrics(classes, mock(FullClassInfo.class), "TestClass3", 10, 10)
                
        when((List<ClassInfo>)pkgInfo.getClasses(HasMetricsFilter.ACCEPT_ALL)).thenReturn(classes)
        return project
    }

    private <T extends HasMetrics> void mockHasMetrics(List<T> infos, T hasMetrics, String name, int numStmts, int numCovered) {
        infos.add(hasMetrics)
        when(hasMetrics.getName()).thenReturn(name)
        BlockMetrics metrics = new BlockMetrics(hasMetrics)
        metrics.setNumStatements(numStmts)
        metrics.setNumCoveredStatements(numCovered)
        when(hasMetrics.getMetrics()).thenReturn(metrics)
    }
}
