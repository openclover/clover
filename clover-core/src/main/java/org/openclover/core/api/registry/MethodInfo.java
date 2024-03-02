package org.openclover.core.api.registry;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

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
        CoverageDataRange, ElementInfo, EntityContainer, EntityEnclosure, SourceInfo,
        HasBranches, HasClasses, HasMethods, HasStatements,
        HasContextFilter, HasMetrics, HasAggregatedMetrics, HasParent,
        IsVisitable {


    /**
     * Add a class inside the method. Useful for method-scope classes or anonymous inline classes.
     */
    void addClass(ClassInfo classInfo);

    /**
     * Add a method inside the method. Useful for nested functions or inline lambda functions.
     */
    void addMethod(MethodInfo methodInfo);

    /**
     * Add a statement inside the method.
     */
    void addStatement(StatementInfo stmt);

    /**
     * Add a branch inside the method.
     */
    void addBranch(BranchInfo branch);

    /**
     * Create a copy of this method, setting the class as a parent.
     * Useful for methods declared inside clases.
     */
    MethodInfo copy(ClassInfo classAsParent);

    /**
     * Create a copy of this method, setting the method as a parent.
     * Useful for functions declared inside other functions.
     */
    MethodInfo copy(MethodInfo methodAsParent);

    /**
     * Create a copy of this method, setting the file as a parent.
     * Useful for top-level functions declared outside a class.
     */
    MethodInfo copy(FileInfo fileAsParent);

    /**
     * Collect all source regions inside this method - the method itself, all statements
     * and branches as well as all inner clases and methods (recursively).
     */
    void gatherSourceRegions(Set<SourceInfo> regions);

    /**
     * Returns source of coverage data for the method.
     */
    CoverageDataProvider getDataProvider();

    /**
     * Return a full method name, including argument types and a return type.
     */
    @Override
    String getName();

    /**
     * Return a base method name.
     */
    String getSimpleName();

    /**
     * Return a qualified method name, i.e with an enclosing fully qualified class name or an
     * enclosing fully qualified method name.
     */
    String getQualifiedName();

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
     * Returns method signature (annotations, keywords, type parameters, method name, parameters, return type, throws).
     *
     * @return MethodSignatureInfo
     */
    @NotNull
    MethodSignatureInfo getSignature();

    /**
     * Returns method visibility. A shorthand for getSignature().getBaseModifiersMask().
     * @return String - "public", "package", "protected" or "private"
     */
    String getVisibility();

    /**
     * Returns whether method is empty, i.e. does not contain any executable code in it - no statements or branches.
     * Nested methods (functions) and classes defined inside the method body are treated separately, i.e. a method
     * having them can still be empty if it has no statements or branches.
     *
     * @return boolean true if has no statements and no branches, false otherwise
     */
    boolean isEmpty();

    /**
     * Returns <code>true</code> if this method is filtered out (i.e. excluded by the filter).
     */
    boolean isFiltered(ContextSet filter);

    /**
     * Returns whether it's a lambda function or not.
     *
     * @return boolean <code>true</code> for lambda function, false otherwise
     */
    boolean isLambda();

    /**
     * Returns true if the method is public. A shorthand for getSignature().getBaseModifiersMask().
     */
    boolean isPublic();

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
     * <p/>
     * For instance, the Spock framweork uses @FeatureMetadata(name="name of the test")
     * <p/>
     * Note that this method returns a static name of the test, i.e. declared in the code. Some test frameworks might
     * dynamically generate test names. Examples are the Spock's @Unroll and JUnit's @Parameterized.
     *
     * @return String name of the test associated to this method or <code>null</code> if not declared
     */
    @Nullable
    String getStaticTestName();

    void setDataLength(int length);

    void setDataProvider(CoverageDataProvider data);

}
