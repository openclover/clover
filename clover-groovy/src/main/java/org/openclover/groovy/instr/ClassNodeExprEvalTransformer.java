package org.openclover.groovy.instr;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.VariableScope;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.openclover.runtime.CloverNames;

import java.util.function.Function;

import static groovyjarjarasm.asm.Opcodes.ACC_PUBLIC;
import static groovyjarjarasm.asm.Opcodes.ACC_STATIC;

class ClassNodeExprEvalTransformer implements ClassNodeTransformer {

    private final Function<ClassRowColumn, Expression> newRecorderExpression;

    public ClassNodeExprEvalTransformer(Function<ClassRowColumn, Expression> newRecorderExpression) {
        this.newRecorderExpression = newRecorderExpression;
    }

    @Override
    public void transform(ClassNode classNode, GroovyInstrumentationResult flags) {
        createExprEvalMethod(classNode);
    }

    private void createExprEvalMethod(final ClassNode classNode) {
        addExprEvalDef(classNode);
    }

    private void addExprEvalDef(ClassNode classNode) {
        //def exprEval(def expr, Integer index) {
        //  RECORDERCLASS.R.inc(index)
        //  return expr
        //}
        final Parameter expr = new Parameter(ClassHelper.DYNAMIC_TYPE, "expr");
        final Parameter index = new Parameter(ClassHelper.Integer_TYPE, "index");
        final VariableScope methodScope = new VariableScope();
        final Statement methodCode = new BlockStatement(
                new Statement[]{
                        new ExpressionStatement(
                                new MethodCallExpression(
                                        newRecorderExpression.apply(ClassRowColumn.of(classNode, -1, -1)),
                                        "inc",
                                        new ArgumentListExpression(new VariableExpression(index)))),
                        new ReturnStatement(new VariableExpression(expr))
                },
                methodScope);

        classNode.addMethod(
                CloverNames.namespace("exprEval"), ACC_STATIC | ACC_PUBLIC,
                ClassHelper.DYNAMIC_TYPE,
                new Parameter[]{expr, index},
                new ClassNode[]{},
                methodCode);
    }

}
