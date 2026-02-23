package org.openclover.groovy.instr;

import org.codehaus.groovy.ast.MethodNode;

import java.util.Map;

/**
 * Helper configuration object which keeps result of the groovy source instrumentation.
 * Whenever any specific statement or expression was instrumented, we need to add proper helper method
 * to the instrumented class (for instance a wrapper for elvis operator).
 */
class GroovyInstrumentationResult {
    final boolean elvisExprUsed;
    final boolean fieldExprUsed;
    final boolean safeExprUsed;
    final boolean testResultsRecorded;
    final boolean isTestClass;
    final boolean isSpockSpecification;
    final boolean isParameterizedJUnit;
    final Map<String, MethodNode> safeEvalMethods;

    /**
     * @param elvisExprUsed        true if evlis expression was present in code
     * @param fieldExprUsed        true if field expression was present in code
     * @param safeExprUsed         true if save evaluation expression was present in code
     * @param testResultsRecorded  true if test results are recorded by Clover (extra instrumentation code)
     * @param safeEvalMethods      list of safeEval_X() methods to be addded to the class
     * @param isTestClass          true if it's a test class according to test detector, false otherwise
     * @param isSpockSpecification true if it's a Spock framework test class (Specification)
     * @param isParameterizedJUnit true if it's a parameterized JUnit4 test class (@Parameterized annotation)
     */
    GroovyInstrumentationResult(boolean elvisExprUsed, boolean fieldExprUsed, boolean safeExprUsed,
                                boolean testResultsRecorded,
                                final Map<String, MethodNode> safeEvalMethods,
                                boolean isTestClass, boolean isSpockSpecification, boolean isParameterizedJUnit) {
        this.elvisExprUsed = elvisExprUsed;
        this.fieldExprUsed = fieldExprUsed;
        this.safeExprUsed = safeExprUsed;
        this.testResultsRecorded = testResultsRecorded;
        this.safeEvalMethods = safeEvalMethods;
        this.isTestClass = isTestClass;
        this.isSpockSpecification = isSpockSpecification;
        this.isParameterizedJUnit = isParameterizedJUnit;
    }
}
