package com.atlassian.clover.build.codegen;

import com.atlassian.clover.instr.ForInstrumentation;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class InstrumentationBindingAPF extends AbstractProcessor {

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotataions = new LinkedHashSet<String>();
        annotataions.add(ForInstrumentation.class.getCanonicalName());
        return annotataions;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        internalProcess(annotations, roundEnv);
        return true;
    }


    private void internalProcess(final Set<? extends TypeElement> atds, RoundEnvironment roundEnv) {
        TypeElement mine = null;
        for (TypeElement atd : atds) {
            if (atd.getQualifiedName().contentEquals(ForInstrumentation.class.getName())) {
                mine = atd;
            }
        }
        if (mine != null) {
            try {
                JavaFileObject classFile = processingEnv.getFiler().createSourceFile("com.atlassian.clover.instr.Bindings");
                PrintWriter writer = new PrintWriter(classFile.openWriter());

                writer.println("package com.atlassian.clover.instr;");
                writer.println("public class Bindings {");

                final Map<String, String> fqClassNames = new HashMap<String, String>();

                for (Element decl : roundEnv.getElementsAnnotatedWith(mine)) {
                    if (decl.getKind() == ElementKind.METHOD) {
                        ExecutableElement methodDecl = (ExecutableElement) decl;
                        final TypeElement fqClass = (TypeElement) methodDecl.getEnclosingElement();

                        final String className = fqClass.getSimpleName().toString();
                        final String fqClassName = fqClass.getQualifiedName().toString();
                        fqClassNames.put(fqClassName, className);

                        final boolean isStatic = methodDecl.getModifiers().contains(Modifier.STATIC);

                        writer.print("  public static String ");
                        writer.print("$");
                        writer.print(className);
                        writer.print("$");

                        final CharSequence methodName =
                                methodDecl.getAnnotation(ForInstrumentation.class).value().length() > 0
                                        ? methodDecl.getAnnotation(ForInstrumentation.class).value()
                                        : methodDecl.getSimpleName();
                        writer.print(methodName);
                        writer.print("(");

                        // limit number of arguments if set in annotation
                        final int maxArgs = methodDecl.getAnnotation(ForInstrumentation.class).maxArguments();
                        final int numParams = maxArgs == ForInstrumentation.UNLIMITED ? methodDecl.getParameters().size()
                                : Math.min(methodDecl.getParameters().size(), maxArgs);
                        if (!isStatic) {
                            writer.print("String instanceName");
                            if (numParams > 0) {
                                writer.print(", ");
                            }
                        }
                        for (int i = 0; i < numParams; i++) {
                            if (i == numParams - 1 && methodDecl.isVarArgs()) {
                                writer.print("String... param" + i);
                            } else {
                                writer.print("String param" + i);
                            }
                            if (i < numParams - 1) {
                                writer.print(", ");
                            }
                        }
                        writer.println(") {");
                        if (methodDecl.isVarArgs()) {
                            writer.println("    StringBuffer varArgs = new StringBuffer();");
                            writer.println("    int argIndex = 0;");
                            writer.println("    for (String varArg : param" + (methodDecl.getParameters().size() - 1) + ") {");
                            writer.println("      varArgs.append(varArg);");
                            writer.println("      if (argIndex < param" + (methodDecl.getParameters().size() - 1) + ".length - 1) {");
                            writer.println("        varArgs.append(\",\");");
                            writer.println("      }");
                            writer.println("    }");
                        }

                        // shall we write opening brace (true by default)?
                        final String optionalOpeningBrace =
                                methodDecl.getAnnotation(ForInstrumentation.class).openingBrace() ? "(" : "";
                        // shall we write closing brace and semicolon (true by default)?
                        final String optionalClosingBrace =
                                methodDecl.getAnnotation(ForInstrumentation.class).closingBrace() ? "+ \")\";" : ";";

                        if (isStatic) {
                            // write static method call like [return COMACME_PKG + ".MyClass.myMethod("]
                            writer.print("    return " + className.toUpperCase(Locale.ENGLISH)
                                    + "_PKG + \"." + className
                                    + "." + methodDecl.getSimpleName() + optionalOpeningBrace + "\"");
                        } else {
                            // write instance method call like [return instanceName + ".myMethod("]
                            writer.print("    return instanceName + \"."
                                    + methodDecl.getSimpleName() + optionalOpeningBrace + "\"");
                        }

                        // write parameters like [ + param0 + "," + param1 + "," + param2]
                        if (numParams > 0) {
                            writer.print(" + ");
                            for (int i = 0; i < numParams; i++) {
                                if (i == numParams - 1 && methodDecl.isVarArgs()) {
                                    writer.print("varArgs.toString()");
                                } else {
                                    writer.print("param" + i);
                                }
                                if (i < numParams - 1) {
                                    writer.print(" + \",\" + ");
                                }
                            }
                        }
                        writer.println(optionalClosingBrace);
                        writer.println("  }");
                    }
                }

                //Needed to resist jarjar mucking things up when packages change
                for (String className : fqClassNames.values()) {
                    writer.println("  private static final String " + className.toUpperCase(Locale.ENGLISH) + "_PKG;");
                }

                writer.println("  static {");
                for (Map.Entry<String, String> fqClassEntry : fqClassNames.entrySet()) {
                    String fqClassName = fqClassEntry.getKey();
                    String className = fqClassEntry.getValue();
                    //CLOVERVERSIONINFO_PKG = CloverVersionInfo.class.getName().substring(0, CloverVersionInfo.class.getName().indexOf(".CloverVersionInfo"));
                    writer.println("    " + className.toUpperCase(Locale.ENGLISH) + "_PKG = " + fqClassName + ".class.getName().substring(0, " + fqClassName + ".class.getName().indexOf(\"." + className + "\"));");
                }
                writer.println("  }");
                writer.print("}");
                writer.flush();
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException("Failed to generate Bindings.java", e);
            }
        }
    }
}
