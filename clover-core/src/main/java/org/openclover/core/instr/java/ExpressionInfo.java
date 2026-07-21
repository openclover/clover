package org.openclover.core.instr.java;

/**
 * A class for processing expressions and gathering information about them
 */
public class ExpressionInfo {

    private boolean constant = false;
    private boolean containsAssign = false;
    private boolean containsInstanceOfWithPattern = false;
    private int complexity = 0;

    public static ExpressionInfo fromTokens(CloverToken start, CloverToken end) {
        ExpressionInfo info = new ExpressionInfo();

        info.constant = scanForConstant(start, end);

        if (!info.constant) {
            // detector of "a = some_value" in the expression
            AssignmentDetector assignmentDetector = new AssignmentDetector();
            // detector of "o instanceof A a" and "o instanceof A(...)" cases, which cannot be instrumented
            InstanceOfStateDetector instanceOfState = new InstanceOfStateDetector();
            // any expression reaching here represents a "branch"
            ExpressionComplexityCounter complexityCounter = new ExpressionComplexityCounter(1);
            // counts opening "(" and closing ")"
            ParenthesisCounter parenthesisCounter = new ParenthesisCounter(1); // start with one open

            CloverToken curr = start;
            while (curr != null  && parenthesisCounter.notLastParenthesis() && curr != end) {
                assignmentDetector.accept(curr);
                instanceOfState.accept(curr);
                complexityCounter.accept(curr);
                parenthesisCounter.accept(curr);
                curr = curr.getNext();
            }

            info.containsAssign = assignmentDetector.containsAssign();
            info.complexity = complexityCounter.getComplexity();
            info.containsInstanceOfWithPattern = instanceOfState.hasPatternBinding();
        }
        return info;
    }

    /**
     * Detect if expression contains constants only. It won't be instrumented in such case.
     */
    private static boolean scanForConstant(CloverToken start, CloverToken end) {
        ConstantExpressionDetector detector = new ConstantExpressionDetector();
        for (CloverToken t = start; t.getNext() != end.getNext(); t=t.getNext()) {
            detector.accept(t);
        }
        return detector.isConstant();
    }

    public boolean isConstant() {
        return constant;
    }

    public boolean isInstrumentable() {
        return !constant && !containsAssign && !containsInstanceOfWithPattern;
    }

    public int getComplexity() {
        return complexity;
    }

}
