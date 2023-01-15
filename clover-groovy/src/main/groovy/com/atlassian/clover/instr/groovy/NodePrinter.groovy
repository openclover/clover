package com.atlassian.clover.instr.groovy

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ModuleNode

class NodePrinter {
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

    void print(ModuleNode module, out) {
        new PrintingVisitor(this, out).with { printer -> module?.classes?.each { clazz -> printer.visitClass(clazz) } }
    }

    private static String toString(node) {
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

