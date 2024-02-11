package org.openclover.core.reporters.json

import clover.org.apache.velocity.VelocityContext
import com.atlassian.clover.api.registry.ClassInfo
import com.atlassian.clover.api.registry.HasMetrics
import com.atlassian.clover.registry.entities.FullClassInfo
import com.atlassian.clover.registry.entities.FullPackageInfo
import com.atlassian.clover.registry.entities.FullProjectInfo
import com.atlassian.clover.registry.metrics.BlockMetrics
import com.atlassian.clover.registry.metrics.HasMetricsFilter
import com.atlassian.clover.registry.metrics.PackageMetrics
import com.atlassian.clover.registry.metrics.ProjectMetrics
import com.atlassian.clover.reporters.CloverReportConfig
import com.atlassian.clover.reporters.Current
import com.atlassian.clover.reporters.Format
import com.atlassian.clover.reporters.json.JSONArray
import com.atlassian.clover.reporters.json.JSONObject
import com.atlassian.clover.reporters.json.RenderTreeMapAction
import org.openclover.core.TestUtils
import org.junit.Before
import org.junit.Test

import java.util.concurrent.Callable

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when
import static org.openclover.util.Lists.newLinkedList

class RenderTreeMapActionTest {

    File tmpDir

    @Before
    void setUp() throws Exception {
        tmpDir = TestUtils.createEmptyDirFor(getClass())
    }

    @Test
    void testCall() throws Exception {
        final FullProjectInfo project = createMockProject()

        final VelocityContext context = new VelocityContext()
        final CloverReportConfig reportConfig = new Current()
        reportConfig.setFormat(Format.DEFAULT_HTML)
        final Callable action = new RenderTreeMapAction(context, reportConfig, tmpDir, project)
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
        final FullProjectInfo project = createMockProject()

        final VelocityContext context = new VelocityContext()
        final CloverReportConfig reportConfig = new Current()
        final RenderTreeMapAction action = new RenderTreeMapAction(context, reportConfig, tmpDir, project)
        final String json = action.generateJson(false)

        // quick check of the json object structure
        final JSONObject jsonObj = new JSONObject(json)
        // check there is one package
        final JSONArray pkgs = jsonObj.getJSONArray("children")
        assertEquals(1, pkgs.length())
        // check there are no classes in that package
        assertEquals(0, pkgs.getJSONObject(0).getJSONArray("children").length())

    }
    
    private FullProjectInfo createMockProject() {
        // mock a Project
        final FullProjectInfo project = mock(FullProjectInfo.class)
        ProjectMetrics projMetrics = new ProjectMetrics(project)
        projMetrics.setNumStatements(20)
        projMetrics.setNumCoveredStatements(16)
        // mock the project with placeholder metrics
        when(project.getMetrics()).thenReturn(projMetrics)
        when(project.getName()).thenReturn("")

        // mock a package
        final FullPackageInfo pkgInfo = mock(FullPackageInfo.class)
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
