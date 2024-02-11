package org.openclover.core.reporters.util

import clover.org.jfree.chart.ChartFrame
import clover.org.jfree.chart.ChartPanel
import clover.org.jfree.chart.JFreeChart
import clover.org.jfree.chart.axis.DateAxis
import clover.org.jfree.chart.axis.LogarithmicAxis
import clover.org.jfree.chart.axis.NumberAxis
import clover.org.jfree.chart.plot.XYPlot
import clover.org.jfree.data.xy.XYDataset
import org.openclover.core.api.registry.BlockMetrics
import org.openclover.core.api.registry.HasMetrics
import org.openclover.core.registry.FixedSourceRegion
import org.openclover.core.registry.entities.BaseClassInfo
import org.openclover.core.registry.entities.BaseFileInfo
import org.openclover.core.registry.entities.BasePackageInfo
import org.openclover.core.registry.entities.Modifiers
import org.openclover.core.registry.metrics.ProjectMetrics
import org.openclover.core.reporters.Columns
import org.openclover.core.reporters.Historical
import org.openclover.core.registry.metrics.HasMetricsTestFixture
import org.openclover.buildutil.testutils.IOHelper
import org.openclover.core.util.CloverUtils
import junit.framework.TestCase

import javax.swing.JFrame
import java.awt.Window

import static org.openclover.core.util.Lists.newArrayList
import static org.openclover.core.util.Maps.newHashMap

class CloverChartFactoryTest extends TestCase {

    static class MockHasMetrics implements HasMetrics {
        private  BlockMetrics metrics

        MockHasMetrics(BlockMetrics metrics) {
            this.metrics = metrics
        }

        String getName() {
            return null
        }

        BlockMetrics getMetrics() {
            return metrics
        }

        BlockMetrics getRawMetrics() {
            return metrics
        }

        void setMetrics(BlockMetrics metrics) {
            this.metrics = metrics
        }
    }

    void testGetDataIndex() throws IOException {
        assertEquals(-1, CloverChartFactory.getDataIndex(-0.10))
        assertEquals(-1, CloverChartFactory.getDataIndex(1.10))
        assertEquals(0, CloverChartFactory.getDataIndex(0.00))
        assertEquals(1, CloverChartFactory.getDataIndex(0.10))
        assertEquals(2, CloverChartFactory.getDataIndex(0.111))
        assertEquals(10, CloverChartFactory.getDataIndex(1.00))
        assertEquals(10, CloverChartFactory.getDataIndex(0.91))
        assertEquals(9, CloverChartFactory.getDataIndex(0.90))
    }

    void testCreateScatterPlot() throws IOException, InterruptedException {
        List<BaseClassInfo> data =  getTestMetrics()

        assertEquals(6, data.size())

        JFreeChart chart = CloverChartFactory.createComplexityCoverageChart("X Axis", "Y Axis", data, "{0} : {1} : {2}")

        XYPlot plot = chart.getXYPlot()
        assertEquals(1, plot.getDatasetCount())

        assertTrue(plot.getRangeAxis() instanceof NumberAxis)
        assertTrue(plot.getDomainAxis() instanceof NumberAxis)
        assertEquals(plot.getRangeAxisCount(), 2)
        assertEquals(plot.getDomainAxisCount(), 1)
        assertEquals(6, plot.getDataset().getItemCount(0))

    }

    void openChart(JFreeChart chart) throws InterruptedException {
        final ChartPanel panel = new ChartPanel(chart)
        panel.setVisible(true)

        JFrame frame = new JFrame()
        frame.add(panel)
        frame.pack()
        frame.setVisible(true)
        Thread.sleep(15000)
    }

    void testGenerateReportCharts() throws IOException {
        List<BaseClassInfo> data = getTestMetrics()
        File baseImgPath = new File(IOHelper.createTmpDir(getName()), "img")
        CloverUtils.createDir(baseImgPath)
        CloverChartFactory.ChartInfo histogram = CloverChartFactory.generateHistogramChart(data, baseImgPath)
        CloverChartFactory.ChartInfo scatter = CloverChartFactory.generateScatterChart(data, baseImgPath)
        Map srcFileCharts = CloverChartFactory.generateSrcFileCharts(data, baseImgPath)

        assertEquals(CloverChartFactory.HISTOGRAM_NAME, histogram.getName())
        assertEquals(CloverChartFactory.SCATTER_NAME, scatter.getName())
        assertExtraNumEquals(83, srcFileCharts, 0.0)
        assertExtraNumEquals(50, srcFileCharts, 0.1)
        assertExtraNumEquals(33, srcFileCharts, 0.5)
        assertExtraNumEquals(16, srcFileCharts, 0.9)
        assertExtraNumEquals(0, srcFileCharts, 1.0)
    }

    void assertExtraNumEquals(int expected, Map srcFileCharts, double covered) {
        assertEquals(expected, ((CloverChartFactory.ChartInfo) srcFileCharts.get(new Integer(CloverChartFactory.getDataIndex(covered)))).getExtraNum())
    }

    void testCreateHistogram() throws IOException, InterruptedException {

        List<BaseClassInfo> data = getTestMetrics()

        JFreeChart chart = CloverChartFactory.createClassCoverageChart("Coverage", "# Classes", data, "class", true)

        XYPlot plot = chart.getXYPlot()
        assertEquals(1, plot.getDatasetCount())

        assertTrue(plot.getRangeAxis() instanceof NumberAxis)
        assertTrue(plot.getDomainAxis() instanceof NumberAxis)
        assertEquals(2, plot.getRangeAxisCount())
        assertEquals(1, plot.getDomainAxisCount())
        assertEquals(11, plot.getDataset().getItemCount(0))

    }

    void testGenerateClassCoverageData() throws IOException {
        List<BaseClassInfo> hasMetricsList = getTestMetrics()
        int buckets = 11

        int[] data = CloverChartFactory.generateClassCoverageData(hasMetricsList)
        assertEquals(buckets, data.length)
        assertEquals(1, data[0])
        assertEquals(2, data[1])
        assertEquals(0, data[2])
        assertEquals(1, data[7])
        assertEquals(0, data[9])
        assertEquals(1, data[10])
    }

    private BaseClassInfo createClassInfo(String name, int nCovSts, int nSts, int cmplx) throws IOException {
        HasMetricsTestFixture fixture = new HasMetricsTestFixture("Test")
        BasePackageInfo pkgInfo = new BasePackageInfo(null, "PackageName")
        BaseFileInfo fileInfo = new BaseFileInfo(pkgInfo, "FileName.extension", null, 0, 0, 0, 0, 0)

        ProjectMetrics metrics = new ProjectMetrics(fixture.getProject())
        metrics.setNumCoveredStatements(nCovSts)
        metrics.setNumStatements(nSts)
        metrics.setComplexity(cmplx)

        BaseClassInfo classInfo = new BaseClassInfo(pkgInfo, fileInfo,
                name, new FixedSourceRegion(0,0,0,0), new Modifiers(),
                false, false, false)
        classInfo.setMetrics(metrics)
        return classInfo
    }

    private List<BaseClassInfo> getTestMetrics() throws IOException {

        java.util.List<BaseClassInfo> data = newArrayList()
        data.add(createClassInfo("blah1", 7, 10, 2))
        data.add(createClassInfo("blah2", 4, 10, 1))
        data.add(createClassInfo("blah3", 1, 10, 0))
        data.add(createClassInfo("blah4", 1, 10, 10))
        data.add(createClassInfo("blah5", 10, 10, 7))
        data.add(createClassInfo("blah6", 0, 10, 7))

        return data
    }

    void testCoverageChart() throws IOException, InterruptedException {
        HasMetricsTestFixture fixture = new HasMetricsTestFixture(this.getName())

        ProjectMetrics metrics = new ProjectMetrics(fixture.getProject())
        metrics.setComplexity(7)
        metrics.setLineCount(1)
        metrics.setNumCoveredStatements(7)
        metrics.setNumStatements(10)

        ProjectMetrics metrics2 = new ProjectMetrics(fixture.getProject())
        metrics2.setComplexity(9)
        metrics2.setLineCount(50)
        metrics2.setNumCoveredStatements(4)
        metrics2.setNumStatements(10)


        ProjectMetrics metrics3 = new ProjectMetrics(fixture.getProject())
        metrics3.setComplexity(8)
        metrics3.setLineCount(25)
        metrics3.setNumCoveredStatements(1)
        metrics3.setNumStatements(10)


        Map<Long, HasMetrics> data = newHashMap()
        data.put(new Long(0), new MockHasMetrics(metrics))
        data.put(new Long(1000), new MockHasMetrics(metrics2))
        data.put(new Long(2000), new MockHasMetrics(metrics3))

        Columns columns = new Columns()
        columns.addConfiguredComplexity(new Columns.Complexity())
        columns.addConfiguredLineCount(new Columns.LineCount())
        columns.addConfiguredCoveredStatements(new Columns.CoveredStatements())

        Historical.Chart chartCfg = new Historical.Chart()
        chartCfg.setTitle("Testing Chart")
        chartCfg.setXLabel("Date")
        chartCfg.setYLabel("Stats")
        chartCfg.setLogScale(true)
        chartCfg.setUpperBound(-1)
        chartCfg.addColumns(columns)
        chartCfg.setAutoRange(true)

        JFreeChart chart = CloverChartFactory.createJFreeChart(chartCfg, data)

//        showChart(chart)

        XYPlot plot = chart.getXYPlot()
        assertEquals("Testing Chart", chart.getTitle().getText())
        assertEquals(1, plot.getDatasetCount())
        ProjectMetrics[] allMetrics = [ metrics, metrics2, metrics3 ]
        final XYDataset dataset = plot.getDataset(0)

        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            assertSeries(allMetrics, dataset, dataset.getSeriesKey(i), i)
        }

        assertTrue(plot.getRangeAxis() instanceof LogarithmicAxis)
        assertTrue(plot.getRangeAxis().isAutoRange())
        assertTrue(plot.getDomainAxis() instanceof DateAxis)


    }

    private void showChart(JFreeChart chart) throws InterruptedException {
        ChartFrame frame = new ChartFrame("test", chart)
        Window window = new Window(frame)
        frame.setVisible(true)
        frame.pack()
        window.setVisible(true)
        Thread.sleep(5000)
    }

    private void assertSeries(ProjectMetrics[] allMetrics, XYDataset dataset, Comparable seriesKey, int index) {
        for (int i = 0; i < allMetrics.length; i++) {
            ProjectMetrics metric = allMetrics[i]
            if ("Complexity".equalsIgnoreCase(seriesKey.toString())) {
                assertEquals(metric.getComplexity(), dataset.getY(index, i).intValue())
            } else if ("Lines".equalsIgnoreCase(seriesKey.toString())){
                assertEquals(metric.getLineCount(), dataset.getY(index, i).intValue())
            }
        }
    }
}
