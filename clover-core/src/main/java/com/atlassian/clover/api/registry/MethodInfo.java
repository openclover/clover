package com.atlassian.clover.api.registry;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a method (or an equivalent, like function)
 * Implements interfaces:
 * <ul>
 *  <li>HasClasses - classes declared inside a method body, for example:
 *   <ul>
 *      <li>anonymous inline class - Java, Groovy - currently Clover does not handle inline classes (it treats them as
 *      a part of the method body)</li>
 *   </ul>
 *  </li>
 *  <li>HasMethods - some programming languages allow to declare a method inside another method, for example:
 *    <ul>
 *      <li>a lambda expression inside a method - Java 8</li>
 *      <li>a function inside a function - Scala, Closure</li>
 *    </ul>
 *  </li>
 *  <li>HasStatements, HasBranches - returns code defined in a method body</li>
 *  <li>HasContextFilter - set of custom statement/method contexts for filtering</li>
 *  <li>HasMetrics, HasAggregatedMetrics - code metrics for the method</li>
 *  <li>HasParent - parent method/class/file for this method</li>
 * </ul>
 */
public interface MethodInfo extends
        ElementInfo, SourceInfo, InstrumentationInfo, EntityContainer,
        HasBranches, HasClasses, HasMethods, HasStatements,
        HasContextFilter, HasMetrics, HasAggregatedMetrics, HasParent {

    @Override
    String getName();

    String getSimpleName();

    String getQualifiedName();

    /**
     * Returns a class in which method is declared or <code>null</code> if method (actually it will be a function
     * or procedure) is declared outside the class.
     *
     * @return ClassInfo containing class or <code>null</code>
     */
    @Nullable
    ClassInfo getContainingClass();

    /**
     * Returns a method in which this method (an inner function actually) is declared or <code>null</code> if method
     * is not nested inside other method.
     *
     * @return MethodInfo containing method or <code>null</code>
     */
    @Nullable
    MethodInfo getContainingMethod();

    /**
     * Returns a file in which this method is declared. Note that some programming languages allows to define a
     * function outside a class (or other function).
     *
     * @return FileInfo file containing this method
     */
    @Nullable
    FileInfo getContainingFile();

    /**
     * Returns an object which encloses this method. You can access it via:
     * <ul>
     *     <li>{@link EntityVisitor#visitClass(ClassInfo)} - for a method declared inside a class</li>
     *     <li>{@link EntityVisitor#visitMethod(MethodInfo)} - for a method declared inside another method</li>
     *     <li>{@link EntityVisitor#visitFile(FileInfo)} - for a method declared in the file (on a top-level)</li>
     * </ul>
     *
     * @return EntityContainer parent code entity
     */
    @Override
    @NotNull
    EntityContainer getParent();

    /**
     * Returns whether method is empty, i.e. does not contain any executable code in it - no statements or branches.
     * Nested methods (functions) and classes defined inside the method body are treated separately, i.e. a method
     * having them can still be empty if it has no statements or branches.
     *
     * @return boolean true if has no statements and no branches, false otherwise
     */
    boolean isEmpty();

    /**
     * Returns method signature (annotations, keywords, type parameters, method name, parameters, return type, throws).
     *
     * @return MethodSignatureInfo
     */
    @NotNull
    MethodSignatureInfo getSignature();

    /**
     * Returns whether it's a test method or not. Classification as a test method depends on the test pattern
     * defined at instrumentation time.
     *
     * @return boolean true if test method, false otherwise
     */
    boolean isTest();

    /**
     * Name of the test associated with a method. Some test frameworks can declare a name of the test using annotations
     * or javadoc tags, so that later the test name and not the original method name is used in reporting.
     *
     * For instance, the Spock framweork uses @FeatureMetadata(name="name of the test")
     *
     * Note that this method returns a static name of the test, i.e. declared in the code. Some test frameworks might
     * dynamically generate test names. Examples are the Spock's @Unroll and JUnit's @Parameterized.
     *
     * @return String name of the test associated to this method or <code>null</code> if not declared
     */
    @Nullable
    String getStaticTestName();

    /**
     * Returns whether it's a lambda function or not.
     *
     * @return boolean <code>true</code> for lambda function, false otherwise
     */
    boolean isLambda();
}
