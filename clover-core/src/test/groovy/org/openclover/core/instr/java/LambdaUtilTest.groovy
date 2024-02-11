package org.openclover.core.instr.java

import org.openclover.core.registry.entities.Parameter
import org.junit.Test

import static org.junit.Assert.assertEquals

/**
 * Test for {@link LambdaUtil}
 */
public class LambdaUtilTest {
    @Test
    public void testGenerateLambdaName() {
        // no-argument lambda
        assertEquals('$lam', LambdaUtil.generateLambdaName(new Parameter[0]))

        // few primitive types
        assertEquals('$lam_x_f', LambdaUtil.generateLambdaName(
                [ new Parameter("int", "x"), new Parameter("double", "f") ] as Parameter[])
        )

        // few implicit types
        assertEquals('$lam_a_b_c', LambdaUtil.generateLambdaName(
                [
                        new Parameter(Parameter.INFERRED, "a"),
                        new Parameter(Parameter.INFERRED, "b"),
                        new Parameter(Parameter.INFERRED, "c") ] as Parameter[])
        )

        // few generic types
        assertEquals('$lam_input', LambdaUtil.generateLambdaName(
                [ new Parameter("List<String>", "input") ] as Parameter[])
        )
    }

    @Test
    public void testGenerateLambdaNameWithId() {
        // few primitive types plus id = 100
        assertEquals('$lam_x_f#100', LambdaUtil.generateLambdaNameWithId(
                [ new Parameter("int", "x"), new Parameter("double", "f") ] as Parameter[], 100)
        )
    }

}
