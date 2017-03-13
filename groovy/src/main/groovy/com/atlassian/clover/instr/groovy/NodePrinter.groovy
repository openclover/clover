package com.atlassian.clover.instr.groovy

import org.codehaus.groovy.ast.ClassCodeVisitorSupport
import org.codehaus.groovy.ast.DynamicVariable
import org.codehaus.groovy.ast.ModuleNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.classgen.BytecodeExpression
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.*
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ConstructorNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.PropertyNode
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.ast.ASTNode

public class NodePrinter {
    static Map NODE_CONFIG = [
            "org.codehaus.groovy.ast.ClassNode"                      : {
                "ClassNode - $it.name"
            },
            "org.codehaus.groovy.ast.InnerClassNode"                 : {
                "InnerClassNode - $it.name"
            },
            "org.codehaus.groovy.ast.ConstructorNode"                : {
                "ConstructorNode - $it.name (${it.parameters.type.join(', ')})"
            },
            "org.codehaus.groovy.ast.MethodNode"                     : {
                "MethodNode - $it.name (${it.parameters.type.join(', ')}) : $it.returnType - synthetic=$it.synthetic"
            },
            "org.codehaus.groovy.ast.FieldNode"                      : {
                "FieldNode - $it.name : $it.type"
            },
            "org.codehaus.groovy.ast.PropertyNode"                   : {
                "PropertyNode - ${it.field?.name} : ${it.field?.type}"
            },
            "org.codehaus.groovy.ast.AnnotationNode"                 : {
                "AnnotationNode - ${it.classNode?.name}"
            },
            "org.codehaus.groovy.ast.Parameter"                      : {
                "Parameter - $it.name"
            },
            "org.codehaus.groovy.ast.DynamicVariable"                : {
                "DynamicVariable - $it.name"
            },
            "org.codehaus.groovy.ast.stmt.BlockStatement"            : {
                "BlockStatement - (${it.statements ? it.statements.size() : 0})"
            },
            "org.codehaus.groovy.ast.stmt.ExpressionStatement"       : {
                "ExpressionStatement - ${it?.expression?.getClass()?.simpleName}"
            },
            "org.codehaus.groovy.ast.stmt.ReturnStatement"           : {
                "ReturnStatement - $it.text"
            },
            "org.codehaus.groovy.ast.stmt.TryCatchStatement"         : {
                "TryCatchStatement - ${it.catchStatements?.size ?: 0} catch, ${it.finallyStatement ? 1 : 0} finally"
            },
            "org.codehaus.groovy.ast.stmt.CatchStatement"            : {
                "CatchStatement - $it.exceptionType"
            },
            "org.codehaus.groovy.ast.expr.ConstructorCallExpression" : {
                "ConstructorCall - $it.text"
            },
            "org.codehaus.groovy.ast.expr.SpreadExpression"          : {
                "Spread - $it.text"
            },
            "org.codehaus.groovy.ast.expr.ArgumentListExpression"    : {
                "ArgumentList - $it.text"
            },
            "org.codehaus.groovy.ast.expr.MethodCallExpression"      : {
                "MethodCall - $it.text (implicitThis=${it.implicitThis}, " +
                        "receiver=${it.hasProperty('receiver') ? it.receiver : ''}, " + // receiver not present in Groovy 1.x
                        "safe={$it.safe}, spreadSafe=${it.spreadSafe})"
            },
            "org.codehaus.groovy.ast.expr.StaticMethodCallExpression": {
                "StaticMethodCall - $it.text (" +
                        "receiver=${it.hasProperty('receiver') ? it.receiver : ''}, " +  // receiver not present in Groovy 1.x
                        "ownerType=${it.ownerType})"
            },
            "org.codehaus.groovy.ast.expr.GStringExpression"         : {
                "GString - $it.text"
            },
            "org.codehaus.groovy.ast.expr.AttributeExpression"       : {
                "Attribute - $it.text"
            },
            "org.codehaus.groovy.ast.expr.DeclarationExpression"     : {
                "Declaration - $it.text"
            },
            "org.codehaus.groovy.ast.expr.VariableExpression"        : {
                "Variable - $it.name : $it.type"
            },
            "org.codehaus.groovy.ast.expr.ConstantExpression"        : {
                "Constant - $it.value : $it.type"
            },
            "org.codehaus.groovy.ast.expr.BinaryExpression"          : {
                "Binary - $it.text"
            },
            "org.codehaus.groovy.ast.expr.ClassExpression"           : {
                "Class - $it.text"
            },
            "org.codehaus.groovy.ast.expr.BooleanExpression"         : {
                "Boolean - $it.text"
            },
            "org.codehaus.groovy.ast.expr.ArrayExpression"           : {
                "Array - $it.text"
            },
            "org.codehaus.groovy.ast.expr.ListExpression"            : {
                "List - $it.text"
            },
            "org.codehaus.groovy.ast.expr.TupleExpression"           : {
                "Tuple - $it.text"
            },
            "org.codehaus.groovy.ast.expr.FieldExpression"           : {
                "Field - $it.text"
            },
            "org.codehaus.groovy.ast.expr.PropertyExpression"        : {
                "Property - $it.propertyAsString"
            },
            "org.codehaus.groovy.ast.expr.NotExpression"             : {
                "Not - $it.text"
            },
            "org.codehaus.groovy.ast.expr.CastExpression"            : {
                "Cast - $it.text"
            },
    ]

    public void print(ModuleNode module, out) {
        new PrintingVisitor(this, out).with { printer -> module?.classes?.each { clazz -> printer.visitClass(clazz) } }
    }

    private String toString(node) {
        String nodeName = node.class.name
        String nodeText
        if (NODE_CONFIG[nodeName]) {
            nodeText = NODE_CONFIG[nodeName].call(node)
        } else {
            nodeText = node.class.simpleName
        }
        if (node instanceof ASTNode) {
            "[${nodeText} - {$node.lineNumber,$node.columnNumber,$node.lastLineNumber,$node.lastColumnNumber}]"
        } else {
            "[${nodeText}]"
        }
    }
}

public class PrintingVisitor extends ClassCodeVisitorSupport {

    private int depth = -1
    private adapter
    private writer

    private PrintingVisitor(adapter, writer) {
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

    public void visitClass(ClassNode node) {
        printNode(node, ClassNode, { super.visitClass(it) });
    }

    public void visitConstructor(ConstructorNode node) {
        printNode(node, ConstructorNode, { super.visitConstructor(it) });
    }

    public void visitMethod(MethodNode node) {
        printNode(node, MethodNode, { super.visitMethod(it) });
    }

    public void visitField(FieldNode node) {
        printNode(node, FieldNode, { super.visitField(it) });
    }

    public void visitProperty(PropertyNode node) {
        printNode(node, PropertyNode, { super.visitProperty(it) });
    }

    public void visitBlockStatement(BlockStatement node) {
        printNode(node, BlockStatement, { super.visitBlockStatement(it) });
    }

    public void visitForLoop(ForStatement node) {
        printNode(node, ForStatement, { super.visitForLoop(it) });
    }

    public void visitWhileLoop(WhileStatement node) {
        printNode(node, WhileStatement, { super.visitWhileLoop(it) });
    }

    public void visitDoWhileLoop(DoWhileStatement node) {
        printNode(node, DoWhileStatement, { super.visitDoWhileLoop(it) });
    }

    public void visitIfElse(IfStatement node) {
        printNode(node, IfStatement, { super.visitIfElse(it) });
    }

    public void visitExpressionStatement(ExpressionStatement node) {
        printNode(node, ExpressionStatement, { super.visitExpressionStatement(it) });
    }

    public void visitReturnStatement(ReturnStatement node) {
        printNode(node, ReturnStatement, { super.visitReturnStatement(it) });
    }

    public void visitAssertStatement(AssertStatement node) {
        printNode(node, AssertStatement, { super.visitAssertStatement(it) });
    }

    public void visitTryCatchFinally(TryCatchStatement node) {
        printNode(node, TryCatchStatement, { super.visitTryCatchFinally(it) });
    }

    protected void visitEmptyStatement(EmptyStatement node) {
        printNode(node, EmptyStatement, {});
    }

    public void visitSwitch(SwitchStatement node) {
        printNode(node, SwitchStatement, { super.visitSwitch(it) });
    }

    public void visitCaseStatement(CaseStatement node) {
        printNode(node, CaseStatement, { super.visitCaseStatement(it) });
    }

    public void visitBreakStatement(BreakStatement node) {
        printNode(node, BreakStatement, { super.visitBreakStatement(it) });
    }

    public void visitContinueStatement(ContinueStatement node) {
        printNode(node, ContinueStatement, { super.visitContinueStatement(it) });
    }

    public void visitSynchronizedStatement(SynchronizedStatement node) {
        printNode(node, SynchronizedStatement, { super.visitSynchronizedStatement(it) });
    }

    public void visitThrowStatement(ThrowStatement node) {
        printNode(node, ThrowStatement, { super.visitThrowStatement(it) });
    }

    public void visitMethodCallExpression(MethodCallExpression node) {
        printNode(node, MethodCallExpression, { super.visitMethodCallExpression(it) });
    }

    public void visitStaticMethodCallExpression(StaticMethodCallExpression node) {
        printNode(node, StaticMethodCallExpression, { super.visitStaticMethodCallExpression(it) });
    }

    public void visitConstructorCallExpression(ConstructorCallExpression node) {
        printNode(node, ConstructorCallExpression, { super.visitConstructorCallExpression(it) });
    }

    public void visitBinaryExpression(BinaryExpression node) {
        printNode(node, BinaryExpression, { super.visitBinaryExpression(it) });
    }

    public void visitTernaryExpression(TernaryExpression node) {
        printNode(node, TernaryExpression, { super.visitTernaryExpression(it) });
    }

    public void visitShortTernaryExpression(ElvisOperatorExpression node) {
        printNode(node, ElvisOperatorExpression, { super.visitShortTernaryExpression(it) });
    }

    public void visitPostfixExpression(PostfixExpression node) {
        printNode(node, PostfixExpression, { super.visitPostfixExpression(it) });
    }

    public void visitPrefixExpression(PrefixExpression node) {
        printNode(node, PrefixExpression, { super.visitPrefixExpression(it) });
    }

    public void visitBooleanExpression(BooleanExpression node) {
        printNode(node, BooleanExpression, { super.visitBooleanExpression(it) });
    }

    public void visitNotExpression(NotExpression node) {
        printNode(node, NotExpression, { super.visitNotExpression(it) });
    }

    public void visitClosureExpression(ClosureExpression node) {
        printNode(node, ClosureExpression, {
            it.parameters?.each { parameter -> visitParameter(parameter) }
            super.visitClosureExpression(it)
        });
    }

    /**
     * Makes walking parameters look like others in the visitor.
     */
    public void visitParameter(Parameter node) {
        printNode(node, Parameter, {
            if (node.initialExpression) {
                node.initialExpression?.visit(this)
            }
        });
    }

    public void visitTupleExpression(TupleExpression node) {
        printNode(node, TupleExpression, { super.visitTupleExpression(it) });
    }

    public void visitListExpression(ListExpression node) {
        printNode(node, ListExpression, { super.visitListExpression(it) });
    }

    public void visitArrayExpression(ArrayExpression node) {
        printNode(node, ArrayExpression, { super.visitArrayExpression(it) });
    }

    public void visitMapExpression(MapExpression node) {
        printNode(node, MapExpression, { super.visitMapExpression(it) });
    }

    public void visitMapEntryExpression(MapEntryExpression node) {
        printNode(node, MapEntryExpression, { super.visitMapEntryExpression(it) });
    }

    public void visitRangeExpression(RangeExpression node) {
        printNode(node, RangeExpression, { super.visitRangeExpression(it) });
    }

    public void visitSpreadExpression(SpreadExpression node) {
        printNode(node, SpreadExpression, { super.visitSpreadExpression(it) });
    }

    public void visitSpreadMapExpression(SpreadMapExpression node) {
        printNode(node, SpreadMapExpression, { super.visitSpreadMapExpression(it) });
    }

    public void visitMethodPointerExpression(MethodPointerExpression node) {
        printNode(node, MethodPointerExpression, { super.visitMethodPointerExpression(it) });
    }

    public void visitUnaryMinusExpression(UnaryMinusExpression node) {
        printNode(node, UnaryMinusExpression, { super.visitUnaryMinusExpression(it) });
    }

    public void visitUnaryPlusExpression(UnaryPlusExpression node) {
        printNode(node, UnaryPlusExpression, { super.visitUnaryPlusExpression(it) });
    }

    public void visitBitwiseNegationExpression(BitwiseNegationExpression node) {
        printNode(node, BitwiseNegationExpression, { super.visitBitwiseNegationExpression(it) });
    }

    public void visitCastExpression(CastExpression node) {
        printNode(node, CastExpression, { super.visitCastExpression(it) });
    }

    public void visitConstantExpression(ConstantExpression node) {
        printNode(node, ConstantExpression, { super.visitConstantExpression(it) });
    }

    public void visitClassExpression(ClassExpression node) {
        printNode(node, ClassExpression, { super.visitClassExpression(it) });
    }

    public void visitVariableExpression(VariableExpression node) {
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

    public void visitDeclarationExpression(DeclarationExpression node) {
        printNode(node, DeclarationExpression, { super.visitDeclarationExpression(it) });
    }

    public void visitPropertyExpression(PropertyExpression node) {
        printNode(node, PropertyExpression, { super.visitPropertyExpression(it) });
    }

    public void visitAttributeExpression(AttributeExpression node) {
        printNode(node, AttributeExpression, { super.visitAttributeExpression(it) });
    }

    public void visitFieldExpression(FieldExpression node) {
        printNode(node, FieldExpression, { super.visitFieldExpression(it) });
    }

    public void visitGStringExpression(GStringExpression node) {
        printNode(node, GStringExpression, { super.visitGStringExpression(it) });
    }

    public void visitCatchStatement(CatchStatement node) {
        printNode(node, CatchStatement, {
            if (it.variable) visitParameter(it.variable)
            super.visitCatchStatement(it)
        });
    }

    public void visitArgumentlistExpression(ArgumentListExpression node) {
        printNode(node, ArgumentListExpression, { super.visitArgumentlistExpression(it) });
    }

    public void visitClosureListExpression(ClosureListExpression node) {
        printNode(node, ClosureListExpression, { super.visitClosureListExpression(it) });
    }

    public void visitBytecodeExpression(BytecodeExpression node) {
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