package com.atlassian.clover.instr;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Annotation used to mark methods which shall be processed by the
 * {@link com.atlassian.clover.build.codegen.InstrumentationBindingAPF} build tool.
 *
 * For example, by annotating a method as follows:
 * <pre>
 *     class Foo {
 *         &#64;ForInstrumentation
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
 */
@Target(METHOD)
@Retention(SOURCE)
public @interface ForInstrumentation {
    String value() default "";
}
