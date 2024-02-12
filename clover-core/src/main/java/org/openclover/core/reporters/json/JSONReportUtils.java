package org.openclover.core.reporters.json;

import org.openclover.core.api.registry.BlockMetrics;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.reporters.CloverReportConfig;
import org.openclover.core.reporters.Column;
import org.openclover.core.reporters.Columns;
import org.openclover.core.reporters.html.HtmlRenderingSupportImpl;
import org.openclover.runtime.api.CloverException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JSONReportUtils {
    public static Map<String, Number> collectColumnValuesFor(
            List columns, HasMetrics mInfo, HtmlRenderingSupportImpl mHelper) throws CloverException {
        final Map<String, Number> columnValues = mHelper.collectColumnValues(columns, mInfo);
        final BlockMetrics metrics = mHelper.metricsFor(mInfo);
        //TODO: add as official columns?
        columnValues.put("Tests", metrics.getNumTests());
        columnValues.put("PassingTests", metrics.getNumTestPasses());
        columnValues.put("FailingTests", metrics.getNumTestFailures());
        columnValues.put("ErroneousTests", metrics.getNumTestErrors());
        columnValues.put("TestExecutionTime", metrics.getTestExecutionTime());
        columnValues.put("PcPassingTests", metrics.getPcTestPasses());
        columnValues.put("PcFailingTests", metrics.getPcTestFailures());
        columnValues.put("PcErroneousTests", metrics.getPcTestErrors());
        return columnValues;
    }

    public static List getColumnNames(CloverReportConfig cfg) {
        List<Column> columns =
            cfg.isColumnsSet()
                ? cfg.getColumns().getProjectColumnsCopy()
                : Columns.getAllColumns();
        List<String> columnNames = new ArrayList<>(columns.size() + 5);
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
