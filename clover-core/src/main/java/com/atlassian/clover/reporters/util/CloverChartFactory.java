package com.atlassian.clover.reporters.util;

import clover.org.jfree.chart.ChartFactory;
import clover.org.jfree.chart.JFreeChart;
import clover.org.jfree.chart.ChartRenderingInfo;
import clover.org.jfree.chart.ChartUtilities;
import clover.org.jfree.chart.StandardChartTheme;
import clover.org.jfree.chart.annotations.XYPointerAnnotation;
import clover.org.jfree.chart.urls.XYURLGenerator;
import clover.org.jfree.chart.labels.XYToolTipGenerator;
import clover.org.jfree.chart.labels.StandardXYToolTipGenerator;
import clover.org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import clover.org.jfree.chart.renderer.xy.XYItemRenderer;
import clover.org.jfree.chart.axis.DateAxis;
import clover.org.jfree.chart.axis.NumberAxis;
import clover.org.jfree.chart.axis.ValueAxis;
import clover.org.jfree.chart.axis.LogarithmicAxis;
import clover.org.jfree.chart.axis.NumberTickUnit;
import clover.org.jfree.chart.axis.AxisLocation;
import clover.org.jfree.chart.plot.PlotOrientation;
import clover.org.jfree.chart.plot.XYPlot;
import clover.org.jfree.data.xy.XYSeries;
import clover.org.jfree.data.xy.XYSeriesCollection;
import clover.org.jfree.data.xy.XYDataset;
import clover.org.jfree.data.xy.XYDataItem;
import clover.org.jfree.chart.renderer.xy.XYBarRenderer;
import clover.org.jfree.ui.RectangleInsets;
import com.atlassian.clover.reporters.Column;
import com.atlassian.clover.reporters.Historical;
import com.atlassian.clover.reporters.html.HtmlRenderingSupportImpl;
import com.atlassian.clover.api.registry.HasMetrics;
import com.atlassian.clover.registry.entities.BaseClassInfo;
import com.atlassian.clover.registry.entities.FullFileInfo;
import com.atlassian.clover.reporters.Columns;
import com.atlassian.clover.Logger;
import com.atlassian.clover.api.CloverException;
import com.atlassian.clover.util.Formatting;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import static org.openclover.util.Maps.newHashMap;


/**
 * Class containing factory methods for creating Charts
 * Percent bar charts, coverage charts, metrics charts, winners and losers
 * Probably needs a better name
 */
public class CloverChartFactory {

    private static final HtmlRenderingSupportImpl HTML_HELPER = new HtmlRenderingSupportImpl();
    private static final int BUCKETS = 11;
    protected static final String HISTOGRAM_NAME = "classDistrubutionChart.png";
    private static final String HISTOGRAM_TITLE = "Class Coverage Distribution";
    protected static final String SCATTER_NAME = "classComplexityChart.png";
    private static final String SCATTER_TITLE = "Class Complexity";
    private static final String SRC_FILE_CHART_NAME = "srcFileCovDistChart";
    private static final String SRC_FILE_CHART_TEXT = "of files have more coverage";
    private static final Color TRANSPARENT_BACKGROUND = new Color(255, 255, 255, 0);
    private static final Color SERIES_COLOR = new Color(59, 115, 175);    // ADG primary bright blue 1
    private static final Color SERIES_COLOR_FILL = new Color(59, 115, 175, 191); // ADG primary bright blue 1, 75% opacity
    private static final Color SERIES_HIGHLIGHT = new Color(208, 68, 55); // ADG primary red
    private static final Font AXIS_FONT = new Font("Arial", Font.PLAIN, 11);
    private static final int WIDTH_LARGE = 350;
    private static final int HEIGHT_LARGE = 250;
    private static final int WIDTH_SMALL = 120;
    private static final int HEIGHT_SMALL = 80;

    // set of colours for line charts
    private static final Color BLUE =           new Color(32, 80, 129);     // ADG primary blue
    private static final Color RED =            new Color(208, 68, 55);     // ADG primary red
    private static final Color GREEN =          new Color(20, 137, 44);     // ADG primary green
    private static final Color YELLOW =         new Color(246, 195, 66);    // ADG primary yellow
    private static final Color BRIGHT_BLUE =    new Color(59, 115, 175);    // ADG primary bright blue 1
    private static final Color ORANGE =         new Color(234, 99, 43);     // ADG secondary orange
    private static final Color LIME_GREEN =     new Color(142, 176, 33);    // ADG secondary lime green
    private static final Color VIOLET =         new Color(101, 73, 130);    // ADG secondary violet
    private static final Color TAN =            new Color(241, 162, 87);    // ADG secondary tan
    private static final Color PINK =           new Color(246, 145, 178);   // ADG secondary pink

    private static final Color[] LINE_COLOURS_XY = {
            // area and line charts use up to 6 colours
            BLUE,
            RED,
            GREEN,
            YELLOW,
            BRIGHT_BLUE,
            ORANGE,
            LIME_GREEN,
            VIOLET,
            PINK,
            TAN,
    };


    public static class ChartInfo {
        private final String name;
        private final String imageMap;
        private final int extraNum;
        private final String text;
        private final String title;

        public ChartInfo(String name, String imageMap, int extraNum, String text, String title) {
            this.name = name;
            this.imageMap = imageMap;
            this.extraNum = extraNum;
            this.text = text;
            this.title = title;
        }

        public String getName() {
            return name;
        }

        public String getImageMap() {
            return imageMap;
        }

        public int getExtraNum() {
            return extraNum;
        }

        public String getText() {
            return text;
        }

        public String getTitle() {
            return title;
        }
    }

    public static ChartInfo getChartForFile(FullFileInfo fileInfo, Map<Integer, CloverChartFactory.ChartInfo> charts) {
        double coverage = fileInfo.getMetrics().getPcCoveredElements();
        if (coverage >= 0 && (!fileInfo.isTestFile())) {
            return charts.get(CloverChartFactory.getDataIndex(fileInfo.getMetrics().getPcCoveredElements()));
        }
        return null;
    }

    public static ChartInfo generateHistogramChart(final List<? extends HasMetrics> appClasses, final File basePath)
            throws IOException {
        JFreeChart chart = createClassCoverageChart("Coverage", "# Classes", appClasses, "class", true);

        final ChartRenderingInfo renderingInfo = new ChartRenderingInfo();
        ChartUtilities.saveChartAsPNG(new File(basePath, HISTOGRAM_NAME), chart, WIDTH_LARGE, HEIGHT_LARGE, renderingInfo, true, 1);
        return new ChartInfo(HISTOGRAM_NAME, ChartUtilities.getImageMap(HISTOGRAM_NAME, renderingInfo), 0, "", HISTOGRAM_TITLE);
    }

    public static ChartInfo generateScatterChart(final List<? extends BaseClassInfo> appClasses, final File basePath)
            throws IOException {
        final JFreeChart chart = createComplexityCoverageChart("Coverage", "Complexity", appClasses, "Complexity: {2}; Coverage: {1}; Class: ");

        final ChartRenderingInfo renderingInfo = new ChartRenderingInfo();
        ChartUtilities.saveChartAsPNG(new File(basePath, SCATTER_NAME), chart, WIDTH_LARGE, HEIGHT_LARGE, renderingInfo, true, 1);
        return new ChartInfo(SCATTER_NAME, ChartUtilities.getImageMap(SCATTER_NAME, renderingInfo), 0, "", SCATTER_TITLE);
    }

    public static Map<Integer, ChartInfo> generateSrcFileCharts(final List<? extends HasMetrics> appFiles,
                                                                final File basePath) throws IOException {
        final Map<Integer, ChartInfo> chartMap = newHashMap();

        final JFreeChart chart = createClassCoverageChart("Coverage", "# Files", appFiles, "file", false);
        final XYPlot plot = chart.getXYPlot();
        plot.getDomainAxis().setVisible(false);
        plot.getRangeAxis(0).setVisible(false); // right Y axis with numbers
        plot.getRangeAxis(1).setVisible(false); // left Y axis with a label
        plot.setRangeGridlinesVisible(false);
        plot.setBackgroundPaint(TRANSPARENT_BACKGROUND);
        chart.setBackgroundPaint(TRANSPARENT_BACKGROUND);
        int totalClasses = 0;
        for (int i = 0; i < BUCKETS; i++) {
            totalClasses += (int) plot.getDataset().getYValue(0, i);
        }


        int higherPc = totalClasses;
        XYPointerAnnotation annotation = getAnnotation();
        plot.addAnnotation(annotation);
        plot.getRangeAxis().setLowerBound(-(0.15 * plot.getRangeAxis().getUpperBound()));
        annotation.setY(plot.getRangeAxis().getUpperBound() * 0.25);
        for (int i = 0; i < BUCKETS; i++) {
            annotation.setX(i * 10);

            String chartName = SRC_FILE_CHART_NAME + i + ".png";

            ChartRenderingInfo renderingInfo = new ChartRenderingInfo();
            ChartUtilities.saveChartAsPNG(new File(basePath, chartName), chart, WIDTH_SMALL, HEIGHT_SMALL, renderingInfo, true, 1);

            higherPc -= plot.getDataset().getYValue(0, i);
            final int pcPosition = totalClasses != 0 ? (int) ((double) higherPc / totalClasses * 100.0) : 0;
            ChartInfo chartInfo = new ChartInfo(chartName,
                                                ChartUtilities.getImageMap(chartName, renderingInfo),
                                                pcPosition,
                                                SRC_FILE_CHART_TEXT,
                                                "");
            chartMap.put(i, chartInfo);
        }

        return chartMap;
    }

    protected static JFreeChart createComplexityCoverageChart(final String xLabel, final String yLabel,
                                                              final List<? extends BaseClassInfo> appClasses,
                                                              final String toolTip) {
        ChartFactory.setChartTheme(StandardChartTheme.createLegacyTheme());
        XYSeriesCollection seriesCollection = new XYSeriesCollection();
        JFreeChart chart = ChartFactory.createScatterPlot("", "", "", seriesCollection, PlotOrientation.VERTICAL, false, true, false);
        chart.setBackgroundPaint(TRANSPARENT_BACKGROUND);


        XYSeries series = new XYSeries("", false, true);
        seriesCollection.addSeries(series);

        //this map contains class info for each datapoint in the series
        //but it can only store info for 1 (x,y) value - if there are two
        //datapoints with the same values, the second overwrites the first
        Map<XYDataItem, BaseClassInfo> classInfoMap = newHashMap();

        for (BaseClassInfo classInfo : appClasses) {
            int covered = (int) (classInfo.getMetrics().getPcCoveredElements() * 100);
            if (covered >= 0) {
                XYDataItem item = new XYDataItem(covered, classInfo.getMetrics().getComplexity());
                series.add(item);
                classInfoMap.put(item, classInfo);
            }
        }

        NumberAxis yAxis = getDashboardYAxis("", chart.getXYPlot().getRangeAxis().getRange().getUpperBound());
        NumberAxis xAxis = configureDashboardXAxis(xLabel, chart.getXYPlot().getDomainAxis());
        NumberAxis yAxisLabel = getDashboardYAxis(yLabel, 0);
        yAxisLabel.setTickLabelsVisible(false);

        XYPlot plot = getDashboardXYPlot(chart, xAxis, yAxis, yAxisLabel);

        XYItemRenderer renderer = plot.getRenderer();
        renderer.setSeriesShape(0, new Ellipse2D.Double(0,0,5,5));
        renderer.setBaseToolTipGenerator(getXYToolTipGeneratorComplexityCoverage(classInfoMap, toolTip));
        renderer.setURLGenerator(getXYURLGenerator(classInfoMap));
        renderer.setSeriesPaint(0, SERIES_COLOR);

        yAxis.setAutoRange(true);
        return chart;
    }

    protected static JFreeChart createClassCoverageChart(final String xLabel, final String yLabel,
                                                         final List<? extends HasMetrics> appClasses,
                                                         final String toolTip, boolean drawWithOutline) {
        ChartFactory.setChartTheme(StandardChartTheme.createLegacyTheme());
        int[] data = generateClassCoverageData(appClasses);

        XYSeriesCollection seriesCollection = new XYSeriesCollection();
        JFreeChart chart = ChartFactory.createHistogram("", "", "", seriesCollection, PlotOrientation.VERTICAL, false, true, false);
        chart.setBackgroundPaint(TRANSPARENT_BACKGROUND);

        XYSeries series = new XYSeries("");
        seriesCollection.addSeries(series);
        for (int i = 0; i < data.length; i++) {
            series.add(i * getBucketSize(), data[i]);
        }

        seriesCollection.setAutoWidth(true);
        seriesCollection.setIntervalWidth(seriesCollection.getIntervalWidth() * 0.90);

        NumberAxis xAxis = configureDashboardXAxis(xLabel, chart.getXYPlot().getDomainAxis());

        NumberAxis yAxis = getDashboardYAxis("", chart.getXYPlot().getRangeAxis().getRange().getLength());
        NumberAxis yAxisLabel = getDashboardYAxis(yLabel, 0);
        yAxisLabel.setTickLabelsVisible(false);

        XYPlot plot = getDashboardXYPlot(chart, xAxis, yAxis, yAxisLabel);
        XYItemRenderer renderer = plot.getRenderer();
        renderer.setSeriesPaint(0, SERIES_COLOR_FILL);
        renderer.setSeriesOutlinePaint(0, SERIES_COLOR);
        if (renderer instanceof XYBarRenderer) {
            ((XYBarRenderer)renderer).setDrawBarOutline(drawWithOutline);
            ((XYBarRenderer)renderer).setShadowVisible(false);
        }
        renderer.setBaseToolTipGenerator(getXYToolTipGeneratorClassCoverage(toolTip));

        return chart;
    }

    private static XYPointerAnnotation getAnnotation() {
        XYPointerAnnotation annotation = new XYPointerAnnotation("", 0, 0, Math.PI / 2.0);
        annotation.setArrowPaint(SERIES_HIGHLIGHT);
        annotation.setArrowLength(8.0);
        annotation.setArrowWidth(5.0);
        annotation.setLabelOffset(0);
        annotation.setTipRadius(16.0);
        return annotation;
    }

    /**
     * @param chartCfg the config of the chart
     * @param data a map containing Long timestamp, HasMetrics
     * @return a jfreechart
     */
    public static JFreeChart createJFreeChart(final Historical.Chart chartCfg, final Map<Long, HasMetrics> data) {
        ChartFactory.setChartTheme(StandardChartTheme.createLegacyTheme());

        ValueAxis xAxis = new DateAxis(chartCfg.getXLabel());
        final NumberAxis yAxis;
        if (chartCfg.isLogScale()) {
            yAxis = new LogarithmicAxis(chartCfg.getYLabel());
            ((LogarithmicAxis)yAxis).setAutoRangeNextLogFlag(true);
        }
        else {
            yAxis = new NumberAxis(chartCfg.getYLabel());
        }

        final XYSeriesCollection seriesCollection = new XYSeriesCollection();
        final JFreeChart chart = ChartFactory.createXYLineChart(chartCfg.getTitle(), "", "", seriesCollection, PlotOrientation.VERTICAL, true, true, false);
        final XYPlot plot = chart.getXYPlot();
        final XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer)plot.getRenderer();
        renderer.setBaseToolTipGenerator(getXYToolTipGenerator("{0}: {1}, {2}"));
        plot.setBackgroundAlpha(0);
        chart.setBackgroundPaint(Color.white);
        int j = 0;
        final Columns columns = chartCfg.getColumns();
        for (Column col : columns.getProjectColumns()) {
            // for each column, add a new series
            final XYSeries series = new XYSeries(j + ". " + col.getTitle()); // adding j to make key unique
            seriesCollection.addSeries(series);
            renderer.setSeriesItemLabelsVisible(j, true);
            renderer.setSeriesShapesVisible(j, true);
            renderer.setSeriesShapesFilled(j, true);
            renderer.setSeriesShape(j, new RoundRectangle2D.Float(-2, -2, 4, 4, 4, 4));
            renderer.setSeriesPaint(j, LINE_COLOURS_XY[j % LINE_COLOURS_XY.length]);
            renderer.setSeriesStroke(j, new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));

            j++;
            for (final Map.Entry<Long, HasMetrics> entry : data.entrySet()) {
                final HasMetrics hasMetrics = entry.getValue();
                final Long timestamp = entry.getKey();

                try {
                    col.init(hasMetrics.getMetrics());
                } catch (CloverException e) {
                    Logger.getInstance().debug("Skipping data for column: " + col.getName(), e);
                    continue;
                }
                final Number yVal = col.getNumber();
                if (chartCfg.isLogScale() && yVal.floatValue() <= 0) {
                    Logger.getInstance().debug(
                            col.getTitle() + " xVal " + timestamp + " yVal = " + yVal +
                                    ". Skipping this datapoint.");
                    continue;
                }
                series.add(timestamp, yVal);
            }
        }

        plot.setDomainAxis(xAxis);
        plot.setRangeAxis(yAxis);
        yAxis.setTickLabelsVisible(true);

        if (chartCfg.isAutoRange()) {
            yAxis.setAutoRange(true);
            yAxis.setAutoRangeIncludesZero(false);
        }
        else {
            if (!chartCfg.isLogScale()) {
                yAxis.setLowerBound(0);
            }
            if (chartCfg.getUpperBound() >= 0 ) {
                yAxis.setUpperBound(chartCfg.getUpperBound());
            }
        }

        return chart;
    }

    private static XYURLGenerator getXYURLGenerator(final Map<XYDataItem, BaseClassInfo> classInfoMap) {
        return (dataset, series, item) -> {
            XYDataItem key = new XYDataItem(dataset.getX(series, item), dataset.getY(series, item));
            BaseClassInfo classInfo = classInfoMap.get(key);
            return new String(HTML_HELPER.getSrcFileLink(true, true, classInfo));
        };
    }

    private static XYToolTipGenerator getXYToolTipGeneratorComplexityCoverage(
            final Map<XYDataItem, BaseClassInfo> classInfoMap,
            final String format) {
         return new StandardXYToolTipGenerator(format, Formatting.getPcFormat(), NumberFormat.getInstance()) {
            @Override
            public String generateToolTip(XYDataset dataset, int series, int item) {
                String toolTip = super.generateToolTip(dataset, series, item);
                XYDataItem key = new XYDataItem(dataset.getX(series, item), dataset.getY(series, item));
                BaseClassInfo classInfo = classInfoMap.get(key);
                return toolTip + classInfo.getName();
            }
        };
    }

    public static XYToolTipGenerator getXYToolTipGenerator(String format) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return new StandardXYToolTipGenerator(format, dateFormat, NumberFormat.getInstance());
    }

    private static XYToolTipGenerator getXYToolTipGeneratorClassCoverage(final String format) {
        return new StandardXYToolTipGenerator(format, NumberFormat.getInstance(), NumberFormat.getInstance()) {
            @Override
            public String generateToolTip(XYDataset dataset, int series, int item) {
                int xVal = dataset.getX(series, item).intValue();
                int yVal = dataset.getY(series, item).intValue();
                
                final String yValStr = Formatting.pluralizedVal(yVal, format) + " " + (yVal != 1 ? "have" : "has");
                final String xValStr;
                if (xVal == 0) {
                    xValStr = xVal +"% coverage";
                }
                else {
                    //get previous bucket value + 1
                    int prevXVal = xVal - (int) getBucketSize() + 1;
                    xValStr = prevXVal + "-" + xVal +"% coverage";
                }

                return yValStr + " " + xValStr;
            }
        };
    }

    private static XYPlot getDashboardXYPlot(JFreeChart chart, NumberAxis xAxis, NumberAxis yAxis, NumberAxis yAxisLabel) {
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(TRANSPARENT_BACKGROUND);
        plot.setDomainGridlinesVisible(false);
        plot.setDomainZeroBaselineVisible(false);
        plot.setOutlineVisible(false);

        plot.setDomainAxis(0, xAxis);
        plot.setRangeAxis(0, yAxis);
        plot.setRangeAxis(1, yAxisLabel);
        plot.setRangeAxisLocation(0, AxisLocation.BOTTOM_OR_RIGHT);

        return plot;
    }

    //return a NumberAxis with formatting
    private static NumberAxis getDashboardYAxis(String label, double maxValue) {
        NumberAxis yAxis;
        yAxis = new NumberAxis(label);

        yAxis.setLabelFont(AXIS_FONT);

        yAxis.setAutoRangeIncludesZero(true);
        yAxis.setAxisLineVisible(false);
        yAxis.setTickLabelsVisible(true);
        yAxis.setTickMarksVisible(false);

        yAxis.setLowerBound(0);
        yAxis.setAutoRange(true);
        yAxis.setTickUnit(new NumberTickUnit(getDashboardYAxisTickUnit(maxValue)));

        return yAxis;
    }

    public static int getDashboardYAxisTickUnit(double upperBound) {
        double goal = upperBound / 2.5;
        double tickValue = 1;
        double returnValue = 1;

        while (tickValue <= goal) {
            returnValue = tickValue;

            tickValue *= 2.5;
            if (tickValue <= goal) {
                returnValue = tickValue;
            }

            tickValue *= 2;
            if (tickValue <= goal) {
                returnValue = tickValue;
            }

            tickValue *= 2;
            if (tickValue <= goal) {
                returnValue = tickValue;
            }
        }

        return (int) returnValue;
    }

    //return a NumberAxis with formatting
    private static NumberAxis configureDashboardXAxis(String label, ValueAxis domainAxis) {

        NumberAxis xAxis = (NumberAxis) domainAxis;
        if (xAxis == null) {
            xAxis = new NumberAxis();
        }
        
        xAxis.setLabel(label);
        xAxis.setLabelFont(AXIS_FONT);

        xAxis.setAutoRange(false);
        xAxis.setLowerBound(-9);
        xAxis.setUpperBound(109);

        xAxis.setAxisLineVisible(false);

        xAxis.setAutoRangeIncludesZero(true);
        xAxis.setAutoTickUnitSelection(false);
        xAxis.setTickUnit(new NumberTickUnit(10, Formatting.getPcFormat()), true, true);
        xAxis.setTickMarksVisible(true);

        xAxis.setLabelInsets(new RectangleInsets(10, 0, 0, 0));

        return xAxis;
    }

    //generates buckets for histogram
    //protected so it can be tested
    protected static int[] generateClassCoverageData(final List<? extends HasMetrics> hasMetrics) {
        final int[] data = new int[BUCKETS];

        for (final HasMetrics clss : hasMetrics) {
            double covered = clss.getMetrics().getPcCoveredElements();
            int index = getDataIndex(covered);
            if (index >= 0) {
                data[index]++;
            }
        }
        return data;
    }

    /**
     * @param covered percentage 0.0-1.0
     * @return assigned bucket 0 if covered = 0, otherwise equally spaced from 1 to buckets-1
     */
    public static int getDataIndex(double covered) {
        if (covered < 0 || covered > 1.0) {
            return -1;
        }
        if (covered == 0) {
            return 0;
        }
        int coveredInt = (int) (covered * 100);
        return ((int) Math.floor((coveredInt - 1) / getBucketSize())) + 1;
    }

    /**
     * @return the number of percentage points between each bucket
     */
    public static double getBucketSize() {
        return 100.0 / (BUCKETS - 1);
    }



}
