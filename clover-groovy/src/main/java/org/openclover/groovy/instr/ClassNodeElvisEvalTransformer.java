package org.openclover.groovy.instr;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.VariableScope;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.BooleanExpression;
import org.codehaus.groovy.ast.expr.CastExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.IfStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.openclover.runtime.CloverNames;

import java.util.function.Function;

import static groovyjarjarasm.asm.Opcodes.ACC_PUBLIC;
import static groovyjarjarasm.asm.Opcodes.ACC_STATIC;

class ClassNodeElvisEvalTransformer implements ClassNodeTransformer {

    private final Function<ClassRowColumn, Expression> newRecorderExpression;

    public ClassNodeElvisEvalTransformer(Function<ClassRowColumn, Expression> newRecorderExpression) {
        this.newRecorderExpression = newRecorderExpression;
    }

    public void transform(ClassNode classNode, GroovyInstrumentationResult flags) {
        if (flags.elvisExprUsed) {
            createEvalElvisMethods(classNode);
        }
    }

    private void createEvalElvisMethods(final ClassNode clazz) {
        addEvalElvisPrimitive(clazz, ClassHelper.byte_TYPE);
        addEvalElvisPrimitive(clazz, ClassHelper.short_TYPE);
        addEvalElvisPrimitive(clazz, ClassHelper.int_TYPE);
        addEvalElvisPrimitive(clazz, ClassHelper.long_TYPE);
        addEvalElvisPrimitive(clazz, ClassHelper.float_TYPE);
        addEvalElvisPrimitive(clazz, ClassHelper.double_TYPE);
        addEvalElvisPrimitive(clazz, ClassHelper.boolean_TYPE);
        addEvalElvisPrimitive(clazz, ClassHelper.char_TYPE);
        addEvalElvisDef(clazz);
    }

    private void addEvalElvisPrimitive(ClassNode clazz, ClassNode primitiveType) {
        //T = boolean|byte|char|short|int|long|float|double
        //T elvisEval(T expr, int index) {
        //  boolean isTrue = expr as Boolean
        //  if (isTrue) { RECORDER_CLASS.R.inc(index) } else { RECORDER_CLASS.R.inc(index + 1) }
        //  return expr
        //}

        final Parameter expr = new Parameter(primitiveType, "expr");
        final Parameter index = new Parameter(ClassHelper.int_TYPE, "index");
        final VariableScope methodScope = new VariableScope();
        final Statement methodCode = new BlockStatement(
                new Statement[]{
                        new ExpressionStatement(
                                new DeclarationExpression(
                                        new VariableExpression("isTrue", ClassHelper.Boolean_TYPE),
                                        Token.newSymbol(Types.EQUAL, -1, -1),
                                        CastExpression.asExpression(ClassHelper.Boolean_TYPE, new VariableExpression(expr)))),
                        new IfStatement(
                                new BooleanExpression(new VariableExpression("isTrue", ClassHelper.Boolean_TYPE)),
                                new BlockStatement(new Statement[]{
                                        new ExpressionStatement(
                                                new MethodCallExpression(
                                                        newRecorderExpression.apply(ClassRowColumn.of(clazz, -1, -1)),
                                                        "inc",
                                                        new ArgumentListExpression(new VariableExpression(index)))),
                                        new ReturnStatement(new VariableExpression(expr))
                                }, methodScope),
                                new BlockStatement(new Statement[]{
                                        new ExpressionStatement(
                                                new MethodCallExpression(
                                                        newRecorderExpression.apply(ClassRowColumn.of(clazz, -1, -1)),
                                                        "inc",
                                                        new ArgumentListExpression(
                                                                new BinaryExpression(
                                                                        new VariableExpression(index),
                                                                        Token.newSymbol(Types.PLUS, -1, -1),
                                                                        new ConstantExpression(1))))),
                                        new ReturnStatement(new VariableExpression(expr))
                                }, methodScope))

                },
                methodScope
        );

        final MethodNode methodNode = new MethodNode(CloverNames.namespace("elvisEval"),
                ACC_STATIC | ACC_PUBLIC,
                primitiveType,
                new Parameter[]{expr, index},
                new ClassNode[]{},
                methodCode);

        clazz.addMethod(methodNode);
    }

    private void addEvalElvisDef(ClassNode clazz) {
        //def elvisEval(def expr, Integer index) {
        //  boolean isTrue = expr as Boolean
        //  if (isTrue) { RECORDERCLASS.R.inc(index) } else { RECORDERCLASS.R.inc(index + 1) }
        //  return expr
        //}
        final Parameter expr = new Parameter(ClassHelper.DYNAMIC_TYPE, "expr");
        final Parameter index = new Parameter(ClassHelper.Integer_TYPE, "index");
        final VariableScope methodScope = new VariableScope();
        final Statement methodCode = new BlockStatement(
                new Statement[]{
                        new ExpressionStatement(
                                new DeclarationExpression(
                                        new VariableExpression("isTrue", ClassHelper.Boolean_TYPE),
                                        Token.newSymbol(Types.EQUAL, -1, -1),
                                        CastExpression.asExpression(ClassHelper.Boolean_TYPE, new VariableExpression(expr)))),
                        new IfStatement(
                                new BooleanExpression(new VariableExpression("isTrue", ClassHelper.Boolean_TYPE)),
                                new BlockStatement(new Statement[]{
                                        new ExpressionStatement(
                                                new MethodCallExpression(
                                                        newRecorderExpression.apply(ClassRowColumn.of(clazz, -1, -1)),
                                                        "inc",
                                                        new ArgumentListExpression(new VariableExpression(index)))),
                                        new ReturnStatement(new VariableExpression(expr))
                                }, methodScope),
                                new BlockStatement(new Statement[]{
                                        new ExpressionStatement(
                                                new MethodCallExpression(
                                                        newRecorderExpression.apply(ClassRowColumn.of(clazz, -1, -1)),
                                                        "inc",
                                                        new ArgumentListExpression(
                                                                new BinaryExpression(
                                                                        new VariableExpression(index),
                                                                        Token.newSymbol(Types.PLUS, -1, -1),
                                                                        new ConstantExpression(1))))),
                                        new ReturnStatement(new VariableExpression(expr))
                                }, methodScope))

                },
                methodScope
        );

        clazz.addMethod(
                CloverNames.namespace("elvisEval"), ACC_STATIC | ACC_PUBLIC,
                ClassHelper.DYNAMIC_TYPE,
                new Parameter[]{expr, index},
                new ClassNode[]{},
                methodCode);
    }
}
