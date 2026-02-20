package org.openclover.groovy.instr

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassCodeVisitorSupport
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ConstructorNode
import org.codehaus.groovy.ast.DynamicVariable
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.PropertyNode
import org.codehaus.groovy.ast.Variable
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

@CompileStatic
class PrintingVisitor extends ClassCodeVisitorSupport {

    private int depth = -1
    private NodePrinter adapter
    private Writer writer

    PrintingVisitor(NodePrinter adapter, Writer writer) {
        if (!adapter) throw new IllegalArgumentException("Null: adapter")
        this.adapter = adapter
        this.writer = writer
    }

    private <T extends ASTNode> void printNode(T node, Class expectedSubclass, Closure<Void> superMethod) {
        if (expectedSubclass.name == node.class.name) {
            depth.times { writer.print("\t") }
            writer.println(adapter.toString(node))
        }

        depth++
        superMethod.call(node)
        depth--
    }

    private <T extends Variable> void printNode(T node, Class expectedSubclass, Closure<Void> superMethod) {
        if (expectedSubclass.name == node.class.name) {
            depth.times { writer.print("\t") }
            writer.println(adapter.toString(node))
        }

        depth++
        superMethod.call(node)
        depth--
    }

    protected SourceUnit getSourceUnit() {
        return null
    }

    protected void visitStatement(Statement statement) {
        if (statement instanceof EmptyStatement) {
            visitEmptyStatement(statement as EmptyStatement)
        }
    }

    void visitClass(ClassNode node) {
        printNode(node, ClassNode, { ClassNode it ->
            super.visitClass(it)
        })
    }

    void visitConstructor(ConstructorNode node) {
        printNode(node, ConstructorNode, { ConstructorNode it ->
            super.visitConstructor(it)
        })
    }

    void visitMethod(MethodNode node) {
        printNode(node, MethodNode, { MethodNode it ->
            super.visitMethod(it)
        })
    }

    void visitField(FieldNode node) {
        printNode(node, FieldNode, { FieldNode it ->
            super.visitField(it)
        })
    }

    void visitProperty(PropertyNode node) {
        printNode(node, PropertyNode, { PropertyNode it ->
            super.visitProperty(it)
        })
    }

    void visitBlockStatement(BlockStatement node) {
        printNode(node, BlockStatement, { BlockStatement it ->
            super.visitBlockStatement(it)
        })
    }

    void visitForLoop(ForStatement node) {
        printNode(node, ForStatement, { ForStatement it ->
            super.visitForLoop(it)
        })
    }

    void visitWhileLoop(WhileStatement node) {
        printNode(node, WhileStatement, { WhileStatement it ->
            super.visitWhileLoop(it)
        })
    }

    void visitDoWhileLoop(DoWhileStatement node) {
        printNode(node, DoWhileStatement, { DoWhileStatement it ->
            super.visitDoWhileLoop(it)
        })
    }

    void visitIfElse(IfStatement node) {
        printNode(node, IfStatement, { IfStatement it ->
            super.visitIfElse(it)
        })
    }

    void visitExpressionStatement(ExpressionStatement node) {
        printNode(node, ExpressionStatement, { ExpressionStatement it ->
            super.visitExpressionStatement(it)
        })
    }

    void visitReturnStatement(ReturnStatement node) {
        printNode(node, ReturnStatement, { ReturnStatement it ->
            super.visitReturnStatement(it)
        })
    }

    void visitAssertStatement(AssertStatement node) {
        printNode(node, AssertStatement, { AssertStatement it ->
            super.visitAssertStatement(it)
        })
    }

    void visitTryCatchFinally(TryCatchStatement node) {
        printNode(node, TryCatchStatement, { TryCatchStatement it ->
            super.visitTryCatchFinally(it)
        })
    }

    void visitEmptyStatement(EmptyStatement node) {
        printNode(node, EmptyStatement, { EmptyStatement it ->
            super.visitEmptyStatement(it)
        })
    }

    void visitSwitch(SwitchStatement node) {
        printNode(node, SwitchStatement, { SwitchStatement it ->
            super.visitSwitch(it)
        })
    }

    void visitCaseStatement(CaseStatement node) {
        printNode(node, CaseStatement, { CaseStatement it ->
            super.visitCaseStatement(it)
        })
    }

    void visitBreakStatement(BreakStatement node) {
        printNode(node, BreakStatement, { BreakStatement it ->
            super.visitBreakStatement(it)
        })
    }

    void visitContinueStatement(ContinueStatement node) {
        printNode(node, ContinueStatement, { ContinueStatement it ->
            super.visitContinueStatement(it)
        })
    }

    void visitSynchronizedStatement(SynchronizedStatement node) {
        printNode(node, SynchronizedStatement, { SynchronizedStatement it ->
            super.visitSynchronizedStatement(it)
        })
    }

    void visitThrowStatement(ThrowStatement node) {
        printNode(node, ThrowStatement, { ThrowStatement it ->
            super.visitThrowStatement(it)
        })
    }

    void visitMethodCallExpression(MethodCallExpression node) {
        printNode(node, MethodCallExpression, { MethodCallExpression it ->
            super.visitMethodCallExpression(it)
        })
    }

    void visitStaticMethodCallExpression(StaticMethodCallExpression node) {
        printNode(node, StaticMethodCallExpression, { StaticMethodCallExpression it ->
            super.visitStaticMethodCallExpression(it)
        })
    }

    void visitConstructorCallExpression(ConstructorCallExpression node) {
        printNode(node, ConstructorCallExpression, { ConstructorCallExpression it ->
            super.visitConstructorCallExpression(it)
        })
    }

    void visitBinaryExpression(BinaryExpression node) {
        printNode(node, BinaryExpression, { BinaryExpression it ->
            super.visitBinaryExpression(it)
        })
    }

    void visitTernaryExpression(TernaryExpression node) {
        printNode(node, TernaryExpression, { TernaryExpression it ->
            super.visitTernaryExpression(it)
        })
    }

    void visitShortTernaryExpression(ElvisOperatorExpression node) {
        printNode(node, ElvisOperatorExpression, { ElvisOperatorExpression it ->
            super.visitShortTernaryExpression(it)
        })
    }

    void visitPostfixExpression(PostfixExpression node) {
        printNode(node, PostfixExpression, { PostfixExpression it ->
            super.visitPostfixExpression(it)
        })
    }

    void visitPrefixExpression(PrefixExpression node) {
        printNode(node, PrefixExpression, { PrefixExpression it ->
            super.visitPrefixExpression(it)
        })
    }

    void visitBooleanExpression(BooleanExpression node) {
        printNode(node, BooleanExpression, { BooleanExpression it ->
            super.visitBooleanExpression(it)
        })
    }

    void visitNotExpression(NotExpression node) {
        printNode(node, NotExpression, { NotExpression it ->
            super.visitNotExpression(it)
        })
    }

    void visitClosureExpression(ClosureExpression node) {
        printNode(node, ClosureExpression, { ClosureExpression it ->
            if (it.parameters != null) {
                for (Parameter parameter : it.parameters) {
                    visitParameter(parameter)
                }
            }
            super.visitClosureExpression(it)
        })
    }

    /**
     * Makes walking parameters look like others in the visitor.
     */
    void visitParameter(Parameter node) {
        printNode(node, Parameter, { Parameter it ->
            if (node.initialExpression) {
                node.initialExpression?.visit(this)
            }
        })
    }

    void visitTupleExpression(TupleExpression node) {
        printNode(node, TupleExpression, { TupleExpression it ->
            super.visitTupleExpression(it)
        })
    }

    void visitListExpression(ListExpression node) {
        printNode(node, ListExpression, { ListExpression it ->
            super.visitListExpression(it)
        })
    }

    void visitArrayExpression(ArrayExpression node) {
        printNode(node, ArrayExpression, { ArrayExpression it ->
            super.visitArrayExpression(it)
        })
    }

    void visitMapExpression(MapExpression node) {
        printNode(node, MapExpression, { MapExpression it ->
            super.visitMapExpression(it)
        })
    }

    void visitMapEntryExpression(MapEntryExpression node) {
        printNode(node, MapEntryExpression, { MapEntryExpression it ->
            super.visitMapEntryExpression(it)
        })
    }

    void visitRangeExpression(RangeExpression node) {
        printNode(node, RangeExpression, { RangeExpression it ->
            super.visitRangeExpression(it)
        })
    }

    void visitSpreadExpression(SpreadExpression node) {
        printNode(node, SpreadExpression, { SpreadExpression it ->
            super.visitSpreadExpression(it)
        })
    }

    void visitSpreadMapExpression(SpreadMapExpression node) {
        printNode(node, SpreadMapExpression, { SpreadMapExpression it ->
            super.visitSpreadMapExpression(it)
        })
    }

    void visitMethodPointerExpression(MethodPointerExpression node) {
        printNode(node, MethodPointerExpression, { MethodPointerExpression it ->
            super.visitMethodPointerExpression(it)
        })
    }

    void visitUnaryMinusExpression(UnaryMinusExpression node) {
        printNode(node, UnaryMinusExpression, { UnaryMinusExpression it ->
            super.visitUnaryMinusExpression(it)
        })
    }

    void visitUnaryPlusExpression(UnaryPlusExpression node) {
        printNode(node, UnaryPlusExpression, { UnaryPlusExpression it ->
            super.visitUnaryPlusExpression(it)
        })
    }

    void visitBitwiseNegationExpression(BitwiseNegationExpression node) {
        printNode(node, BitwiseNegationExpression, { BitwiseNegationExpression it ->
            super.visitBitwiseNegationExpression(it)
        })
    }

    void visitCastExpression(CastExpression node) {
        printNode(node, CastExpression, { CastExpression it ->
            super.visitCastExpression(it)
        })
    }

    void visitConstantExpression(ConstantExpression node) {
        printNode(node, ConstantExpression, { ConstantExpression it ->
            super.visitConstantExpression(it)
        })
    }

    void visitClassExpression(ClassExpression node) {
        printNode(node, ClassExpression, { ClassExpression it ->
            super.visitClassExpression(it)
        })
    }

    void visitVariableExpression(VariableExpression node) {
        printNode(node, VariableExpression, visitParameterOrDynamicVariable)
    }

    private Closure<Void> visitParameterOrDynamicVariable = { VariableExpression it ->
        if (it.accessedVariable) {
            if (it.accessedVariable instanceof Parameter) {
                visitParameter((Parameter) it.accessedVariable)
            } else if (it.accessedVariable instanceof DynamicVariable) {
                DynamicVariable dynamicVariable = (DynamicVariable) it.accessedVariable
                printNode(dynamicVariable, DynamicVariable, visitDynamicVariable)
            }
        }
    }

    private Closure<Void> visitDynamicVariable = { DynamicVariable dv ->
        dv.initialExpression?.visit(this)
    }

    void visitDeclarationExpression(DeclarationExpression node) {
        printNode(node, DeclarationExpression, { DeclarationExpression it ->
            super.visitDeclarationExpression(it)
        })
    }

    void visitPropertyExpression(PropertyExpression node) {
        printNode(node, PropertyExpression, { PropertyExpression it ->
            super.visitPropertyExpression(it)
        })
    }

    void visitAttributeExpression(AttributeExpression node) {
        printNode(node, AttributeExpression, { AttributeExpression it ->
            super.visitAttributeExpression(it)
        })
    }

    void visitFieldExpression(FieldExpression node) {
        printNode(node, FieldExpression, { FieldExpression it ->
            super.visitFieldExpression(it)
        })
    }

    void visitGStringExpression(GStringExpression node) {
        printNode(node, GStringExpression, { GStringExpression it ->
            super.visitGStringExpression(it)
        })
    }

    void visitCatchStatement(CatchStatement node) {
        printNode(node, CatchStatement, { CatchStatement it ->
            if (it.variable) visitParameter(it.variable)
            super.visitCatchStatement(it)
        })
    }

    void visitArgumentlistExpression(ArgumentListExpression node) {
        printNode(node, ArgumentListExpression, { ArgumentListExpression it ->
            super.visitArgumentlistExpression(it)
        })
    }

    void visitClosureListExpression(ClosureListExpression node) {
        printNode(node, ClosureListExpression, { ClosureListExpression it ->
            super.visitClosureListExpression(it)
        })
    }

    void visitBytecodeExpression(BytecodeExpression node) {
        printNode(node, BytecodeExpression, { BytecodeExpression it ->
            super.visitBytecodeExpression(it)
        })
    }

    void visitListOfExpressions(List<? extends Expression> list) {
        // can happen e.g. for ArrayExpression.getSizeExpression
        if (list == null) {
            return
        }
        for (Expression node : list) {
            if (node instanceof NamedArgumentListExpression) {
                printNode(node, NamedArgumentListExpression, { NamedArgumentListExpression it ->
                    it.visit(this)
                })
            } else {
                node.visit(this)
            }
        }
    }
}