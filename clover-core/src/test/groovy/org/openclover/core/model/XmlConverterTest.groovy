package org.openclover.core.model

import org.junit.Before
import org.junit.Test
import org.openclover.core.api.registry.ClassInfo
import org.openclover.core.api.registry.FileInfo
import org.openclover.core.api.registry.HasMetrics
import org.openclover.core.api.registry.PackageInfo
import org.openclover.core.api.registry.ProjectInfo
import org.openclover.core.registry.entities.FullProjectInfo
import org.openclover.core.api.registry.HasMetricsFilter
import org.openclover.core.reporters.util.HistoricalSupport

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

class XmlConverterTest {

    private File xmlFile
    private File xmlSample
    private double EXPECTED_PROJECT = 0.5165
    private double EXPECTED_PACKAGE = 0.8375
    private String EXPECTED_PACKAGE_NAME = "com.cenqua"
    private String EXPECTED_FILE_NAME = "FullCover.java"

    @Before
    void setUp() throws Exception {
        xmlFile = new File((this.getClass().getResource("clover-historypoint.xml")).toURI())
        xmlSample = new File((this.getClass().getResource("clover-historypointsample.xml")).toURI())
    }

    @Test
    void testProjectLevel() throws Exception {
        HistoricalSupport.HasMetricsWrapper wrapper = new HistoricalSupport.HasMetricsWrapper(new FullProjectInfo(""), xmlFile)
        CoverageDataPoint model = XmlConverter.getFromXmlFile(wrapper.getDataFile(), XmlConverter.PROJECT_LEVEL)
        ProjectInfo project = model.getProject()

        assertEquals(EXPECTED_PROJECT, project.getMetrics().getPcCoveredElements(), 0.0001)
        assertTrue(project.getAllPackages().isEmpty())
    }

    @Test
    void testPackageLevel() throws Exception {
        HistoricalSupport.HasMetricsWrapper wrapper = new HistoricalSupport.HasMetricsWrapper(new FullProjectInfo(""), xmlFile)
        CoverageDataPoint model = XmlConverter.getFromXmlFile(wrapper.getDataFile(), XmlConverter.PACKAGE_LEVEL)
        ProjectInfo project = model.getProject()

        assertEquals(EXPECTED_PROJECT, project.getMetrics().getPcCoveredElements(), 0.0001)
        assertTrue(project.getFiles(HasMetricsFilter.ACCEPT_ALL).isEmpty())

        assertEquals(5, project.getAllPackages().size())
        int packageIndex = getInfoIndex(project.getAllPackages(), EXPECTED_PACKAGE_NAME)
        assertTrue(packageIndex != -1)
        assertEquals(EXPECTED_PACKAGE, project.getAllPackages().get(packageIndex).getMetrics().getPcCoveredElements(), 0.0001)

    }

    private int getInfoIndex(List list, String name) {
        for (int i = 0; i < list.size(); i++) {
            if (((HasMetrics) list.get(i)).getName().equals(name)) {
                return i
            }
        }
        return -1
    }

    @Test
    void testFileLevel() throws Exception {
        HistoricalSupport.HasMetricsWrapper wrapper = new HistoricalSupport.HasMetricsWrapper(new FullProjectInfo(""), xmlFile)
        CoverageDataPoint model = XmlConverter.getFromXmlFile(wrapper.getDataFile(), XmlConverter.FILE_LEVEL)
        ProjectInfo project = model.getProject()

        assertEquals(EXPECTED_PROJECT, project.getMetrics().getPcCoveredElements(), 0.0001)
        assertTrue(project.getClasses(HasMetricsFilter.ACCEPT_ALL).isEmpty())

        assertEquals(5, project.getAllPackages().size())

        int packageIndex = getInfoIndex(project.getAllPackages(), EXPECTED_PACKAGE_NAME)
        assertTrue(packageIndex != -1)
        assertEquals(EXPECTED_PACKAGE, project.getAllPackages().get(packageIndex).getMetrics().getPcCoveredElements(), 0.0001)
        
        int fileIndex = getInfoIndex(project.getFiles(HasMetricsFilter.ACCEPT_ALL), EXPECTED_FILE_NAME)
        assertTrue(fileIndex != -1)
        assertEquals(7, project.getFiles(HasMetricsFilter.ACCEPT_ALL).size())
        assertEquals(1, project.getFiles(HasMetricsFilter.ACCEPT_ALL).get(fileIndex).getMetrics().getPcCoveredElements(), 0)
    }

    @Test
    void testInnerClassMetrics() throws Exception {
        HistoricalSupport.HasMetricsWrapper wrapper = new HistoricalSupport.HasMetricsWrapper(new FullProjectInfo(""), xmlSample)
        CoverageDataPoint model = XmlConverter.getFromXmlFile(wrapper.getDataFile(), XmlConverter.CLASS_LEVEL)
        ProjectInfo project = model.getProject()
        PackageInfo pkg = project.getNamedPackage("org.apache.tools.ant.taskdefs")

        FileInfo file = pkg.getFiles().get(0)
        ClassInfo class0 = file.getClasses().get(0)
        ClassInfo class1 = file.getClasses().get(1)

        assertEquals(123, class0.getMetrics().getNumCoveredElements())
        assertEquals(279, class0.getMetrics().getNumElements())
        assertEquals(65, class0.getMetrics().getComplexity())

        assertEquals(10, class1.getMetrics().getNumCoveredElements())
        assertEquals(11, class1.getMetrics().getNumElements())
        assertEquals(5, class1.getMetrics().getComplexity())
    }

}
