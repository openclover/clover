package org.openclover.groovy.instr;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.VariableScope;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.BooleanExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.TernaryExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ForStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.openclover.runtime.CloverNames;
import org.openclover.runtime.recorder.PerTestRecorder;

import static groovyjarjarasm.asm.Opcodes.ACC_PUBLIC;
import static groovyjarjarasm.asm.Opcodes.ACC_STATIC;

class ClassNodeEvalTestExceptionTransformer implements ClassNodeTransformer {
    @Override
    public void transform(ClassNode classNode, GroovyInstrumentationResult flags) {
        createEvalTestExceptionMethod(classNode, flags);
    }

    private void createEvalTestExceptionMethod(final ClassNode clazz, final GroovyInstrumentationResult flags) {
        if (flags.testResultsRecorded) {
            addEvalTestException(clazz);
        }
    }

    private void addEvalTestException(ClassNode clazz) {
        //int evalTestException(Throwable exception, def expected) {
        //  boolean isExpected = false
        //  for (ex in expected) {
        //      isExpected = isExpected || (exception != null && ex.isAssignableFrom(exception.getClass()))
        //  }
        //  return (isExpected || (exception == null && expected.isEmpty())) ? $PerTestRecorder.NORMAL_EXIT$ : $PerTestRecorder.ABNORMAL_EXIT$
        //}
        Parameter exceptionParam = new Parameter(ClassHelper.make(Throwable.class), "exception");
        Parameter expectedParam = new Parameter(ClassHelper.DYNAMIC_TYPE, "expected");
        VariableScope methodScope = new VariableScope();

        VariableExpression isExpectedVar = new VariableExpression("isExpected", ClassHelper.Boolean_TYPE);
        Parameter exVariable = new Parameter(ClassHelper.make(Class.class), "ex");

        clazz.addMethod(
                CloverNames.namespace("evalTestException"), ACC_STATIC | ACC_PUBLIC,
                ClassHelper.int_TYPE,
                new Parameter[]{exceptionParam, expectedParam},
                new ClassNode[]{},
                new BlockStatement(
                        new Statement[]{
                                new ExpressionStatement(
                                        new DeclarationExpression(
                                                isExpectedVar,
                                                Token.newSymbol(Types.EQUAL, -1, -1),
                                                ConstantExpression.FALSE
                                        )),
                                new ForStatement(
                                        exVariable,
                                        new VariableExpression(expectedParam),
                                        new BlockStatement(
                                                new Statement[]{
                                                        new ExpressionStatement(
                                                                new BinaryExpression(
                                                                        isExpectedVar,
                                                                        Token.newSymbol(Types.EQUAL, -1, -1),
                                                                        new BinaryExpression(
                                                                                isExpectedVar,
                                                                                Token.newSymbol(Types.LOGICAL_OR, -1, -1),
                                                                                new BinaryExpression(
                                                                                        new BinaryExpression(
                                                                                                new VariableExpression(exceptionParam),
                                                                                                Token.newSymbol(Types.COMPARE_NOT_EQUAL, -1, -1),
                                                                                                ConstantExpression.NULL),
                                                                                        Token.newSymbol(Types.LOGICAL_AND, -1, -1),
                                                                                        new MethodCallExpression(
                                                                                                new VariableExpression(exVariable),
                                                                                                "isAssignableFrom",
                                                                                                new MethodCallExpression(
                                                                                                        new VariableExpression(exceptionParam),
                                                                                                        "getClass",
                                                                                                        ArgumentListExpression.EMPTY_ARGUMENTS))))))
                                                },
                                                methodScope
                                        )),
                                new ReturnStatement(
                                        new TernaryExpression(
                                                new BooleanExpression(
                                                        new BinaryExpression(
                                                                isExpectedVar,
                                                                Token.newSymbol(Types.LOGICAL_OR, -1, -1),
                                                                new BinaryExpression(
                                                                        new BinaryExpression(
                                                                                new VariableExpression(exceptionParam),
                                                                                Token.newSymbol(Types.COMPARE_EQUAL, -1, -1),
                                                                                ConstantExpression.NULL),
                                                                        Token.newSymbol(Types.LOGICAL_AND, -1, -1),
                                                                        new MethodCallExpression(
                                                                                new VariableExpression(expectedParam),
                                                                                "isEmpty",
                                                                                ArgumentListExpression.EMPTY_ARGUMENTS)))),
                                                new ConstantExpression(PerTestRecorder.NORMAL_EXIT),
                                                new ConstantExpression(PerTestRecorder.ABNORMAL_EXIT)
                                        ))
                        },
                        methodScope));
    }
}
