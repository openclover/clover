package com.atlassian.clover.reporters.json

import com.atlassian.clover.TestUtils
import junit.framework.TestCase
import com.atlassian.clover.reporters.Columns
import com.atlassian.clover.registry.metrics.HasMetricsTestFixture

import static org.openclover.util.Maps.newHashMap

class JSONHistoricalReporterTest extends TestCase {


    void testGenerateJSON() throws Exception {
        final File outFile = File.createTempFile(getName(), ".json", TestUtils.createEmptyDirFor(getClass(), getName()))

        JSONHistoricalReporter reporter = new JSONHistoricalReporter(outFile)
        Map data = newHashMap() // Long, HasMetrics

        HasMetricsTestFixture fixture = new HasMetricsTestFixture(getName())
        data.put(new Long(0), fixture.getProject())
        final String name = "Project Name"
        final JSONObject json = reporter.generateJSON(data, name)
        assertNotNull(json)
        assertEquals(name, json.get("name"))
        final JSONArray tableArray = (JSONArray) json.get("table")
        final Map table = (Map) tableArray.get(0)
        assertTrue(table instanceof Map)
        final List rows = (List) table.get("rows")
        final List cols = (List) table.get("cols")
        assertEquals(1, rows.size()); // exactly one data row
        // expect allColumns.size + 1 due to the extra column for the timestamp
        assertEquals(Columns.getAllColumns().size() + 1, cols.size())
    }


}
