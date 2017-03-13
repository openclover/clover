package com.atlassian.clover.build.codegen;

import com.sun.mirror.apt.AnnotationProcessorFactory;
import com.sun.mirror.apt.AnnotationProcessor;
import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import com.sun.mirror.apt.Filer;
import com.sun.mirror.declaration.AnnotationTypeDeclaration;
import com.sun.mirror.declaration.Declaration;
import com.sun.mirror.declaration.MethodDeclaration;
import com.sun.mirror.declaration.Modifier;
import com.atlassian.clover.instr.ForInstrumentation;

import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class InstrumentationBindingAPF implements AnnotationProcessorFactory {
    @Override
    public Collection<String> supportedOptions() {
        return Collections.emptySet();
    }

    @Override
    public Collection<String> supportedAnnotationTypes() {
        return Collections.singleton(ForInstrumentation.class.getName());
    }

    @Override
    public AnnotationProcessor getProcessorFor(Set<AnnotationTypeDeclaration> atds, AnnotationProcessorEnvironment env) {
        return new Processor(atds, env);
    }

    public static class Processor implements AnnotationProcessor {
        private final Set<AnnotationTypeDeclaration> atds;
        private final AnnotationProcessorEnvironment env;

        public Processor(Set<AnnotationTypeDeclaration> atds, AnnotationProcessorEnvironment env) {
            this.atds = atds;
            this.env = env;
        }

        @Override
        public void process() {
            AnnotationTypeDeclaration mine = null;
            for (AnnotationTypeDeclaration atd : atds) {
                if (ForInstrumentation.class.getName().equals(atd.getQualifiedName())) {
                    mine = atd;
                }
            }
            if (mine != null) {
                try {
                    final PrintWriter writer = env.getFiler().createTextFile(
                        Filer.Location.SOURCE_TREE,
                        "com.atlassian.clover.instr",
                        new File("Bindings.java"),
                        "UTF-8");
                    writer.println("package com.atlassian.clover.instr;");
                    writer.println("public class Bindings {");

                    final Set<String> fqClassNames = new HashSet<String>();

                    for (Declaration decl : env.getDeclarationsAnnotatedWith(mine)) {
                        if (decl instanceof MethodDeclaration) {
                            MethodDeclaration methodDecl = (MethodDeclaration)decl;

                            final String fqClassName = methodDecl.getDeclaringType().getQualifiedName();
                            final String className = classNameOf(fqClassName);
                            final String packageName = pkgNameOf(fqClassName);
                            fqClassNames.add(fqClassName);

                            final boolean isStatic = methodDecl.getModifiers().contains(Modifier.STATIC);

                            writer.print("  public static String ");
                            writer.print("$");
                            writer.print(methodDecl.getDeclaringType().getSimpleName());
                            writer.print("$");

                            final String methodName =
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
                            for(int i = 0; i < numParams; i++) {
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
                                for(int i = 0; i < numParams; i++) {
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
                    for (String fqClassName : fqClassNames) {
                        String className = classNameOf(fqClassName);
                        writer.println("  private static final String " + className.toUpperCase(Locale.ENGLISH) + "_PKG;");
                    }
                    
                    writer.println("  static {");
                    for (String fqClassName : fqClassNames) {
                        String className = classNameOf(fqClassName);
                        //CLOVERVERSIONINFO_PKG = CloverVersionInfo.class.getName().substring(0, CloverVersionInfo.class.getName().indexOf(".CloverVersionInfo"));
                        writer.println("    " + className.toUpperCase(Locale.ENGLISH) + "_PKG = " + fqClassName + ".class.getName().substring(0, " + fqClassName + ".class.getName().indexOf(\"." + className + "\"));");
                    }
                    writer.println("  }");
                    writer.print("}");
                    writer.flush();
                } catch (IOException e) {
                    throw new RuntimeException("Failed to generate Bindings.java", e);
                }
            }
        }

        private String pkgNameOf(String fqClassName) {
            return fqClassName.substring(0, fqClassName.lastIndexOf("."));
        }

        private String classNameOf(String fqClassName) {
            return fqClassName.substring(fqClassName.lastIndexOf(".") + 1, fqClassName.length());
        }
    }
}
