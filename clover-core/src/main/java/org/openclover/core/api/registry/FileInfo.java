package org.openclover.core.api.registry;

import org.jetbrains.annotations.NotNull;
import org.openclover.core.registry.entities.LineInfo;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a single source file. A file is a part of a package - {@link #getContainingPackage()}
 *
 * Implemented interfaces:
 * <ul>
 *     <li>InstrumentationInfo - data index range for the whole file</li>
 *     <li>SourceInfo - represents content of the whole file</li>
 *     <li>HasClasses, HasMethods, HasStatements - code entities which can be declared in the file
 *     (on the lop level)</li>
 *     <li>HasContextFilter - set of custom statement/method contexts for filtering</li>
 *     <li>HasMetrics - code metrics for the file</li>
 *     <li>HasParent - parent package for this file</li>
 * </ul>
 */
public interface FileInfo extends
        CoverageDataRange, SourceInfo, EntityContainer,
        HasClasses, HasMethods, HasStatements,
        HasContextFilter, HasMetrics, HasParent, HasVersionRanges,
        IsMetricsComparable, IsVisitable {

    /**
     * Add a class inside the file.
     */
    void addClass(ClassInfo classInfo);

    /**
     * Add a method inside the file.
     */
    void addMethod(MethodInfo methodInfo);

    /**
     * Add a statement inside the file.
     */
    void addStatement(StatementInfo statementInfo);

    /**
     * Create a copy of the file, setting a package as a parent.
     */
    FileInfo copy(PackageInfo pkg, HasMetricsFilter filter);

    /**
     * Return a list of all source regions present in this file (all classes, methods, statements).
     */
    Set<SourceInfo> gatherSourceRegions();

    /**
     * Returns file's checksum.
     */
    long getChecksum();

    /**
     * Returns list of classes which are declared on a top-level of this source file  (i.e. it does not return inner
     * classes). Exact content may depend on the programming language, e.g.:
     * <ul>
     *  <li>Java, Groovy - classes and interfaces</li>
     *  <li>Scala - classes, objects, traits</li>
     * </ul>
     *
     * @return List&lt;ClassInfo&gt; - list of classes or empty list if none
     */
    @Override
    @NotNull
    List<ClassInfo> getClasses();

    /**
     * Returns a package for which this package belongs to (or the default package)
     * Note: it supports only one package namespace per source file
     *
     * @return PackageInfo
     */
    PackageInfo getContainingPackage();

    /**
     * Returns file encoding, e.g. "UTF-8"
     *
     * @return String
     */
    String getEncoding();

    /**
     * Returns file size in bytes
     * @return long size
     */
    long getFileSize();

    LineInfo[] getLineInfo(boolean showLambdaFunctions, boolean showInnerFunctions);

    LineInfo[] getLineInfo(int ensureLineCountAtLeast, boolean showLambdaFunctions,
                           boolean showInnerFunctions);

    /**
     * Returns list of methods which are declared on a top-level of this source file. Exact content may depend on the
     * programming language, e.g.:
     * <ul>
     *     <li>Java - not applicable</li>
     *     <li>Groovy - methods declared outside a class (in a groovy script)</li>
     *     <li>Scala - functions declared outside a class</li>
     * </ul>
     *
     * @return List&lt;MethodInfo&gt; - list of methods or empty list if none
     */
    @Override
    @NotNull
    List<MethodInfo> getMethods();

    /**
     * Returns source file name
     *
     * @return String
     */
    @Override
    String getName();

    ClassInfo getNamedClass(String name);

    /**
     * Returns a file name with a package path, e.g. "com/acme/Foo.java"
     * Note: it supports only one package namespace per source file
     *
     * @return String package path
     */
    String getPackagePath();

    File getPhysicalFile();

    Reader getSourceReader() throws IOException;

    /**
     * Returns list of statements which are declared on a top-level of this source file. Exact content may depend on the
     * programming language, e.g.:
     * <ul>
     *     <li>Java - not applicable (it's not possible to write statements outside a class)</li>
     *     <li>Groovy - methods declared outside a class (in a groovy script)</li>
     *     <li>Scala - functions declared outside a class</li>
     * </ul>
     *
     * @return List&lt;StatementInfo&gt; - list of methods or empty list if none
     */
    @Override
    @NotNull
    List<StatementInfo> getStatements();

    /**
     * Returns file modification time stamp as per File.lastModified()
     *
     * @return long time stamp
     */
    long getTimestamp();

    /**
     * Returns number of source lines in a file
     *
     * @return int
     */
    int getLineCount();

    /**
     * Returns number of non-comment and non-empty source lines in a file.
     *
     * @return int
     */
    int getNcLineCount();

    /**
     * Returns true if the file does not contain any inner code entities.
     *
     * @return boolean - true if getClasses() is empty
     */
    boolean isEmpty();

    /**
     * Returns true if this is a test file (according to the test pattern defined during instrumentation)
     *
     * @return boolean - true if it's a test file, false otherwise
     */
    boolean isTestFile();

    void setContainingPackage(PackageInfo containingPackage);

    void setDataLength(int length);

    void setDataProvider(final CoverageDataProvider data);

    void setFailStackEntries(Map<Integer, List<StackTraceEntry>> entries);

    void addFailStackEntry(int lineNum, StackTraceEntry stackTraceEntry);
}
