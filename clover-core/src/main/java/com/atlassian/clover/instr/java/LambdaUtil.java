package com.atlassian.clover.instr.java;

import com.atlassian.clover.registry.entities.Parameter;

/**
 * Utility class for handling lambdas
 */
public class LambdaUtil {

    public static final String LAMBDA_PREFIX = "$lam_";

    /**
     * Build a friendly name for lambda function based on their parameters:
     *
     * <pre>
     *   (int x, double f) ->  $lambda_x_f
     *   ()                ->  $lambda
     *   (a, b, c)         ->  $lambda_a_b_c
     * </pre>
     *
     * @param parameters lambda parameters
     * @return String friendly name for lambda
     */
    public static String generateLambdaName(Parameter[] parameters) {
        final StringBuilder name = new StringBuilder(LAMBDA_PREFIX);
        for (final Parameter parameter : parameters) {
            // cut implicit types (Parameter.INFERRED), replace non-alphanumeric characters by underscore
            final String paramName = parameter.getName()
                    .replace(Parameter.INFERRED, "").replaceAll("[^a-zA-Z0-9_$]", "_");
            // separate parameters by underscore
            name.append(paramName).append('_');
        }
        // remove duplicated underscores and the last underscore at the end
        return name.toString().replaceAll("_+", "_").replaceAll("_$", "");
    }

    /**
     * Build a friendly name for lambda with extra identifier number (it can be a line number,
     * Nth lambda in file etc)
     *
     * @param parameters lambda parameters
     * @param id identifier
     * @return String friendly name with id
     */
    public static String generateLambdaNameWithId(Parameter[] parameters, int id) {
        return generateLambdaName(parameters) + "#" + id;
    }

}
