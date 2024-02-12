package org.openclover.core.reporters

import org.junit.Test
import org.openclover.core.registry.metrics.BlockMetrics
import org.openclover.core.registry.metrics.ClassMetrics
import org.openclover.core.reporters.html.HtmlReporter
import org.openclover.runtime.Logger
import org.openclover.runtime.api.CloverException
import org.openclover.runtime.util.Formatting

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue
import static org.junit.Assert.fail

class ColumnTest {

    @Test
    void testAverageColumn() throws Exception {
        Columns.AvgClassesPerFile avgCol = new Columns.AvgClassesPerFile()

        try {
            avgCol.setFormat("%")
        } catch (Exception e) {
            fail("Average Column should just warn about wrong format and set to raw.")
        }

        avgCol.setFormat("raw")
        avgCol.setMin(3)

        avgCol.setValues(1.234f)
        assertEquals(Formatting.format2d(1.23f), avgCol.render())
        assertEquals("Avg Classes / File", avgCol.getTitle())
        assertEquals(Column.ALERT_STYLE, avgCol.getStyle())
        assertTrue(avgCol.getFormat() instanceof ColumnFormat.FloatColumnFormat)

        avgCol.setMin(1.2f)
        assertEquals("", avgCol.getStyle())

        avgCol.setMax(4f)
        assertEquals("", avgCol.getStyle())

        avgCol.setMin(2f)
        assertEquals(Column.ALERT_STYLE, avgCol.getStyle())
    }

    @Test
    void testExpressionColumn() throws CloverException {
        Columns.Expression col = new Columns.Expression()
        col.addText("Complexity + %CoveredElements")
        col.setTitle("Cmp + %Cov")

        BlockMetrics m = new BlockMetrics(null)
        m.setComplexity(9)
        m.setNumStatements(10)
        m.setNumCoveredStatements(3)
        
        col.init(m)
        assertEquals("39", col.render())

        col = new Columns.SUM()
        col.init(m)
        assertTrue(new Float(36.782997f).equals(col.getNumber()))

        Columns cols = new Columns()
        col = new Columns.Expression()
        col.addText("+ BadExpression")
        addAndExpectError(col, cols, "unexpected token: + in expression '+ BadExpression'")
        col.addText("")
        addAndExpectError(col, cols, "unexpected token: null in expression ''")
    }

    private void addAndExpectError(Columns.Expression col, Columns cols, String errorMessage) {
        try {
            cols.addConfiguredExpression(col)
            fail("No exception thrown for bad expression.")
        } catch (CloverException e) {
            // ignore
            assertEquals(errorMessage, e.getMessage())
        }
    }

    @Test
    void testScopedExpressionColumn() throws CloverException {

        final Logger origLogger = Logger.getInstance()
        try {
            final StringBuffer log = new StringBuffer()
            Logger.setInstance(new Logger() {
                void log(int level, String msg, Throwable t) {
                    log.append(msg).append("\n")
                }
            })
            Columns.Expression col = new Columns.Expression()
            final String expr = "NcLineCount"
            col.addText(expr)

            ClassMetrics m = new ClassMetrics(null)

            col.init(m)
            assertEquals("-", col.render())
            final String expectedLogMsg = "Expression: '" + expr + "' contains columns that are not unavailable at this scope."
            assertTrue(log.indexOf(expectedLogMsg) >= 0)
            log.delete(0, log.length() - 1)

            col.addText("Bad Expression")
            try {
                col.init(m)
                fail("No exception thrown for Bad Expression")
            } catch (CloverException e) {
                // pass
                assertTrue(log.indexOf("Invalid column name: 'Bad' in expression 'Bad Expression'") >= 0)

            }
        } finally {
            Logger.setInstance(origLogger)
        }
    }

    @Test
    void testScopedColumns() {

        BlockMetrics m = new BlockMetrics(null)
        m.setComplexity(9)

        Columns cols = new Columns()
        Columns.Complexity col = addColForScope(cols, Columns.SCOPE_METHOD)
        assertTrue(cols.getMethodColumns().contains(col))
        assertTrue(cols.getClassColumns().contains(col))
        assertTrue(cols.getPkgColumns().contains(col))
        assertTrue(cols.getProjectColumns().contains(col))

        col = addColForScope(cols, Columns.SCOPE_CLASS)
        assertFalse(cols.getMethodColumns().contains(col))
        assertTrue(cols.getClassColumns().contains(col))
        assertTrue(cols.getPkgColumns().contains(col))
        assertTrue(cols.getProjectColumns().contains(col))

        cols = new Columns()
        col = addColForScope(cols, Columns.SCOPE_PACKAGE)
        assertFalse(cols.getMethodColumns().contains(col))
        assertFalse(cols.getClassColumns().contains(col))
        assertTrue(cols.getPkgColumns().contains(col))
        assertTrue(cols.getProjectColumns().contains(col))

        col.init(m)

        assertEquals("9", col.render())

    }

    private Columns.Complexity addColForScope(Columns cols, String scopePackage) {
        Columns.Complexity col
        col = new Columns.Complexity()
        col.setScope(scopePackage)
        cols.addConfiguredComplexity(col)
        return col
    }

    @Test
    void testAverageMethodComplexity() throws Exception {
        Columns.AvgMethodComplexity avgCol = new Columns.AvgMethodComplexity()
        Columns cols = new Columns()
        cols.addConfiguredAvgMethodComplexity(avgCol)
        assertEquals(1, cols.getMethodColumns().size())

        List<Column> methodCols = cols.getMethodColumns()
        for (Column methodCol : methodCols) {
            Columns.Complexity complexity = (Columns.Complexity) methodCol
            BlockMetrics value = new BlockMetrics(null)
            value.setComplexity(42)
            complexity.init(value)
            assertEquals("42", complexity.render())
        }
    }

    @Test
    void testCoverageColumn() throws Exception {
        Columns.CoveredElements pcCol = new Columns.CoveredElements()

        pcCol.setValues(300, 0.75f)

        pcCol.setFormat("raw")
        pcCol.setMin(299)
        assertEquals("300", pcCol.render())
        assertEquals("", pcCol.getStyle())
        assertTrue(pcCol.getFormat() instanceof ColumnFormat.FloatColumnFormat)

        pcCol.setFormat("%")
        pcCol.setMin(80); // show alert if under 80% coverage
        assertEquals("75%", pcCol.render())
        assertTrue(pcCol.getFormat() instanceof ColumnFormat.PercentageColumnFormat)
        assertEquals(Column.ALERT_STYLE, pcCol.getStyle())

        pcCol.setFormat("bar")
        assertEquals(HtmlReporter.renderHtmlBarTable(0.75f, 40, ""), pcCol.render())

        pcCol.setFormat("longbar")
        assertEquals(HtmlReporter.renderHtmlBarTable(0.75f, 200, ""), pcCol.render())

    }

    @Test
    void testUncoveredColumn() throws Exception {
        BlockMetrics metrics = new BlockMetrics(null)
        metrics.setNumBranches(5)
        metrics.setNumCoveredBranches(3); // 40%, 2/5 branches covered
        metrics.setNumStatements(10)
        metrics.setNumCoveredStatements(7); // 30%, 3/10 statements covered
        // 33.3% (5/15) elements uncovered

        Columns.UncoveredElements col = new Columns.UncoveredElements()
        col = (Columns.UncoveredElements) col.copy()
        col.init(metrics)
        col.setFormat("%")
        assertEquals(Formatting.format2d(33.3f) + "%", col.render())
        col.setFormat("raw")
        assertEquals("5", col.render())

        Columns.UncoveredBranches colBr = new Columns.UncoveredBranches()
        colBr = (Columns.UncoveredBranches) colBr.copy()
        colBr.init(metrics)
        colBr.setFormat("%")
        assertEquals("40%", colBr.render())
        colBr.setFormat("raw")
        assertEquals("2", colBr.render())


        Columns.UncoveredStatements colSt = new Columns.UncoveredStatements()
        colSt = (Columns.UncoveredStatements) colSt.copy()
        colSt.init(metrics)
        colSt.setFormat("%")
        assertEquals("30%", colSt.render())
        colSt.setFormat("raw")
        assertEquals("3", colSt.render())

        ClassMetrics classMetrics = new ClassMetrics(null)
        classMetrics.setNumMethods(10)
        classMetrics.setNumCoveredMethods(2); // 80%, 8/10 uncovered
        Columns.UncoveredMethods colMe = new Columns.UncoveredMethods()
        colMe = (Columns.UncoveredMethods) colMe.copy()
        colMe.init(classMetrics)
        colMe.setFormat("%")
        assertEquals("80%", colMe.render())
        colMe.setFormat("raw")
        assertEquals("8", colMe.render())

    }
}
