package com.atlassian.clover.cfg.instr

import com.atlassian.clover.api.CloverException
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

import static org.hamcrest.CoreMatchers.containsString
import static org.junit.Assert.assertEquals
import static org.junit.Assert.fail

/**
 * Test for MethodContextDef.
 */
class MethodContextDefTest {

    @Rule
    public ExpectedException throwsRule = ExpectedException.none()

    /**
     * Check default values.
     */
    @Test
    void testDefaultValues() {
        MethodContextDef contextDef = new MethodContextDef()
        assertEquals(Integer.MAX_VALUE, contextDef.getMaxComplexity())
        assertEquals(Integer.MAX_VALUE, contextDef.getMaxStatements())
    }

    /**
     * Check validation for correct inputs.
     */
    @Test
    void testValidationOK()  {
        MethodContextDef contextDef = new MethodContextDef()
        contextDef.setName("mycontext")
        contextDef.setMaxComplexity(10)
        contextDef.setMaxStatements(20)
        contextDef.setMaxAggregatedComplexity(30)
        contextDef.setMaxAggregatedStatements(40)
        try {
            contextDef.validate()
        } catch (CloverException ex) {
            fail(ex.toString())
        }
    }

    /**
     * Check validation for bad inputs.
     */
    @Test
    void testValidationFailMaxComplexity() throws CloverException {
        throwsRule.expect(CloverException.class)
        throwsRule.expectMessage(containsString("maxComplexity must be"))

        MethodContextDef contextDef = new MethodContextDef()
        contextDef.setName("mycontext")
        contextDef.setMaxComplexity(-10)
        contextDef.validate()
    }

    @Test
    void testValidationFailMaxStatements() throws CloverException {
        throwsRule.expect(CloverException.class)
        throwsRule.expectMessage(containsString("maxStatements must be"))

        MethodContextDef contextDef = new MethodContextDef()
        contextDef.setName("mycontext")
        contextDef.setMaxStatements(-20)
        contextDef.validate()
    }

    @Test
    void testValidationFailMaxAggregatedComplexity() throws CloverException {
        throwsRule.expect(CloverException.class)
        throwsRule.expectMessage(containsString("maxAggregatedComplexity must be"))

        MethodContextDef contextDef = new MethodContextDef()
        contextDef.setName("mycontext")
        contextDef.setMaxAggregatedComplexity(-30)
        contextDef.validate()
    }

    @Test
    void testValidationFailMaxAggregatedStatements() throws CloverException {
        throwsRule.expect(CloverException.class)
        throwsRule.expectMessage(containsString("maxAggregatedStatements must be"))

        MethodContextDef contextDef = new MethodContextDef()
        contextDef.setName("mycontext")
        contextDef.setMaxAggregatedStatements(-40)
        contextDef.validate()
    }
}
