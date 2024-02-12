package org.openclover.core.reporters

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.openclover.core.registry.metrics.BlockMetrics

import static org.junit.Assert.assertEquals
import static org.junit.Assert.fail

class ColumnELTest {
    BlockMetrics m

    @Rule
    public TestName testName = new TestName()

    @Before
    void setUp() throws Exception {
        m = new BlockMetrics(null)
        m.setComplexity(9)
        m.setNumStatements(10)
        m.setNumCoveredStatements(3)
    }

    @Test
    void testSimpleExpressions() throws Exception {
        assertExprEquals(1, "1")
        assertExprEquals(1 + 1, "1 + 1")
        assertExprEquals(1 * 2, "1 * 2")
        assertExprEquals(1f / 2, "1 / 2")
        assertExprEquals(Math.pow(2, 3), "2 ^ 3")
        assertExprEquals(1 - 1, "1 - 1")
        assertExprEquals(1 * 2 + (3 + 4), "1 * 2 + (3 + 4)")
        assertExprEquals((1 + 2) * 3, "(1 + 2) * 3")
        assertExprEquals(Math.pow((1 + 2) * 3, 4), "((1 + 2) * 3) ^ 4")
    }

    @Test
    void testBadExpressions() throws Exception {
        assertExprEquals(Double.POSITIVE_INFINITY, "1/0")
        assertBadExpr("&", "unexpected char: '&' in expression '&'")
        assertBadExpr("1--0", "unexpected token: - in expression '1--0'")
        assertBadExpr("covered + 1", "Invalid column name: 'covered' in expression 'covered + 1'")
    }

    @Test
    void testValidationOfInvalidExpressions() throws Exception {
        assertValidateExprYieldsException("&", "unexpected char: '&' in expression '&'")
        assertValidateExprYieldsException("1--0", "unexpected token: - in expression '1--0'")
        assertValidateExprYieldsException("covered + 1", "Invalid column name: 'covered' in expression 'covered + 1'")
    }

    @Test
    void testColumnExpressions() throws Exception {
        assertExprEquals(m.getComplexity() + 1, "complexity + 1")

        assertExprEquals(m.getPcCoveredElements() * 100 as float, "%CoveredElements")

        final double pcCoveredEle = Columns.getColumnValue("coveredElements", "%", m)
        assertExprEquals(m.getComplexity() + pcCoveredEle as double, "complexity + %coveredElements")

        assertExprEquals(Math.pow(1 - pcCoveredEle/100 as double, 3),
                        "(1 - %coveredElements/100)^3")


        assertExprEquals(Math.pow(m.getComplexity(), 2) * Math.pow(1 - pcCoveredEle/100 as double, 3),
                        "complexity^2 * ((1 - %coveredElements/100)^3)")

        // A RISK ALGORITHM - Crap4j
        assertExprEquals(
                        Math.pow(m.getComplexity(), 2) * Math.pow(1 - pcCoveredEle/100 as double, 3) + m.getComplexity(),
                        "complexity^2 * ((1 - %coveredElements/100)^3) + complexity")
    }

    private void assertExprEquals(double expected, String expression) throws Exception {
        double r = evalExpr(expression)
        assertEquals(expected, r, 0)
    }

    private void assertBadExpr(String expression, String expectedMessage) {
        try {
            evalExpr(expression)
            fail("No exception thrown for bad expression")
        } catch (Exception e) {
            assertEquals(expectedMessage, e.getMessage())
        }
    }

    private void assertValidateExprYieldsException(String expression, String expectedMessage) {
        try {
            ExpressionEvaluator.validate(expression, testName.methodName)
            fail("No exception thrown when validating invalid expression")
        } catch (Exception e) {
            assertEquals(expectedMessage, e.getMessage())
        }
    }

    private double evalExpr(String expression) throws Exception {
       return ExpressionEvaluator.eval(expression, m, testName.methodName)
    }
}
