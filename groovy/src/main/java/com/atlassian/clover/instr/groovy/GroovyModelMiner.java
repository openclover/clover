package com.atlassian.clover.instr.groovy;

import com.atlassian.clover.registry.PersistentAnnotationValue;
import com.atlassian.clover.registry.entities.AnnotationImpl;
import com.atlassian.clover.registry.entities.ArrayAnnotationValue;
import com.atlassian.clover.registry.entities.MethodSignature;
import com.atlassian.clover.registry.entities.Modifiers;
import com.atlassian.clover.registry.entities.StringifiedAnnotationValue;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.GenericsType;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ListExpression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GroovyModelMiner {
    public static MethodSignature extractMethodSignature(MethodNode method) {
        return extractMethodSignature(method, new HashMap<String, ClassNode>());
    }

    public static MethodSignature extractMethodSignature(MethodNode method, Map<String, ClassNode> annotationClassNodes) {
        return new MethodSignature(
            methodNameFor(method),
            joinGenericTypes(method.getGenericsTypes()),
            extractReturnType(method),
            extractParameters(method),
            extractExceptions(method),
            extractModifiers(method, annotationClassNodes));
    }

    public static String methodNameFor(MethodNode method) {
        return
            GroovyUtils.isScriptClass(method.getDeclaringClass()) && "run".equals(method.getName())
                ? "script"
                : method.getName();
    }

    public static String extractReturnType(MethodNode method) {
        return extractVerbatimType(method.getReturnType(), method.isDynamicReturnType());
    }

    public static String extractVerbatimType(ClassNode type, boolean isDynamic) {
        return extractVerbatimType(type, isDynamic, false);
    }

    public static String extractVerbatimType(ClassNode type, boolean isDynamic, boolean fullyQualified) {
        if (isDynamic) {
            return "def";
        } else {
            if (type.isUsingGenerics()) {
                if (type.isGenericsPlaceHolder()) {
                    //Handles a single named generic type e.g. public <T> *T* foo(*T* t)
                    //which we don't want to convert to its underlying type (normally Object unless with upper bounds)
                    return type.getGenericsTypes()[0].getName();
                } else {
                    //Recursively handles generics including placeholders
                    return type.getNameWithoutPackage() + "<" + joinGenericTypes(type.getGenericsTypes()) + ">";

                }
            } else {
                if (type.isArray()) {
                    return extractVerbatimType(type.getComponentType(), false) + "[]";
                } else {
                    if (fullyQualified) {
                        return type.getName();
                    } else {
                        return type.getNameWithoutPackage();
                    }
                }
            }
        }
    }

    private static String joinGenericTypes(GenericsType[] genericsTypes) {
        final StringBuilder buf = new StringBuilder();
        for (int i = 0; genericsTypes != null && i < genericsTypes.length; i++) {
            buf.append(extractGenericType(genericsTypes[i]));
            if (i < genericsTypes.length - 1) {
                buf.append(", ");
            }
        }
        return buf.toString();
    }

    private static String joinVerbatimTypes(GenericsType g) {
        final StringBuilder buf = new StringBuilder();
        final ClassNode[] upperBounds = g.getUpperBounds();
        for (int i = 0; upperBounds != null && i < upperBounds.length; i++) {
            buf.append(extractVerbatimType(upperBounds[i], false));
            if (i < upperBounds.length - 1) {
                buf.append(", ");
            }
        }
        return buf.toString();
    }

    public static String extractGenericType(GenericsType g) {
        //Need special handling of ? because its not a placeholder
        if (g.isPlaceholder() || g.getName().equals("?")) {
            return g.getName() + extractBounds(g);
        } else {
            return extractVerbatimType(g.getType(), false);
        }
    }

    public static String extractBounds(GenericsType g) {
        if (g.getUpperBounds() != null) {
            return " extends " + joinVerbatimTypes(g);
        } else if (g.getLowerBound() != null) {
            return " super " + extractVerbatimType(g.getLowerBound(), false);
        } else {
            return "";
        }
    }

    public static Modifiers extractModifiers(ClassNode classNode) {
        return extractModifiers(classNode, new HashMap<String, ClassNode>());
    }

    public static Modifiers extractModifiers(MethodNode methodNode) {
        return extractModifiers(methodNode, new HashMap<String, ClassNode>());
    }

    /** Extracts modifiers from anything that supports the modfiers property and extends AnnotationNode  */
    public static Modifiers extractModifiers(AnnotatedNode annotatedNode, Map<String, ClassNode> classNodes) {
        return Modifiers.createFrom(
            annotatedNode instanceof ClassNode
                ? ((ClassNode)annotatedNode).getModifiers()
                : ((MethodNode)annotatedNode).getModifiers(),
            extractAnnotations(annotatedNode, classNodes));
    }

    public static String[] extractExceptions(MethodNode method) {
        final ClassNode[] exceptions = method.getExceptions();
        List<String> exceptionNames = new ArrayList<>(exceptions.length);
        for (ClassNode exception : exceptions) {
            exceptionNames.add(exception.getNameWithoutPackage());
        }
        return exceptionNames.toArray(new String[exceptionNames.size()]);
    }

    //FQ class name required here so Groovy uses the right Parameter class
    public static com.atlassian.clover.registry.entities.Parameter[] extractParameters(MethodNode method) {
        final Parameter[] parameters = method.getParameters();
        final List<com.atlassian.clover.registry.entities.Parameter> clovParams = new ArrayList<>(parameters.length);

        for (Parameter p : parameters) {
            clovParams.add(new com.atlassian.clover.registry.entities.Parameter(extractVerbatimType(p.getType(), p.isDynamicTyped()), p.getName()));
        }
        return clovParams.toArray(new com.atlassian.clover.registry.entities.Parameter[clovParams.size()]);
    }

    public static AnnotationImpl[] extractAnnotations(AnnotatedNode annotated) {
        return extractAnnotations(annotated, new HashMap<String, ClassNode>());
    }

    public static AnnotationImpl[] extractAnnotations(AnnotatedNode annotated, Map<String, ClassNode> classNodes) {
        final List<AnnotationNode> annotations = annotated.getAnnotations();
        final List<AnnotationImpl> clovAnnotations = new ArrayList<>(annotations.size());
        for (AnnotationNode n : annotations) {
            clovAnnotations.add(extractAnnotation(n, classNodes));
        }
        return clovAnnotations.toArray(new AnnotationImpl[clovAnnotations.size()]);
    }

    public static AnnotationImpl extractAnnotation(AnnotationNode annotationNode) {
        return extractAnnotation(annotationNode, new HashMap<String, ClassNode>());
    }

    public static AnnotationImpl extractAnnotation(AnnotationNode annotationNode, Map<String, ClassNode> classNodes) {
        AnnotationImpl annotation = new AnnotationImpl(annotationNode.getClassNode().getName());
        final Set<Map.Entry<String, Expression>> members = annotationNode.getMembers().entrySet();

        for (Map.Entry<String, Expression> e : members) {
            PersistentAnnotationValue value = extractAnnotationValue(e.getValue(), classNodes);
            if (value != null) {
                annotation.put(e.getKey(), value);
            }
        }
        return annotation;
    }

    public static PersistentAnnotationValue extractAnnotationValue(Expression e) {
        return extractAnnotationValue(e, new HashMap<String, ClassNode>());
    }

    public static PersistentAnnotationValue extractAnnotationValue(Expression e, Map<String, ClassNode> classNodes) {
        if (e instanceof ConstantExpression) {
            Object constantValue = ((ConstantExpression)e).getValue();
            if (constantValue instanceof String
                || constantValue instanceof Number
                || constantValue instanceof Boolean) {
                return new StringifiedAnnotationValue(constantValue.toString());
            } else if (constantValue instanceof AnnotationNode) {
                return extractAnnotation((AnnotationNode) constantValue, classNodes);
            }
        } else if (e instanceof ClassExpression) {
            //Capture any ClassNodes as these may be needed when augmenting the AST
            classNodes.put(e.getType().getName(), e.getType());
            return new StringifiedAnnotationValue(extractVerbatimType(e.getType(), false, true));
        } else if (e instanceof ListExpression) {
            ArrayAnnotationValue array = new ArrayAnnotationValue();
            final List<Expression> expressions = ((ListExpression) e).getExpressions();
            for (Expression expression : expressions) {
                PersistentAnnotationValue value = extractAnnotationValue(expression, classNodes);
                //TODO: What if we've encountered an array constant expression we can't evaluate?
                if (value != null) {
                    array.put(null, value);
                }
            }
            return array;
        }
        return null;
    }
}