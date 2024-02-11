package com.atlassian.clover.reporters.json;

import clover.org.apache.velocity.VelocityContext;

import java.util.Map;
import java.util.List;
import java.util.Date;
import java.io.File;
import java.text.SimpleDateFormat;

import org.openclover.runtime.Logger;
import org.openclover.runtime.api.CloverException;
import com.atlassian.clover.api.registry.HasMetrics;
import com.atlassian.clover.reporters.html.HtmlReportUtil;
import com.atlassian.clover.reporters.Columns;
import com.atlassian.clover.reporters.Column;

import static org.openclover.util.Lists.newLinkedList;
import static org.openclover.util.Maps.newHashMap;

/**
 */
public class JSONHistoricalReporter {

    private final File mBasePath;

    public JSONHistoricalReporter(File basePath) {
        this.mBasePath = basePath;
    }

    /**
     * Generates historical JSON data using a subset of the
     * <a href="http://code.google.com/apis/visualization/documentation/dev/implementing_data_source.html#jsondatatable">Google Visualisation DataSource response format</a>.
     * <p/>
     * Since Clover reports are only static, this will only contain the 'table' object, and does not contain the surrounding status etc.
     * These extra fields could be added by a service which serves this file.
     * <p/>
     * The basic format of the JSON produced is:
     * <pre>
     * table: {
     *      cols: [{id: 'timestamp', label: 'Date', type: 'date'}, ...],
     *      rows: [{c:[{v: new Date(0), f: '1 January 1970 00:00'}, ...]}]
     * }
     * </pre>
     * <p/>
     * This data may be used directly with the
     * <a href="http://code.google.com/apis/visualization/documentation/reference.html">google.visualization javascript Library.</a>
     * ie - the above JSON object
     * may be passed directly to a <a href="http://code.google.com/apis/visualization/documentation/reference.html#DataTable">google.visualization.DataTable</a>
     * constructor like so:
     * new google.visualization.DataTable(json.table[0], 0.5);
     * <p/>
     * e.g.
     * <pre>
     *    var data = new google.visualization.DataTable(json.table[0], 0.5);
     *    var chart = new google.visualization.AreaChart(document.getElementById('chart_div'));
     *    chart.draw(data, {width: 800, height: 400, legend: 'bottom', title: 'Clover Historical Chart'});
     *</pre>
     *
     * @param context the velocity context to use when rendering
     * @param data the map of historical data points
     * @throws Exception if an error occurs while writing the JSON
     */
    public void generateHistoricalJSON(VelocityContext context, Map<Long, HasMetrics> data, String title) throws Exception {
        final JSONObject json = generateJSON(data, title);
        final File jsonOutfile = new File(mBasePath, "historical-json.js");
        context.put("json", json.toString(2));
        context.put("callback", "processHistoricalCloverData");
        Logger.getInstance().info("Writing JSON historical-data to: " + jsonOutfile);
        HtmlReportUtil.mergeTemplateToFile(jsonOutfile, context, "api-json.vm");
    }

    /** Package protected for testing. */
    /*protected*/ JSONObject generateJSON(Map<Long, HasMetrics> data, String title) throws JSONException {
        final JSONObject json = new JSONObject();

        final Map<String, List> table = newHashMap();
        final List<Map<String, String>> cols = newLinkedList();
        final List<Map<String, List<Map<String, Object>>>> rows = newLinkedList();
        table.put("cols", cols);
        table.put("rows", rows);
        json.put("name", title); // configured via <historical title="foo" .../>
        json.append("table", table);

        // add a timestamp column
        addColumnInfo(cols, "timestamp", "Date", "date");
        final SimpleDateFormat dateFormat = new SimpleDateFormat();

        final List<Column> columns = Columns.getAllColumns();
        for (final Column column : columns) {
            addColumnInfo(cols, column.getName(), column.getTitle(), "number");
        }

        for (final Map.Entry<Long, HasMetrics> entry : data.entrySet()) {
            final HasMetrics hasMetrics = entry.getValue();
            final Long timestamp = entry.getKey();

            final Map<String, List<Map<String, Object>>> rowData = newHashMap();
            rows.add(rowData);
            final List<Map<String, Object>> row = newLinkedList();
            rowData.put("c", row);

            // add timestamp data
            addRowInfo(row, new Date(timestamp), dateFormat.format(timestamp));
            addColumnData(columns, hasMetrics, row);
        }
        return json;
    }

    private void addColumnData(List<Column> columns, HasMetrics hasMetrics, List<Map<String, Object>> row) {
        for (Column column : columns) {
            try {
                column.init(hasMetrics.getMetrics());
            } catch (CloverException e) {
                Logger.getInstance().debug("Skipping data for column: " + column.getName(), e);
                continue;
            }
            addRowInfo(row, column.getNumber(), column.getFormat().format(column.getColumnData()));
        }
    }

    private void addRowInfo(List<Map<String, Object>> row, Object value, String fmtValue) {
        final Map<String, Object> rowInfo = newHashMap();
        rowInfo.put("v", value);
        rowInfo.put("f", fmtValue);
        row.add(rowInfo);
    }

    private void addColumnInfo(List<Map<String, String>> cols, String id, String label, String type) {
        final Map<String, String> colInfo = newHashMap();
        colInfo.put("id", id);
        colInfo.put("label", label);
        colInfo.put("type", type);
        cols.add(colInfo);
    }
}
