package com.atlassian.clover.instr.groovy;

import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.expr.AttributeExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.ElvisOperatorExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.TernaryExpression;
import org.codehaus.groovy.control.SourceUnit;

public class ExpressionComplexityCounter extends ClassCodeVisitorSupport {
    private int complexity;

    public static int count(Expression exp) {
        final ExpressionComplexityCounter counter = new ExpressionComplexityCounter();
        exp.visit(counter);
        return counter.complexity;
    }

    @Override
    public void visitBinaryExpression(BinaryExpression expression) {
        super.visitBinaryExpression(expression);
        complexity++;
    }

    @Override
    public void visitMethodCallExpression(MethodCallExpression call) {
        super.visitMethodCallExpression(call);
        if (call.isSafe()) {
            complexity++;
        }
    }

    @Override
    public void visitAttributeExpression(AttributeExpression expression) {
        super.visitAttributeExpression(expression);
        if (expression.isSafe()) {
            complexity++;
        }
    }

    @Override
    public void visitPropertyExpression(PropertyExpression expression) {
        super.visitPropertyExpression(expression);
        if (expression.isSafe()) {
            complexity++;
        }
    }

    /** Handles both ternary and elvis operators */
    @Override
    public void visitTernaryExpression(TernaryExpression expression) {
        //We don't visit the true expression of an elvis as it's the same as the boolean expression - avoid double counting
        if (expression instanceof ElvisOperatorExpression) {
            expression.getBooleanExpression().visit(this);
            expression.getFalseExpression().visit(this);
        } else {
            expression.getBooleanExpression().visit(this);
            expression.getTrueExpression().visit(this);
            expression.getFalseExpression().visit(this);
        }

        complexity++;
    }

    @Override
    protected SourceUnit getSourceUnit() {
        return null;
    }
}

