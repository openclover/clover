package com.atlassian.clover.instr;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

/**
 * Annotation used to mark methods which shall be processed by the
 * {@link com.atlassian.clover.build.codegen.InstrumentationBindingAPF} build tool.
 *
 * For example, by annotating a method as follows:
 * <pre>
 *     class Foo {
 *         @ForInstrumentation
 *         void foo(int x, int y)
 *     }
 * </pre>
 *
 * Annotation processing tool will generate a method like this:
 * <pre>
 *     class Bindings {
 *         public static String $Foo$foo(String instanceName, String x, String y) {
 *             return instanceName + ".foo(" + param0 + "," + param1 + ");";
 *         }
 *     }
 * </pre>
 * which returns a string representing a call of the annotated method.
 *
 * You can tune code generation, for example:
 * <pre>
 *     class Foo {
 *         @ForInstrumentation(maxArguments = 1, closingBrace = false)
 *         void foo(int x, int y)
 *     }
 * </pre>
 *
 * will produce:
 * <pre>
 *     class Bindings {
 *         public static String $Foo$foo(String instanceName, String param0) {
 *             return instanceName + ".foo(" + param0;
 *         }
 *     }
 * </pre>
 */
@Target(METHOD)
@Retention(SOURCE)
public @interface ForInstrumentation {
    public static int UNLIMITED = -1;

    public String value() default "";

    /**
     * Maximum number of arguments of the annotated method which shall be in the wrapper.
     * Typically used with <code>closingBrace = false</code>
     * @return int maximum number of arguments which shall be passed in generated string
     */
    int maxArguments() default UNLIMITED;

    /**
     * Whether to generate binding method call with an opening brace for argument list
     * @return boolean true by default
     */
    boolean openingBrace() default true;

    /**
     * Whether to generate binding method call with a closing brace for argument list and a semicolon
     * @return boolean true by default
     */
    boolean closingBrace() default true;
}
