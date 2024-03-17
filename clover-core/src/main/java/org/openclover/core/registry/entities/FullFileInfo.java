package org.openclover.core.registry.entities;

import org.jetbrains.annotations.NotNull;
import org.openclover.core.api.registry.BlockMetrics;
import org.openclover.core.api.registry.BranchInfo;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.ContextSet;
import org.openclover.core.api.registry.EntityContainer;
import org.openclover.core.api.registry.EntityVisitor;
import org.openclover.core.api.registry.FileInfo;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.api.registry.MethodInfo;
import org.openclover.core.api.registry.PackageInfo;
import org.openclover.core.api.registry.SourceInfo;
import org.openclover.core.api.registry.StackTraceEntry;
import org.openclover.core.api.registry.StatementInfo;
import org.openclover.core.api.registry.TestCaseInfo;
import org.openclover.core.io.tags.TaggedDataInput;
import org.openclover.core.io.tags.TaggedDataOutput;
import org.openclover.core.io.tags.TaggedPersistent;
import org.openclover.core.api.registry.CoverageDataProvider;
import org.openclover.core.api.registry.CoverageDataReceptor;
import org.openclover.core.api.registry.ElementVisitor;
import org.openclover.core.registry.FixedSourceRegion;
import org.openclover.core.registry.metrics.ClassMetrics;
import org.openclover.core.registry.metrics.FileMetrics;
import org.openclover.core.api.registry.HasMetricsFilter;
import org.openclover.core.api.registry.HasMetricsNode;
import org.openclover.core.registry.util.EntityVisitorUtils;
import org.openclover.core.spi.lang.Language;
import org.openclover.core.util.FileUtils;
import org.openclover.core.util.Path;
import org.openclover.runtime.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.openclover.core.util.Lists.newArrayList;
import static org.openclover.core.util.Maps.newTreeMap;
import static org.openclover.core.util.Sets.newHashSet;
import static org.openclover.core.util.Sets.newTreeSet;

public class FullFileInfo
        implements CoverageDataReceptor, FileInfo, HasMetricsNode, TaggedPersistent {

    protected String name;
    protected String encoding;
    protected int lineCount;
    protected int ncLineCount;

    protected long timestamp;
    protected long filesize;
    protected long checksum;

    protected transient PackageInfo containingPackage;
    protected transient BlockMetrics rawMetrics;
    protected transient BlockMetrics metrics;
    protected transient ContextSet contextFilter;

    /** classes declared inside the file */
    protected Map<String, ClassInfo> classes = new LinkedHashMap<>();
    /**
     * statements declared inside the file on the top-level (e.g in scripts)
     */
    protected List<StatementInfo> statements = newArrayList();
    /**
     * methods (functions) declared on the top-level of the file (e.g. outside classes
     */
    protected List<MethodInfo> methods = newArrayList();

    private File actualFile;
    protected int dataIndex;
    private int dataLength;

    private long minVersion;
    private long maxVersion;

    private transient List<ClassInfo> orderedClasses;
    private transient LineInfo[] lineInfo;
    private transient Comparator<HasMetrics> orderby;
    private transient CoverageDataProvider data;
    private transient Map<Integer, List<StackTraceEntry>> failStackInfos;

    ///////////////////////////////////////////////////////////////////////////////////////////////

    public FullFileInfo(
            PackageInfo containingPackage, File actualFile, String encoding,
            int dataIndex, int lineCount, int ncLineCount, long timestamp, long filesize,
            long checksum, long minVersion) {

        this.containingPackage = containingPackage;
        this.name = actualFile.getName();
        this.encoding = encoding;
        this.lineCount = lineCount;
        this.ncLineCount = ncLineCount;
        this.timestamp = timestamp;
        this.filesize = filesize;
        this.checksum = checksum;

        this.actualFile = actualFile;
        this.dataIndex = dataIndex;
        this.minVersion = this.maxVersion = minVersion;
    }

    private FullFileInfo(final File actualFile, final String encoding,
                         final int dataIndex, final int dataLength,
                         final int lineCount, final int ncLineCount,
                         final long timestamp, final long checksum, final long filesize,
                         final long minVersion, final long maxVersion,
                         final Map<String, ClassInfo> classes,
                         final List<MethodInfo> methods,
                         final List<StatementInfo> statements) {

        this.containingPackage = null;
        this.name = actualFile.getName();
        this.encoding = encoding;
        this.lineCount = lineCount;
        this.ncLineCount = ncLineCount;
        this.timestamp = timestamp;
        this.filesize = filesize;
        this.checksum = checksum;

        this.actualFile = actualFile;
        this.dataIndex = dataIndex;
        this.dataLength = dataLength;
        this.minVersion = minVersion;
        this.maxVersion = maxVersion;
        this.classes.putAll(classes);
        this.methods = methods;
        this.statements = statements;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    // FileInfo

    @Override
    public ClassInfo getNamedClass(String name) {
        return classes.get(name);
    }

    @Override
    public String getEncoding() {
        return encoding;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public long getFileSize() {
        return filesize;
    }

    @Override
    public long getChecksum() {
        return checksum;
    }

    @Override
    public String getPackagePath() {
        if (containingPackage == null) {
            throw new IllegalStateException("This FileInfo has no PackageInfo set on it yet");
        }
        return containingPackage.getPath() + getName();
    }

    @Override
    public PackageInfo getContainingPackage() {
        return containingPackage;
    }

    @Override
    public void setContainingPackage(PackageInfo containingPackage) {
        this.containingPackage = containingPackage;
        for (ClassInfo classInfo : getClasses()) {
            classInfo.getClassMetadata().setPackage(containingPackage);
        }
    }

    @Override
    public int getLineCount() {
        return lineCount;
    }

    @Override
    public int getNcLineCount() {
        return ncLineCount;
    }

    @Override
    public boolean isTestFile() {
        for (ClassInfo classInfo : getClasses()) {
            if (classInfo.isTestClass()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public File getPhysicalFile() {
        return actualFile;
    }

    @Override
    public Reader getSourceReader() throws IOException {
        if (getEncoding() == null) {
            return new FileReader(getPhysicalFile());
        } else {
            return new InputStreamReader(Files.newInputStream(getPhysicalFile().toPath()), getEncoding());
        }
    }

    // HasContextFilter

    @Override
    public ContextSet getContextFilter() {
        return getContainingPackage().getContextFilter();
    }

    // HasParent

    @Override
    public EntityContainer getParent() {
        return entityVisitor -> entityVisitor.visitPackage(containingPackage);
    }

    // SourceInfo

    @Override
    public int getStartLine() {
        return 1;
    }

    @Override
    public int getStartColumn() {
        return 1;
    }

    @Override
    public int getEndLine() {
        return lineCount;
    }

    @Override
    public int getEndColumn() {
        return 1;  // TODO hack, not a true end
    }

    // EntityContainer

    /**
     * Visit yourself
     *
     * @param entityVisitor callback
     */
    @Override
    public void visit(EntityVisitor entityVisitor) {
        entityVisitor.visitFile(this);
    }

    // CoverageDataReceptor

    @Override
    public CoverageDataProvider getDataProvider() {
        return data;
    }

    @Override
    public void setDataProvider(final CoverageDataProvider data) {
        this.data = data;
        for (ClassInfo classInfo : classes.values()) {
            classInfo.setDataProvider(data);
        }
        for (MethodInfo methodInfo : methods) {
            methodInfo.setDataProvider(data);
        }
        // note: don't call setDataProvider on 'statements' because FullStatementInfo takes provider form its parent
        rawMetrics = null;
        metrics = null;
    }

    // InstrumentationInfo

    @Override
    public int getDataIndex() {
        return dataIndex;
    }

    @Override
    public int getDataLength() {
        return dataLength;
    }

    // HasClasses

    @Override
    @NotNull
    public List<ClassInfo> getClasses() {
        return newArrayList(classes.values());
    }

    @NotNull
    @Override
    public List<ClassInfo> getAllClasses() {
        final List<ClassInfo> allClasses = newArrayList();
        // in-order
        // get file-level classes and classes declared inside them
        for (ClassInfo classInfo : classes.values()) {
            allClasses.add(classInfo);
            allClasses.addAll(classInfo.getAllClasses());
        }
        // get file-level functions and classes declared inside them
        for (MethodInfo methodInfo : methods) {
            allClasses.addAll(methodInfo.getAllClasses());
        }
        return allClasses;
    }

    // HasMethods

    @Override
    @NotNull
    public List<MethodInfo> getMethods() {
        return newArrayList(methods); // copy of the list
    }

    @NotNull
    @Override
    public List<MethodInfo> getAllMethods() {
        final List<MethodInfo> allMethods = newArrayList();
        // in-order
        // get file-level classes and methods declared inside them
        for (ClassInfo classInfo : classes.values()) {
            allMethods.addAll(classInfo.getAllMethods());
        }
        // get file-level functions and functions declared inside them
        for (MethodInfo methodInfo : methods) {
            allMethods.add(methodInfo);
            allMethods.addAll(methodInfo.getAllMethods());
        }
        return allMethods;
    }

    // HasStatements

    @Override
    @NotNull
    public List<StatementInfo> getStatements() {
        return newArrayList(statements); // copy of the list
    }

    // HasMetrics

    @Override
    public String getName() {
        return name;
    }

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

    @Override
    public void setMetrics(BlockMetrics metrics) {
        this.metrics = metrics;
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
    public void setComparator(Comparator<HasMetrics> cmp) {
        orderby = cmp;
        orderedClasses = null;
        for (ClassInfo classInfo : classes.values()) {
            classInfo.setComparator(cmp);
        }
    }

    // HasVersionRanges

    @Override
    public void addVersion(long version) {
        if (minVersion == NO_VERSION) {
            minVersion = version;
        }
        maxVersion = Math.max(version, maxVersion);
    }

    @Override
    public void addVersions(long minVersion, long maxVersion) {
        if (this.minVersion == NO_VERSION) {
            this.minVersion = minVersion;
            this.maxVersion = maxVersion;
        } else {
            this.minVersion = Math.min(this.minVersion, minVersion);
            this.maxVersion = Math.max(this.maxVersion, maxVersion);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////


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
    @Override
    public Set<SourceInfo> gatherSourceRegions() {
        final Set<SourceInfo> regions = newTreeSet(FixedSourceRegion.SOURCE_ORDER_COMP);
        for (ClassInfo classInfo : classes.values()) {
            classInfo.gatherSourceRegions(regions);
        }
        for (MethodInfo methodInfo : methods) {
            methodInfo.gatherSourceRegions(regions);
        }
        regions.addAll(statements);
        return regions;
    }

    @Override
    public LineInfo[] getLineInfo(boolean showLambdaFunctions, boolean showInnerFunctions) {
        // not using the 0th array slot so line numbers index naturally
        return getLineInfo(getLineCount() + 1, showLambdaFunctions, showInnerFunctions);
    }

    @Override
    public LineInfo[] getLineInfo(int ensureLineCountAtLeast, final boolean showLambdaFunctions,
                                  final boolean showInnerFunctions) {
        if (lineInfo == null) {
            final LineInfo[] tmpLineInfo = new LineInfo[Math.max(getLineCount() + 1, ensureLineCountAtLeast)];
            visitElements(new ElementVisitor() {

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
                        lineInfo.addClassStart(info);
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
                            lineInfo.addMethodStart(info);
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
                            lineInfo.addStatement(info);
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
                            lineInfo.addBranch(info);
                        }
                    }
                }
            });

            if (failStackInfos != null) {
                for (final Map.Entry<Integer, List<StackTraceEntry>> entry : failStackInfos.entrySet()) {
                    final int line = entry.getKey();
                    final List<StackTraceEntry> stackFrames = entry.getValue();
                    if (line > 0 && line < tmpLineInfo.length) {
                        if (tmpLineInfo[line] == null) {
                            tmpLineInfo[line] = new LineInfo(line);
                        }
                        tmpLineInfo[line].setFailStackEntries(stackFrames.toArray(new FullStackTraceInfo.StackTraceEntryImpl[0]));
                    }

                }
            }

            lineInfo = tmpLineInfo;
        }

        return lineInfo;
    }

    @Override
    public void visitElements(ElementVisitor visitor) {
        for (ClassInfo classInfo : classes.values()) {
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

    @Override
    public void setDataLength(int length) {
        dataLength = length;
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
        return getChecksum() != checksum || getFileSize() != filesize;
    }

    @Override
    public void addClass(ClassInfo classInfo) {
        classes.put(classInfo.getName(), classInfo);
    }

    @Override
    public void addMethod(MethodInfo methodInfo) {
        methods.add(methodInfo);
    }

    @Override
    public void addStatement(StatementInfo statementInfo) {
        statements.add(statementInfo);
    }

    @Override
    public FileInfo copy(PackageInfo pkg, HasMetricsFilter filter) {
        FileInfo file = new FullFileInfo(pkg, actualFile, encoding, dataIndex, lineCount, ncLineCount, timestamp, filesize, checksum, minVersion);
        file.addVersion(maxVersion);
        file.setDataProvider(getDataProvider());

        for (ClassInfo classInfo : classes.values()) {
            if (filter.accept(classInfo)) {
                file.addClass(classInfo.copy(file, filter));
            }
        }
        for (MethodInfo methodInfo : methods) {
            if (filter.accept(methodInfo)) {
                file.addMethod(methodInfo.copy(file));
            }
        }
        for (StatementInfo statementInfo : statements) {
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
            tests = newHashSet();
            for (final Map.Entry<Integer, List<StackTraceEntry>> entry : failStackInfos.entrySet()) {
                final List<StackTraceEntry> entries = entry.getValue();
                for (StackTraceEntry stackTraceEntry : entries) {
                    tests.add(stackTraceEntry.getParentTrace().getOriginatingTest());
                }
            }
        }
        return tests;
    }

    public Map<Integer, List<StackTraceEntry>> getFailStackEntries() {
        return failStackInfos;
    }

    @Override
    public void setFailStackEntries(final Map<Integer, List<StackTraceEntry>> entries) {
        failStackInfos = new TreeMap<>(entries);
    }

    @Override
    public void addFailStackEntry(final int lineNum, final StackTraceEntry stackTraceEntry) {
        if (failStackInfos == null) {
            failStackInfos = newTreeMap();
        }
        final Integer lineKey = lineNum;
        List<StackTraceEntry> tracesForLine = failStackInfos.computeIfAbsent(lineKey, k -> newArrayList());
        tracesForLine.add(stackTraceEntry);
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
        List<ClassInfo> tmpOrderedClasses = newArrayList(classes.values());
        if (orderby != null) {
            tmpOrderedClasses.sort(orderby);
        } else {
            tmpOrderedClasses.sort(FixedSourceRegion.SOURCE_ORDER_COMP);
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
        for (ClassInfo classInfo : classes.values()) {
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

        for (MethodInfo methodInfo : methods) {
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
        for (StatementInfo statementInfo : statements) {
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
        final Map<String, ClassInfo> classes = readClasses(in);
        final List<MethodInfo> methods = in.readList(FullMethodInfo.class);
        final List<StatementInfo> statements = in.readList(FullStatementInfo.class);

        // construct file object and attach nested sub-elements
        final FullFileInfo fileInfo = new FullFileInfo(actualFile, encoding,
                dataIndex, dataLength,
                lineCount, ncLineCount,
                timestamp, checksum, filesize,
                minVersion, maxVersion,
                classes, methods, statements);
        for (ClassInfo classInfo : classes.values()) {
            classInfo.setContainingFile(fileInfo);
        }
        for (MethodInfo methodInfo : methods) {
            methodInfo.setContainingFile(fileInfo);
        }
        for (StatementInfo statementInfo : statements) {
            statementInfo.setContainingFile(fileInfo);
        }

        return fileInfo;
    }

    private static Map<String, ClassInfo> readClasses(TaggedDataInput in) throws IOException {
        // read list
        final List<ClassInfo> classInfos = in.readList(FullClassInfo.class);
        // and rewrite to map
        final Map<String, ClassInfo> classes = new LinkedHashMap<>(classInfos.size() * 2);
        for (ClassInfo classInfo : classInfos) {
            classes.put(classInfo.getName(), classInfo);
        }
        return classes;
    }

    public int hashCode() {
        return getPackagePath().hashCode();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final FullFileInfo that = (FullFileInfo) o;
        return this.getPackagePath().equals(that.getPackagePath());
    }

}
