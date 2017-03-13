package com.atlassian.clover.reporters.json;

import com.atlassian.clover.reporters.CloverReportConfig;
import com.atlassian.clover.reporters.Column;
import com.atlassian.clover.reporters.Columns;
import com.atlassian.clover.reporters.html.HtmlRenderingSupportImpl;
import com.atlassian.clover.api.registry.BlockMetrics;
import com.atlassian.clover.api.registry.HasMetrics;
import com.atlassian.clover.api.CloverException;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class JSONReportUtils {
    public static Map<String, Number> collectColumnValuesFor(
        List columns, HasMetrics mInfo, HtmlRenderingSupportImpl mHelper) throws CloverException {
        final Map<String, Number> columnValues = mHelper.collectColumnValues(columns, mInfo);
        final BlockMetrics metrics = mHelper.metricsFor(mInfo);
        //TODO: add as official columns?
        columnValues.put("Tests", new Integer(metrics.getNumTests()));
        columnValues.put("PassingTests", new Integer(metrics.getNumTestPasses()));
        columnValues.put("FailingTests", new Integer(metrics.getNumTestFailures()));
        columnValues.put("ErroneousTests", new Integer(metrics.getNumTestErrors()));
        columnValues.put("TestExecutionTime", new Double(metrics.getTestExecutionTime()));
        columnValues.put("PcPassingTests", new Float(metrics.getPcTestPasses()));
        columnValues.put("PcFailingTests", new Float(metrics.getPcTestFailures()));
        columnValues.put("PcErroneousTests", new Float(metrics.getPcTestErrors()));
        return columnValues;
    }

    public static List getColumnNames(CloverReportConfig cfg) {
        List<Column> columns =
            cfg.isColumnsSet()
                ? cfg.getColumns().getProjectColumnsCopy()
                : Columns.getAllColumns();
        List<String> columnNames = new ArrayList<String>(columns.size() + 5);
        for (Column column : columns) {
            columnNames.add(column.getName());
        }
        //TODO: add as official columns?
        columnNames.add("Tests");
        columnNames.add("PassingTests");
        columnNames.add("FailingTests");
        columnNames.add("ErroneousTests");
        columnNames.add("TestExecutionTime");
        columnNames.add("PcPassingTests");
        columnNames.add("PcFailingTests");
        columnNames.add("PcErroneousTests");
        return columnNames;
    }
}
