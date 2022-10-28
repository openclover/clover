package com.atlassian.clover.instr.groovy

import org.codehaus.groovy.ast.ClassCodeVisitorSupport
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ConstructorNode
import org.codehaus.groovy.ast.DynamicVariable
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.PropertyNode
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ArrayExpression
import org.codehaus.groovy.ast.expr.AttributeExpression
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.BitwiseNegationExpression
import org.codehaus.groovy.ast.expr.BooleanExpression
import org.codehaus.groovy.ast.expr.CastExpression
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.expr.ClosureListExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.ConstructorCallExpression
import org.codehaus.groovy.ast.expr.DeclarationExpression
import org.codehaus.groovy.ast.expr.ElvisOperatorExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.FieldExpression
import org.codehaus.groovy.ast.expr.GStringExpression
import org.codehaus.groovy.ast.expr.ListExpression
import org.codehaus.groovy.ast.expr.MapEntryExpression
import org.codehaus.groovy.ast.expr.MapExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.MethodPointerExpression
import org.codehaus.groovy.ast.expr.NamedArgumentListExpression
import org.codehaus.groovy.ast.expr.NotExpression
import org.codehaus.groovy.ast.expr.PostfixExpression
import org.codehaus.groovy.ast.expr.PrefixExpression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.ast.expr.RangeExpression
import org.codehaus.groovy.ast.expr.SpreadExpression
import org.codehaus.groovy.ast.expr.SpreadMapExpression
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression
import org.codehaus.groovy.ast.expr.TernaryExpression
import org.codehaus.groovy.ast.expr.TupleExpression
import org.codehaus.groovy.ast.expr.UnaryMinusExpression
import org.codehaus.groovy.ast.expr.UnaryPlusExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.AssertStatement
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.BreakStatement
import org.codehaus.groovy.ast.stmt.CaseStatement
import org.codehaus.groovy.ast.stmt.CatchStatement
import org.codehaus.groovy.ast.stmt.ContinueStatement
import org.codehaus.groovy.ast.stmt.DoWhileStatement
import org.codehaus.groovy.ast.stmt.EmptyStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.ForStatement
import org.codehaus.groovy.ast.stmt.IfStatement
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.ast.stmt.SwitchStatement
import org.codehaus.groovy.ast.stmt.SynchronizedStatement
import org.codehaus.groovy.ast.stmt.ThrowStatement
import org.codehaus.groovy.ast.stmt.TryCatchStatement
import org.codehaus.groovy.ast.stmt.WhileStatement
import org.codehaus.groovy.classgen.BytecodeExpression
import org.codehaus.groovy.control.SourceUnit

class PrintingVisitor extends ClassCodeVisitorSupport {

    private int depth = -1
    private adapter
    private writer

    PrintingVisitor(adapter, writer) {
        if (!adapter) throw new IllegalArgumentException("Null: adapter")
        this.adapter = adapter;
        this.writer = writer
    }

    private void printNode(node, Class expectedSubclass, Closure superMethod) {
        if (expectedSubclass.name.equals(node.class.name)) {
            depth.times { writer.print("\t") }
            writer.println(adapter.toString(node))
        }

        depth++
        superMethod.call(node)
        depth--
    }

    protected SourceUnit getSourceUnit() {
        return null;
    }

    protected void visitStatement(Statement statement) {
        if (statement instanceof EmptyStatement) {
            visitEmptyStatement(statement as EmptyStatement)
        }
    }

    void visitClass(ClassNode node) {
        printNode(node, ClassNode, { super.visitClass(it) });
    }

    void visitConstructor(ConstructorNode node) {
        printNode(node, ConstructorNode, { super.visitConstructor(it) });
    }

    void visitMethod(MethodNode node) {
        printNode(node, MethodNode, { super.visitMethod(it) });
    }

    void visitField(FieldNode node) {
        printNode(node, FieldNode, { super.visitField(it) });
    }

    void visitProperty(PropertyNode node) {
        printNode(node, PropertyNode, { super.visitProperty(it) });
    }

    void visitBlockStatement(BlockStatement node) {
        printNode(node, BlockStatement, { super.visitBlockStatement(it) });
    }

    void visitForLoop(ForStatement node) {
        printNode(node, ForStatement, { super.visitForLoop(it) });
    }

    void visitWhileLoop(WhileStatement node) {
        printNode(node, WhileStatement, { super.visitWhileLoop(it) });
    }

    void visitDoWhileLoop(DoWhileStatement node) {
        printNode(node, DoWhileStatement, { super.visitDoWhileLoop(it) });
    }

    void visitIfElse(IfStatement node) {
        printNode(node, IfStatement, { super.visitIfElse(it) });
    }

    void visitExpressionStatement(ExpressionStatement node) {
        printNode(node, ExpressionStatement, { super.visitExpressionStatement(it) });
    }

    void visitReturnStatement(ReturnStatement node) {
        printNode(node, ReturnStatement, { super.visitReturnStatement(it) });
    }

    void visitAssertStatement(AssertStatement node) {
        printNode(node, AssertStatement, { super.visitAssertStatement(it) });
    }

    void visitTryCatchFinally(TryCatchStatement node) {
        printNode(node, TryCatchStatement, { super.visitTryCatchFinally(it) });
    }

    protected void visitEmptyStatement(EmptyStatement node) {
        printNode(node, EmptyStatement, {});
    }

    void visitSwitch(SwitchStatement node) {
        printNode(node, SwitchStatement, { super.visitSwitch(it) });
    }

    void visitCaseStatement(CaseStatement node) {
        printNode(node, CaseStatement, { super.visitCaseStatement(it) });
    }

    void visitBreakStatement(BreakStatement node) {
        printNode(node, BreakStatement, { super.visitBreakStatement(it) });
    }

    void visitContinueStatement(ContinueStatement node) {
        printNode(node, ContinueStatement, { super.visitContinueStatement(it) });
    }

    void visitSynchronizedStatement(SynchronizedStatement node) {
        printNode(node, SynchronizedStatement, { super.visitSynchronizedStatement(it) });
    }

    void visitThrowStatement(ThrowStatement node) {
        printNode(node, ThrowStatement, { super.visitThrowStatement(it) });
    }

    void visitMethodCallExpression(MethodCallExpression node) {
        printNode(node, MethodCallExpression, { super.visitMethodCallExpression(it) });
    }

    void visitStaticMethodCallExpression(StaticMethodCallExpression node) {
        printNode(node, StaticMethodCallExpression, { super.visitStaticMethodCallExpression(it) });
    }

    void visitConstructorCallExpression(ConstructorCallExpression node) {
        printNode(node, ConstructorCallExpression, { super.visitConstructorCallExpression(it) });
    }

    void visitBinaryExpression(BinaryExpression node) {
        printNode(node, BinaryExpression, { super.visitBinaryExpression(it) });
    }

    void visitTernaryExpression(TernaryExpression node) {
        printNode(node, TernaryExpression, { super.visitTernaryExpression(it) });
    }

    void visitShortTernaryExpression(ElvisOperatorExpression node) {
        printNode(node, ElvisOperatorExpression, { super.visitShortTernaryExpression(it) });
    }

    void visitPostfixExpression(PostfixExpression node) {
        printNode(node, PostfixExpression, { super.visitPostfixExpression(it) });
    }

    void visitPrefixExpression(PrefixExpression node) {
        printNode(node, PrefixExpression, { super.visitPrefixExpression(it) });
    }

    void visitBooleanExpression(BooleanExpression node) {
        printNode(node, BooleanExpression, { super.visitBooleanExpression(it) });
    }

    void visitNotExpression(NotExpression node) {
        printNode(node, NotExpression, { super.visitNotExpression(it) });
    }

    void visitClosureExpression(ClosureExpression node) {
        printNode(node, ClosureExpression, {
            it.parameters?.each { parameter -> visitParameter(parameter) }
            super.visitClosureExpression(it)
        });
    }

    /**
     * Makes walking parameters look like others in the visitor.
     */
    void visitParameter(Parameter node) {
        printNode(node, Parameter, {
            if (node.initialExpression) {
                node.initialExpression?.visit(this)
            }
        });
    }

    void visitTupleExpression(TupleExpression node) {
        printNode(node, TupleExpression, { super.visitTupleExpression(it) });
    }

    void visitListExpression(ListExpression node) {
        printNode(node, ListExpression, { super.visitListExpression(it) });
    }

    void visitArrayExpression(ArrayExpression node) {
        printNode(node, ArrayExpression, { super.visitArrayExpression(it) });
    }

    void visitMapExpression(MapExpression node) {
        printNode(node, MapExpression, { super.visitMapExpression(it) });
    }

    void visitMapEntryExpression(MapEntryExpression node) {
        printNode(node, MapEntryExpression, { super.visitMapEntryExpression(it) });
    }

    void visitRangeExpression(RangeExpression node) {
        printNode(node, RangeExpression, { super.visitRangeExpression(it) });
    }

    void visitSpreadExpression(SpreadExpression node) {
        printNode(node, SpreadExpression, { super.visitSpreadExpression(it) });
    }

    void visitSpreadMapExpression(SpreadMapExpression node) {
        printNode(node, SpreadMapExpression, { super.visitSpreadMapExpression(it) });
    }

    void visitMethodPointerExpression(MethodPointerExpression node) {
        printNode(node, MethodPointerExpression, { super.visitMethodPointerExpression(it) });
    }

    void visitUnaryMinusExpression(UnaryMinusExpression node) {
        printNode(node, UnaryMinusExpression, { super.visitUnaryMinusExpression(it) });
    }

    void visitUnaryPlusExpression(UnaryPlusExpression node) {
        printNode(node, UnaryPlusExpression, { super.visitUnaryPlusExpression(it) });
    }

    void visitBitwiseNegationExpression(BitwiseNegationExpression node) {
        printNode(node, BitwiseNegationExpression, { super.visitBitwiseNegationExpression(it) });
    }

    void visitCastExpression(CastExpression node) {
        printNode(node, CastExpression, { super.visitCastExpression(it) });
    }

    void visitConstantExpression(ConstantExpression node) {
        printNode(node, ConstantExpression, { super.visitConstantExpression(it) });
    }

    void visitClassExpression(ClassExpression node) {
        printNode(node, ClassExpression, { super.visitClassExpression(it) });
    }

    void visitVariableExpression(VariableExpression node) {
        printNode(node, VariableExpression, { VariableExpression it ->
            if (it.accessedVariable) {
                if (it.accessedVariable instanceof Parameter) {
                    visitParameter((Parameter) it.accessedVariable)
                } else if (it.accessedVariable instanceof DynamicVariable) {
                    printNode(it.accessedVariable, DynamicVariable, { it.initialExpression?.visit(this) });
                }
            }
        });
    }

    void visitDeclarationExpression(DeclarationExpression node) {
        printNode(node, DeclarationExpression, { super.visitDeclarationExpression(it) });
    }

    void visitPropertyExpression(PropertyExpression node) {
        printNode(node, PropertyExpression, { super.visitPropertyExpression(it) });
    }

    void visitAttributeExpression(AttributeExpression node) {
        printNode(node, AttributeExpression, { super.visitAttributeExpression(it) });
    }

    void visitFieldExpression(FieldExpression node) {
        printNode(node, FieldExpression, { super.visitFieldExpression(it) });
    }

    void visitGStringExpression(GStringExpression node) {
        printNode(node, GStringExpression, { super.visitGStringExpression(it) });
    }

    void visitCatchStatement(CatchStatement node) {
        printNode(node, CatchStatement, {
            if (it.variable) visitParameter(it.variable)
            super.visitCatchStatement(it)
        });
    }

    void visitArgumentlistExpression(ArgumentListExpression node) {
        printNode(node, ArgumentListExpression, { super.visitArgumentlistExpression(it) });
    }

    void visitClosureListExpression(ClosureListExpression node) {
        printNode(node, ClosureListExpression, { super.visitClosureListExpression(it) });
    }

    void visitBytecodeExpression(BytecodeExpression node) {
        printNode(node, BytecodeExpression, { super.visitBytecodeExpression(it) });
    }

    protected void visitListOfExpressions(List list) {
        list.each { Expression node ->
            if (node instanceof NamedArgumentListExpression) {
                printNode(node, NamedArgumentListExpression, { it.visit(this) });
            } else {
                node.visit(this)
            }
        }
    }
}