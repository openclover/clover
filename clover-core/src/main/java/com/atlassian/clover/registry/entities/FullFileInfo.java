package com.atlassian.clover.registry.entities;

import clover.com.google.common.collect.Sets;
import com.atlassian.clover.Logger;
import com.atlassian.clover.api.registry.BlockMetrics;
import com.atlassian.clover.api.registry.BranchInfo;
import com.atlassian.clover.api.registry.ClassInfo;
import com.atlassian.clover.api.registry.FileInfo;
import com.atlassian.clover.api.registry.MethodInfo;
import com.atlassian.clover.api.registry.SourceInfo;
import com.atlassian.clover.api.registry.StatementInfo;
import com.atlassian.clover.io.tags.TaggedDataInput;
import com.atlassian.clover.io.tags.TaggedDataOutput;
import com.atlassian.clover.io.tags.TaggedPersistent;
import com.atlassian.clover.registry.CoverageDataProvider;
import com.atlassian.clover.registry.CoverageDataReceptor;
import com.atlassian.clover.registry.FileElementVisitor;
import com.atlassian.clover.registry.FixedSourceRegion;
import com.atlassian.clover.registry.metrics.ClassMetrics;
import com.atlassian.clover.registry.metrics.FileMetrics;
import com.atlassian.clover.registry.metrics.HasMetricsFilter;
import com.atlassian.clover.registry.metrics.HasMetricsNode;
import com.atlassian.clover.registry.util.EntityVisitorUtils;
import com.atlassian.clover.spi.lang.Language;
import com.atlassian.clover.util.FileUtils;
import com.atlassian.clover.util.Path;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static clover.com.google.common.collect.Lists.newArrayList;
import static clover.com.google.common.collect.Maps.newTreeMap;


public class FullFileInfo extends BaseFileInfo implements CoverageDataReceptor, FileInfo, HasMetricsNode, TaggedPersistent {
    public static final long NO_VERSION = -1L;

    /** classes declared inside the file */
    protected Map<String, FullClassInfo> classes = new LinkedHashMap<String, FullClassInfo>();
    /**
     * statements declared inside the file on the top-level (e.g in scripts)
     */
    protected List<FullStatementInfo> statements = newArrayList();
    /**
     * methods (functions) declared on the top-level of the file (e.g. outside classes
     */
    protected List<FullMethodInfo> methods = newArrayList();

    private File actualFile;
    protected int dataIndex;
    private int dataLength;

    private long minVersion;
    private long maxVersion;

    private transient List orderedClasses;
    private transient LineInfo[] lineInfo;
    private transient Comparator orderby;
    private transient CoverageDataProvider data;
    private transient Map<Integer, List<StackTraceInfo.TraceEntry>> failStackInfos;

    ///////////////////////////////////////////////////////////////////////////////////////////////

    public FullFileInfo(
            FullPackageInfo containingPackage, File actualFile, String encoding,
            int dataIndex, int lineCount, int ncLineCount, long timestamp, long filesize,
            long checksum, long minVersion) {

        super(containingPackage, actualFile.getName(), encoding, lineCount, ncLineCount, timestamp, filesize, checksum);
        this.actualFile = actualFile;
        this.dataIndex = dataIndex;
        this.lineCount = lineCount;
        this.minVersion = this.maxVersion = minVersion;
    }

    private FullFileInfo(final File actualFile, final String encoding,
                         final int dataIndex, final int dataLength,
                         final int lineCount, final int ncLineCount,
                         final long timestamp, final long checksum, final long filesize,
                         final long minVersion, final long maxVersion,
                         final Map<String, FullClassInfo> classes,
                         final List<FullMethodInfo> methods,
                         final List<FullStatementInfo> statements) {
        super(null, actualFile.getName(), encoding, lineCount, ncLineCount, timestamp, filesize, checksum);
        this.actualFile = actualFile;
        this.dataIndex = dataIndex;
        this.dataLength = dataLength;
        this.lineCount = lineCount;
        this.minVersion = minVersion;
        this.maxVersion = maxVersion;
        this.classes.putAll(classes);
        this.methods = methods;
        this.statements = statements;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    // CoverageDataReceptor

    @Override
    public CoverageDataProvider getDataProvider() {
        return data;
    }

    @Override
    public void setDataProvider(final CoverageDataProvider data) {
        this.data = data;
        for (FullClassInfo classInfo : classes.values()) {
            classInfo.setDataProvider(data);
        }
        for (FullMethodInfo methodInfo : methods) {
            methodInfo.setDataProvider(data);
        }
        // note: don't call setDataProvider on 'statements' because FullStatementInfo takes provider form its parent
        rawMetrics = null;
        metrics = null;
    }

    // FileInfo -> InstrumentationInfo

    @Override
    public int getDataIndex() {
        return dataIndex;
    }

    @Override
    public int getDataLength() {
        return dataLength;
    }

    // FileInfo -> SourceInfo - see BaseFileInfo

    // FileInfo -> HasClasses

    @Override
    @NotNull
    public List<? extends ClassInfo> getClasses() {
        return newArrayList(classes.values());
    }

    @NotNull
    @Override
    public List<? extends ClassInfo> getAllClasses() {
        final List<ClassInfo> allClasses = newArrayList();
        // in-order
        // get file-level classes and classes declared inside them
        for (FullClassInfo classInfo : classes.values()) {
            allClasses.add(classInfo);
            allClasses.addAll(classInfo.getAllClasses());
        }
        // get file-level functions and classes declared inside them
        for (FullMethodInfo methodInfo : methods) {
            allClasses.addAll(methodInfo.getAllClasses());
        }
        return allClasses;
    }

    // FileInfo -> HasMethods

    @Override
    @NotNull
    public List<? extends MethodInfo> getMethods() {
        return newArrayList(methods); // copy of the list
    }

    @NotNull
    @Override
    public List<? extends MethodInfo> getAllMethods() {
        final List<MethodInfo> allMethods = newArrayList();
        // in-order
        // get file-level classes and methods declared inside them
        for (FullClassInfo classInfo : classes.values()) {
            allMethods.addAll(classInfo.getAllMethods());
        }
        // get file-level functions and functions declared inside them
        for (FullMethodInfo methodInfo : methods) {
            allMethods.add(methodInfo);
            allMethods.addAll(methodInfo.getAllMethods());
        }
        return allMethods;
    }

    // FileInfo -> HasStatements

    @Override
    @NotNull
    public List<? extends StatementInfo> getStatements() {
        return newArrayList(statements); // copy of the list
    }

    // FileInfo -> HasContextFilter - see BaseFile Info

    // FileInfo -> HasMetrics

    @Override
    public BlockMetrics getMetrics() {
        if (metrics == null || getContainingPackage().getContextFilter() != contextFilter) {
            contextFilter = getContainingPackage().getContextFilter();
            metrics = calcMetrics(true);
        }
        return metrics;
    }

    @Override
    public BlockMetrics getRawMetrics() {
        if (rawMetrics == null) {
            rawMetrics = calcMetrics(false);
        }
        return rawMetrics;
    }

    // HasMetricsNode

    @Override
    public String getChildType() {
        return "class";
    }

    @Override
    public boolean isEmpty() {
        return classes.size() == 0;
    }

    @Override
    public int getNumChildren() {
        return classes.size();
    }

    @Override
    public HasMetricsNode getChild(int i) {
        if (orderedClasses == null) {
            buildOrderedClassList();
        }
        // todo - bounds checking?
        return (HasMetricsNode)orderedClasses.get(i);
    }

    @Override
    public int getIndexOfChild(HasMetricsNode child) {
        if (orderedClasses == null) {
            buildOrderedClassList();
        }
        return orderedClasses.indexOf(child);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public void setComparator(Comparator cmp) {
        orderby = cmp;
        orderedClasses = null;
        for (FullClassInfo classInfo : classes.values()) {
            classInfo.setComparator(cmp);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    public File getPhysicalFile() {
        return actualFile;
    }

    public boolean validatePhysicalFile() {
        if (actualFile.exists()) {
            try {
                return FileUtils.calcAdlerChecksum(getPhysicalFile(), getEncoding()) == getChecksum();
            }
            catch (IOException e) {
                Logger.getInstance().warn("IOException calculating file checksum for " + actualFile , e);
            }
        }
        return false;
    }

    /**
     * @return a set of source regions for this file, sorted by start position and length.
     */
    public Set<SourceInfo> getSourceRegions() {
        final Set<SourceInfo> regions = Sets.newTreeSet(FixedSourceRegion.SOURCE_ORDER_COMP);
        for (ClassInfo classInfo : classes.values()) {
            FullClassInfo fullClassInfo = (FullClassInfo) classInfo;
            fullClassInfo.gatherSourceRegions(regions);
        }
        for (MethodInfo methodInfo : methods) {
            FullMethodInfo fullMethodInfo = (FullMethodInfo) methodInfo;
            fullMethodInfo.gatherSourceRegions(regions);
        }
        regions.addAll(statements);
        return regions;
    }

    public LineInfo[] getLineInfo(boolean showLambdaFunctions, boolean showInnerFunctions) {
        // not using the 0th array slot so line numbers index naturally
        return getLineInfo(getLineCount() + 1, showLambdaFunctions, showInnerFunctions);
    }

    public LineInfo[] getLineInfo(int ensureLineCountAtLeast, final boolean showLambdaFunctions,
                                  final boolean showInnerFunctions) {
        if (lineInfo == null) {
            final LineInfo[] tmpLineInfo = new LineInfo[Math.max(getLineCount() + 1, ensureLineCountAtLeast)];
            visitElements(new FileElementVisitor() {

                final EntityVisitorUtils entityUtils = new EntityVisitorUtils();

                private LineInfo getOrCreateLineInfo(SourceInfo r) {
                    final int startLine = r.getStartLine();
                    LineInfo lineInfo = null;
                    if (startLine >= 1 && startLine < tmpLineInfo.length) {
                        lineInfo = tmpLineInfo[startLine];
                        if (lineInfo == null) {
                            tmpLineInfo[startLine] = lineInfo = new LineInfo(startLine);
                        }
                    }
                    return lineInfo;
                }

                @Override
                public void visitClass(ClassInfo info) {
                    final LineInfo lineInfo = getOrCreateLineInfo(info);
                    if (lineInfo != null) {
                        lineInfo.addClassStart((FullClassInfo)info);
                    }
                }

                @Override
                public void visitMethod(MethodInfo info) {
                    final LineInfo lineInfo = getOrCreateLineInfo(info);
                    if (lineInfo != null) {
                        // do not add this method if it's a lambda and we're not showing them or
                        // if it's an inner method and we're not showing them
                        if ( (showLambdaFunctions || !info.isLambda())
                                && (showInnerFunctions || !entityUtils.isInnerMethod(info)) ) {
                            lineInfo.addMethodStart((FullMethodInfo)info);
                        }
                    }
                }

                @Override
                public void visitStatement(StatementInfo info) {
                    final LineInfo lineInfo = getOrCreateLineInfo(info);
                    if (lineInfo != null) {
                        // do not add this statement if it belongs to a lambda or an inner method and we're not showing them
                        if ( !entityUtils.isParentAMethod(info)    // the statement is under a class or a file
                                || ( (showLambdaFunctions || !entityUtils.isParentALambdaMethod(info))  // inside a lambda
                                        && (showInnerFunctions || !entityUtils.isParentAnInnerMethod(info)) ) // inside an inner method
                                ) {
                            lineInfo.addStatement((FullStatementInfo)info);
                        }
                    }
                }

                @Override
                public void visitBranch(BranchInfo info) {
                    final LineInfo lineInfo = getOrCreateLineInfo(info);
                    if (lineInfo != null) {
                        // do not add this statement if it belongs to a lambda or an inner method and we're not showing them
                        if ( !entityUtils.isParentAMethod(info)    // the branch is under a class or a file
                                || ( (showLambdaFunctions || !entityUtils.isParentALambdaMethod(info))  // branch inside a lambda
                                        && (showInnerFunctions || !entityUtils.isParentAnInnerMethod(info)) ) // branch inside an inner method
                                ) {
                            lineInfo.addBranch((FullBranchInfo)info);
                        }
                    }
                }
            });

            if (failStackInfos != null) {
                for (final Map.Entry<Integer, List<StackTraceInfo.TraceEntry>> entry : failStackInfos.entrySet()) {
                    final int line = entry.getKey();
                    final List<StackTraceInfo.TraceEntry> stackFrames = entry.getValue();
                    if (line > 0 && line < tmpLineInfo.length) {
                        if (tmpLineInfo[line] == null) {
                            tmpLineInfo[line] = new LineInfo(line);
                        }
                        tmpLineInfo[line].setFailStackEntries(stackFrames.toArray(new StackTraceInfo.TraceEntry[0]));
                    }

                }
            }

            lineInfo = tmpLineInfo;
        }

        return lineInfo;
    }

    public void visitElements(FileElementVisitor visitor) {
        for (FullClassInfo classInfo : classes.values()) {
            classInfo.visitElements(visitor);
        }
    }

    public void resolve(Path path) {
        File resolved = path.resolveFile(getPackagePath());
        if (resolved != null) {
            actualFile = resolved;
        }
    }

    public void setDataIndex(int dataIndex) {
        this.dataIndex = dataIndex;
    }

    public void setDataLength(int length) {
        dataLength = length;
    }

    public void addVersion(long version) {
        if (minVersion == NO_VERSION) {
            minVersion = version;
        }
        maxVersion = Math.max(version, maxVersion);
    }

    public void addVersions(long minVersion, long maxVersion) {
        if (this.minVersion == NO_VERSION) {
            this.minVersion = minVersion;
            this.maxVersion = maxVersion;
        }
        else {
            this.minVersion = Math.min(this.minVersion, minVersion);
            this.maxVersion = Math.max(this.maxVersion, maxVersion);
        }
    }

    // NO_VERSION should be checked here, but in the lifecycle of a FullFileInfo, it will never be NO_VERSION and have
    // this method called.
    public boolean supportsVersion(long version) {
        return version <= maxVersion && version >= minVersion;
    }

    public long getMinVersion() {
        return minVersion;
    }

    public long getMaxVersion() {
        return maxVersion;
    }

    public void resetVersions(long version) {
        minVersion = maxVersion = version;
    }

    public boolean changedFrom(long checksum, long filesize) {
        return getChecksum() != checksum || getFilesize() != filesize;
    }

    public void addClass(FullClassInfo classInfo) {
        classes.put(classInfo.getName(), classInfo);
    }

    public void addMethod(FullMethodInfo methodInfo) {
        methods.add(methodInfo);
    }

    public void addStatement(FullStatementInfo statementInfo) {
        statements.add(statementInfo);
    }

    public ClassInfo getNamedClass(String name) {
        return classes.get(name);
    }

    public FullFileInfo copy(FullPackageInfo pkg, HasMetricsFilter filter) {
        FullFileInfo file = new FullFileInfo(pkg, actualFile, encoding, dataIndex, lineCount, ncLineCount, timestamp, filesize, checksum, minVersion);
        file.addVersion(maxVersion);
        file.setDataProvider(getDataProvider());

        for (FullClassInfo classInfo : classes.values()) {
            if (filter.accept(classInfo)) {
                file.addClass(classInfo.copy(file, filter));
            }
        }
        for (FullMethodInfo methodInfo : methods) {
            if (filter.accept(methodInfo)) {
                file.addMethod(methodInfo.copy(file));
            }
        }
        for (FullStatementInfo statementInfo : statements) {
            file.addStatement(statementInfo); // TODO what about metric filtering for statements ?
        }

        file.setDataLength(getDataLength());
        if (failStackInfos != null) {
            file.setFailStackEntries(failStackInfos);
        }
        return file;
    }

    /**
     * @return the unique set of tests whose failures touched this class
     */
    public Set<TestCaseInfo> getUniqueFailingTests() {
        Set<TestCaseInfo> tests = null;

        if (failStackInfos != null) {
            tests = Sets.newHashSet();
            for (final Map.Entry<Integer, List<StackTraceInfo.TraceEntry>> entry : failStackInfos.entrySet()) {
                final List<StackTraceInfo.TraceEntry> entries = entry.getValue();
                for (StackTraceInfo.TraceEntry traceEntry : entries) {
                    tests.add(traceEntry.getParentTrace().getOriginatingTest());
                }
            }
        }
        return tests;
    }

    public Map<Integer, List<StackTraceInfo.TraceEntry>> getFailStackEntries() {
        return failStackInfos;
    }

    public void setFailStackEntries(final Map<Integer, List<StackTraceInfo.TraceEntry>> entries) {
        failStackInfos = new TreeMap<Integer, List<StackTraceInfo.TraceEntry>>(entries);
    }

    public void addFailStackEntry(final int lineNum, final StackTraceInfo.TraceEntry traceEntry) {
        if (failStackInfos == null) {
            failStackInfos = newTreeMap();
        }
        final Integer lineKey = lineNum;
        List<StackTraceInfo.TraceEntry> tracesForLine = failStackInfos.get(lineKey);
        if (tracesForLine == null) {
            tracesForLine = newArrayList();
            failStackInfos.put(lineKey, tracesForLine);
        }
        tracesForLine.add(traceEntry);
    }

    public Reader getSourceReader() throws FileNotFoundException, UnsupportedEncodingException {
        if (getEncoding() == null) {
            return new FileReader(getPhysicalFile());
        } else {
            return new InputStreamReader(new FileInputStream(getPhysicalFile()), getEncoding());
        }
    }

    public Language getLanguage() {
        if (actualFile != null) {
            for (Language language : Language.Builtin.values()) {
                for (String extension : language.getFileExtensions()) {
                    if (actualFile.getPath().endsWith(extension)) {
                        return language;
                    }
                }
            }
        }
        return null;
    }

    private void buildOrderedClassList() {
        List<FullClassInfo> tmpOrderedClasses = newArrayList(classes.values());
        if (orderby != null) {
            Collections.sort(tmpOrderedClasses, orderby);
        } else {
            Collections.sort(tmpOrderedClasses, FixedSourceRegion.SOURCE_ORDER_COMP);
        }
        orderedClasses = tmpOrderedClasses;
    }

    private FileMetrics calcMetrics(boolean filtered) {
        final FileMetrics fileMetrics = new FileMetrics(this);
        fileMetrics.setLineCount(lineCount);
        fileMetrics.setNcLineCount(ncLineCount);

        calcAndAddClassMetrics(fileMetrics, filtered);
        calcAndAddMethodMetrics(fileMetrics, filtered);
        calcAndAddStatementMetrics(fileMetrics);

        return fileMetrics;
    }

    /**
     * Collect metrics from classes defined in this file
     */
    private void calcAndAddClassMetrics(FileMetrics fileMetrics, boolean filtered) {
        int numClasses = 0;
        for (FullClassInfo classInfo : classes.values()) {
            if (!filtered) {
                fileMetrics.add((ClassMetrics) classInfo.getRawMetrics());
            } else {
                fileMetrics.add((ClassMetrics) classInfo.getMetrics());
            }
            numClasses += 1 + classInfo.getClasses().size(); // top-level class and its inner classes
        }
        fileMetrics.setNumClasses(numClasses);
    }

    /**
     * Collect metrics from top-level methods (functions) defined in this file
     */
    private void calcAndAddMethodMetrics(FileMetrics fileMetrics, boolean isFiltered) {
        int covered = 0;
        int numMethods = 0;
        int numTestMethods = 0;

        for (FullMethodInfo methodInfo : methods) {
            if (methodInfo.isFiltered(contextFilter)) {
                continue;
            }
            if (!isFiltered) {
                fileMetrics.add(methodInfo.getRawMetrics());
            } else {
                fileMetrics.add(methodInfo.getMetrics());
            }
            if (methodInfo.getHitCount() > 0) {
                covered++;
            }
            if (methodInfo.isTest()) {
                numTestMethods++;
            }
            numMethods++;
        }

        fileMetrics.addNumMethods(numMethods);
        fileMetrics.addNumCoveredMethods(covered);
        fileMetrics.addNumTestMethods(numTestMethods);
    }

    /**
     * Collect metrics from top-level statements defined in this file
     */
    private void calcAndAddStatementMetrics(FileMetrics fileMetrics) {
        int covered = 0;
        int numStatements = 0;
        int complexity = 0;

        // sum metrics from statements declared in a file
        for (FullStatementInfo statementInfo : statements) {
            if (statementInfo.isFiltered(contextFilter)) {
                continue;
            }
            if (statementInfo.getHitCount() > 0) {
                covered++;
            }
            complexity += statementInfo.getComplexity();
            numStatements++;
        }

        fileMetrics.addNumCoveredStatements(covered);
        fileMetrics.addNumStatements(numStatements);
        fileMetrics.addComplexity(complexity);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    @SuppressWarnings("unchecked")
    public void write(TaggedDataOutput out) throws IOException {
        // write file's metadata
        out.writeUTF(actualFile.getPath());
        out.writeUTF(encoding);
        out.writeLong(timestamp);
        out.writeLong(filesize);
        out.writeLong(checksum);
        out.writeInt(dataIndex);
        out.writeInt(dataLength);
        out.writeLong(minVersion);
        out.writeLong(maxVersion);
        out.writeInt(lineCount);
        out.writeInt(ncLineCount);

        // write classes, functions and statements
        out.writeList(FullClassInfo.class, newArrayList(classes.values()));
        out.writeList(FullMethodInfo.class, methods);
        out.writeList(FullStatementInfo.class, statements);
    }

    public static FullFileInfo read(TaggedDataInput in) throws IOException {
        // Make sure that file separators are interpreted correctly across Unix/Windows platforms.
        // Database can be written on one platform (e.g. Windows) and read on another (e.g. Linux).
        final String actualFileName = FileUtils.getPlatformSpecificPath(in.readUTF());
        final File actualFile = new File(actualFileName);
        final String encoding = in.readUTF();
        final long timestamp = in.readLong();
        final long filesize = in.readLong();
        final long checksum = in.readLong();
        final int dataIndex = in.readInt();
        final int dataLength = in.readInt();
        final long minVersion = in.readLong();
        final long maxVersion = in.readLong();
        final int lineCount = in.readInt();
        final int ncLineCount = in.readInt();

        // read classes, functions and statements
        final Map<String, FullClassInfo> classes = readClasses(in);
        final List<FullMethodInfo> methods = in.readList(FullMethodInfo.class);
        final List<FullStatementInfo> statements = in.readList(FullStatementInfo.class);

        // construct file object and attach nested sub-elements
        final FullFileInfo fileInfo = new FullFileInfo(actualFile, encoding,
                dataIndex, dataLength,
                lineCount, ncLineCount,
                timestamp, checksum, filesize,
                minVersion, maxVersion,
                classes, methods, statements);
        for (FullClassInfo classInfo : classes.values()) {
            classInfo.setContainingFile(fileInfo);
        }
        for (FullMethodInfo methodInfo : methods) {
            methodInfo.setContainingFile(fileInfo);
        }
        for (FullStatementInfo statementInfo : statements) {
            statementInfo.setContainingFile(fileInfo);
        }

        return fileInfo;
    }

    private static Map<String, FullClassInfo> readClasses(TaggedDataInput in) throws IOException {
        // read list
        final List<FullClassInfo> classInfos = in.readList(FullClassInfo.class);
        // and rewrite to map
        final Map<String, FullClassInfo> classes = new LinkedHashMap<String, FullClassInfo>(classInfos.size()  * 2);
        for(FullClassInfo classInfo : classInfos) {
            classes.put(classInfo.getName(), classInfo);
        }
        return classes;
    }

}
