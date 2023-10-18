package com.atlassian.clover.instr.groovy

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ConstructorNode
import org.codehaus.groovy.ast.DynamicVariable
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.GroovyClassVisitor
import org.codehaus.groovy.ast.InnerClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.ModuleNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.PropertyNode
import org.codehaus.groovy.ast.Variable
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ArrayExpression
import org.codehaus.groovy.ast.expr.AttributeExpression
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.BooleanExpression
import org.codehaus.groovy.ast.expr.CastExpression
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.ConstructorCallExpression
import org.codehaus.groovy.ast.expr.DeclarationExpression
import org.codehaus.groovy.ast.expr.FieldExpression
import org.codehaus.groovy.ast.expr.GStringExpression
import org.codehaus.groovy.ast.expr.ListExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.NotExpression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.ast.expr.SpreadExpression
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression
import org.codehaus.groovy.ast.expr.TupleExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.CatchStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.ast.stmt.TryCatchStatement

import java.util.stream.Collectors

@CompileStatic
class NodePrinter {
    static Map<String, Closure<GString>> NODE_CONFIG = [
            "org.codehaus.groovy.ast.ClassNode"                      : { ClassNode it ->
                "ClassNode - $it.name"
            },
            "org.codehaus.groovy.ast.InnerClassNode"                 : { InnerClassNode it ->
                "InnerClassNode - $it.name"
            },
            "org.codehaus.groovy.ast.ConstructorNode"                : { ConstructorNode it ->
                "ConstructorNode - $it.name (${joinParameterTypes(it.parameters)})"
            },
            "org.codehaus.groovy.ast.MethodNode"                     : { MethodNode it ->
                "MethodNode - $it.name (${joinParameterTypes(it.parameters)}) : $it.returnType - synthetic=$it.synthetic"
            },
            "org.codehaus.groovy.ast.FieldNode"                      : { FieldNode it ->
                "FieldNode - $it.name : $it.type"
            },
            "org.codehaus.groovy.ast.PropertyNode"                   : { PropertyNode it ->
                "PropertyNode - ${it.field?.name} : ${it.field?.type}"
            },
            "org.codehaus.groovy.ast.AnnotationNode"                 : { AnnotationNode it ->
                "AnnotationNode - ${it.classNode?.name}"
            },
            "org.codehaus.groovy.ast.Parameter"                      : { Parameter it ->
                "Parameter - $it.name"
            },
            "org.codehaus.groovy.ast.DynamicVariable"                : { DynamicVariable it ->
                "DynamicVariable - $it.name"
            },
            "org.codehaus.groovy.ast.stmt.BlockStatement"            : { BlockStatement it ->
                "BlockStatement - (${it.statements ? it.statements.size() : 0})"
            },
            "org.codehaus.groovy.ast.stmt.ExpressionStatement"       : { ExpressionStatement it ->
                "ExpressionStatement - ${it?.expression?.getClass()?.simpleName}"
            },
            "org.codehaus.groovy.ast.stmt.ReturnStatement"           : { ReturnStatement it ->
                "ReturnStatement - $it.text"
            },
            "org.codehaus.groovy.ast.stmt.TryCatchStatement"         : { TryCatchStatement it ->
                "TryCatchStatement - ${it.catchStatements?.size() ?: 0} catch, ${it.finallyStatement ? 1 : 0} finally"
            },
            "org.codehaus.groovy.ast.stmt.CatchStatement"            : { CatchStatement it ->
                "CatchStatement - $it.exceptionType"
            },
            "org.codehaus.groovy.ast.expr.ConstructorCallExpression" : { ConstructorCallExpression it ->
                "ConstructorCall - $it.text"
            },
            "org.codehaus.groovy.ast.expr.SpreadExpression"          : { SpreadExpression it ->
                "Spread - $it.text"
            },
            "org.codehaus.groovy.ast.expr.ArgumentListExpression"    : { ArgumentListExpression it ->
                "ArgumentList - $it.text"
            },
            "org.codehaus.groovy.ast.expr.MethodCallExpression"      : { MethodCallExpression it ->
                "MethodCall - $it.text (implicitThis=${it.implicitThis}, " +
                        "receiver=${it.hasProperty('receiver') ? it.receiver : ''}, " + // receiver not present in Groovy 1.x
                        "safe={$it.safe}, spreadSafe=${it.spreadSafe})"
            },
            "org.codehaus.groovy.ast.expr.StaticMethodCallExpression": { StaticMethodCallExpression it ->
                "StaticMethodCall - $it.text (" +
                        "receiver=${it.hasProperty('receiver') ? it.receiver : ''}, " +  // receiver not present in Groovy 1.x
                        "ownerType=${it.ownerType})"
            },
            "org.codehaus.groovy.ast.expr.GStringExpression"         : { GStringExpression it ->
                "GString - $it.text"
            },
            "org.codehaus.groovy.ast.expr.AttributeExpression"       : { AttributeExpression it ->
                "Attribute - $it.text"
            },
            "org.codehaus.groovy.ast.expr.DeclarationExpression"     : { DeclarationExpression it ->
                "Declaration - $it.text"
            },
            "org.codehaus.groovy.ast.expr.VariableExpression"        : { VariableExpression it ->
                "Variable - $it.name : $it.type"
            },
            "org.codehaus.groovy.ast.expr.ConstantExpression"        : { ConstantExpression it ->
                "Constant - $it.value : $it.type"
            },
            "org.codehaus.groovy.ast.expr.BinaryExpression"          : { BinaryExpression it ->
                "Binary - $it.text"
            },
            "org.codehaus.groovy.ast.expr.ClassExpression"           : { ClassExpression it ->
                "Class - $it.text"
            },
            "org.codehaus.groovy.ast.expr.BooleanExpression"         : { BooleanExpression it ->
                "Boolean - $it.text"
            },
            "org.codehaus.groovy.ast.expr.ArrayExpression"           : { ArrayExpression it ->
                "Array - $it.text"
            },
            "org.codehaus.groovy.ast.expr.ListExpression"            : { ListExpression it ->
                "List - $it.text"
            },
            "org.codehaus.groovy.ast.expr.TupleExpression"           : { TupleExpression it ->
                "Tuple - $it.text"
            },
            "org.codehaus.groovy.ast.expr.FieldExpression"           : { FieldExpression it ->
                "Field - $it.text"
            },
            "org.codehaus.groovy.ast.expr.PropertyExpression"        : { PropertyExpression it ->
                "Property - $it.propertyAsString"
            },
            "org.codehaus.groovy.ast.expr.NotExpression"             : { NotExpression it ->
                "Not - $it.text"
            },
            "org.codehaus.groovy.ast.expr.CastExpression"            : { CastExpression it ->
                "Cast - $it.text"
            },
    ]

    void print(ModuleNode module, Writer out) {
        new PrintingVisitor(this, out).with { GroovyClassVisitor printer ->
            List<ClassNode> classNodeList = module?.classes
            if (classNodeList != null) {
                for (ClassNode classNode : classNodeList) {
                    printer.visitClass(classNode)
                }
            }
        }
    }

    static String joinParameterTypes(Parameter[] parameters) {
        Arrays.asList(parameters).stream()
                .map({ Parameter it-> it.getType().toString() })
                .collect(Collectors.joining(", "))
    }

    static <T extends ASTNode> String toString(T node) {
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

    static <T extends Variable> String toString(T node) {
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

