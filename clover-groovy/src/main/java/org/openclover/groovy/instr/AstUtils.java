package org.openclover.groovy.instr;

import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.FieldExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;

public class AstUtils {
    public static Statement sysOutPrintln(Expression expression) {
        return new ExpressionStatement(sysOutPrintlnExpr(expression));
    }

    public static MethodCallExpression sysOutPrintlnExpr(Expression expression) {
        try {
            return new MethodCallExpression(
                new FieldExpression(FieldNode.newStatic(System.class, "out")),
                "println",
                new ArgumentListExpression(expression));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
}
