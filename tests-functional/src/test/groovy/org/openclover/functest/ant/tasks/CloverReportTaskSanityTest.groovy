package org.openclover.functest.ant.tasks

import groovy.transform.CompileStatic
import org.openclover.core.api.registry.ClassInfo
import org.openclover.core.api.registry.FileInfo
import org.openclover.core.api.registry.PackageInfo
import org.openclover.core.api.registry.ProjectInfo
import org.openclover.core.model.CoverageDataPoint
import org.openclover.core.model.XmlConverter
import org.openclover.core.registry.metrics.ProjectMetrics
import org.openclover.runtime.CloverNames
import org.openclover.runtime.api.CloverException

import static org.openclover.functest.ant.testutils.MetricsHelper.assertMetricsEquals

@CompileStatic
class CloverReportTaskSanityTest extends CloverBuildFileTestBase{

    CloverReportTaskSanityTest(String name) {
        super(name)
    }

    String getAntFileName() {
        return "clover-sanity-report.xml"
    }

    void tearDown() throws Exception {
        executeTarget("tearDown")
        super.tearDown()
    }

    void testCloverMergeSanityTest() throws Exception {
        String db1 = util.getWorkDir().getAbsolutePath() + "/merge-db1/db/clover.db"
        getProject().setUserProperty(CloverNames.PROP_INITSTRING, db1)
        getProject().setProperty("outdir", util.getWorkDir().getAbsolutePath() + "merge-db1")
        getProject().executeTarget("cloverMoneyBags")

        String db2 = util.getWorkDir().getAbsolutePath() + "/merge-db2/db/clover.db"
        getProject().setUserProperty(CloverNames.PROP_INITSTRING, db2)
        getProject().setProperty("outdir", util.getWorkDir().getAbsolutePath() + "merge-db2")
        getProject().executeTarget("cloverMoneyBags")

        getProject().setProperty("outdir", util.getWorkDir().getAbsolutePath() + "merge-db3")
        getProject().setProperty("db1", db1)
        getProject().setProperty("db2", db2)
        getProject().executeTarget("testCloverMergeSanityTest")

        assertPropertySet("merged.db.available")
        assertPropertySet("merged.report.available")

    }

    void testCloverCheck() throws Exception {
        copyFile("expected-money-src.txt")
        copyFile("expected-linkified-stack-trace.txt")
        copyFile("expected-money-da-not-instrumented.txt")
        getProject().executeTarget("testCloverCheck")
        assertTrue(getProject().getProperty("check1.fail") == null) //failure property not set
        assertTrue(getProject().getProperty("check2.fail") == null) //failure property not set
    }

    void testMoneyBagHtmlReport() throws Exception {
        copyFile("expected-money-src.txt")
        copyFile("expected-linkified-stack-trace.txt")
        copyFile("expected-money-da-not-instrumented.txt")
        getProject().executeTarget("testMoneyBagHtmlReport")
        checkPropContainsExpected("pkg-summary-src")
        checkPropContainsExpected("index-src")
        checkPropContainsExpected("money-src")
        checkPropContainsExpected("money-da-not-instrumented")
        checkPropContainsExpected("linkified-stack-trace-src")
        assertPropertyEquals("excludes.success", "true")

        assertTrue(getFullLog().contains("Method context match, line 131, id=simple"))
        assertTrue(getFullLog().contains("Method context match, line 131, id=toString"))
        assertPropertyEquals("canned.success", "true")
        assertPropertyEquals("historical.success", "true")
    }

    void testCloverJsonReport() throws Exception {
        getProject().executeTarget("testJSONReport")
        final String mbJson = getProject().getProperty("moneybag.json")
        assertTrue(mbJson != null && mbJson.indexOf("\"CoveredStatements\": 93.333336") > 0)
    }

    void testFilterSanityTest() throws Exception {
        copyFile("expected-filtered-money.txt")
        copyFile("expected-filtered-xml-report.txt")
        copyFile("expected-failed-coverage-included-historypoint.txt")
        getProject().executeTarget("testFilterSanityTest")
        checkPropContainsExpected("filtered-money")

        asssertModelsFromXmlSame("expected-filtered-xml-report", "filtered-xml-report")
        asssertModelsFromXmlSame("expected-filtered-xml-report", "filtered-xml-historypoint")
        asssertModelsFromXmlSame("expected-failed-coverage-included-historypoint", "filtered-xml-historypoint-failed-coverage-included")

        assertPropertySet("filtered-pdf-report-exists")

    }

    private void asssertModelsFromXmlSame(String expectedXmlProp, String actualXmlProp)
            throws IOException, CloverException {
        // to check XML equal, load expected and generated into a model first.
        ProjectInfo expectedProject = loadModelFromProperty(expectedXmlProp)
        ProjectInfo actualProject = loadModelFromProperty(actualXmlProp)

        ProjectMetrics expectedMetrics = (ProjectMetrics) expectedProject.getMetrics()
        ProjectMetrics actualMetrics = (ProjectMetrics) actualProject.getMetrics()

        assertMetricsEquals(expectedMetrics, actualMetrics)

        final List<PackageInfo> expectedPackages = expectedProject.getAllPackages()
        for (PackageInfo expectedPackage : expectedPackages) {
            PackageInfo packageInfo = expectedPackage
            PackageInfo actualPkgInfo = actualProject.getNamedPackage(packageInfo.getName())
            assertMetricsEquals(packageInfo.getMetrics(), actualPkgInfo.getMetrics())

            final List<FileInfo> expectedFiles = packageInfo.getFiles()
            for (FileInfo expectedFile : expectedFiles) {
                FileInfo actualFileInfo = actualPkgInfo.getFile(expectedFile.getPackagePath())
                assertNotNull(actualFileInfo)

                final List<ClassInfo> expectedClasses = expectedFile.getClasses()
                for (ClassInfo expectedClass : expectedClasses) {
                    ClassInfo actualClassInfo = actualFileInfo.getNamedClass(expectedClass.getName())
                    assertMetricsEquals(expectedClass.getMetrics(), actualClassInfo.getMetrics())
                }
            }

        }
    }

    private ProjectInfo loadModelFromProperty(String prop) throws IOException, CloverException {
        CoverageDataPoint data = XmlConverter.getFromXmlFile(new File(getProject().getProperty(prop)), XmlConverter.LINE_LEVEL)
        return data.getProject()
    }

    private void checkPropContainsExpected(String prop) throws Exception {
        assertPropertyContains(prop, "expected-" + prop,
                new File(util.getWorkDir(), getName() + "-ERROR-actual-" + prop + ".html"))
    }

}
