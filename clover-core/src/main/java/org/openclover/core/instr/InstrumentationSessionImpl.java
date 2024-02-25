package org.openclover.core.instr;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openclover.core.api.instrumentation.ConcurrentInstrumentationException;
import org.openclover.core.api.instrumentation.InstrumentationSession;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.ContextSet;
import org.openclover.core.api.registry.EntityContainer;
import org.openclover.core.api.registry.EntityVisitor;
import org.openclover.core.api.registry.FileInfo;
import org.openclover.core.api.registry.MethodInfo;
import org.openclover.core.api.registry.ModifiersInfo;
import org.openclover.core.api.registry.PackageInfo;
import org.openclover.core.api.registry.SourceInfo;
import org.openclover.core.context.ContextStore;
import org.openclover.core.registry.Clover2Registry;
import org.openclover.core.registry.ReadOnlyRegistryException;
import org.openclover.core.registry.RegistryUpdate;
import org.openclover.core.registry.entities.BasicElementInfo;
import org.openclover.core.registry.entities.BasicMethodInfo;
import org.openclover.core.registry.entities.FullBranchInfo;
import org.openclover.core.registry.entities.FullClassInfo;
import org.openclover.core.registry.entities.FullFileInfo;
import org.openclover.core.registry.entities.FullMethodInfo;
import org.openclover.core.registry.entities.FullPackageInfo;
import org.openclover.core.registry.entities.FullStatementInfo;
import org.openclover.core.registry.entities.MethodSignature;
import org.openclover.core.registry.entities.Modifiers;
import org.openclover.core.spi.lang.LanguageConstruct;
import org.openclover.runtime.Logger;
import org.openclover.runtime.api.CloverException;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.openclover.core.util.Lists.newArrayList;
import static org.openclover.core.util.Lists.newLinkedList;
import static org.openclover.core.util.Maps.newHashMap;

public class InstrumentationSessionImpl implements InstrumentationSession {

    static class ClassEntityVisitor extends EntityVisitor {
        private AtomicReference<FullClassInfo> classFound;

        ClassEntityVisitor(AtomicReference<FullClassInfo> storage) {
            classFound = storage;
        }

        @Override
        public void visitClass(final ClassInfo currentClass) {
            classFound.set((FullClassInfo) currentClass);
        }
    }

    static class MethodEntityVisitor extends EntityVisitor {
        private AtomicReference<FullMethodInfo> methodFound;

        MethodEntityVisitor(AtomicReference<FullMethodInfo> storage) {
            methodFound = storage;
        }

        @Override
        public void visitMethod(final MethodInfo methodInfo) {
            methodFound.set((FullMethodInfo) methodInfo);
        }
    }


    /** The registry this instrumentation session is for */
    private final Clover2Registry reg;
    /** The version of the registry at the start of the instrumentation session */
    private final long startVersion;
    /**
     * Entity containers (class/method). We use it to set proper parent for inner functions or classes
     */
    private final List<EntityContainer> parentStack = newArrayList();
    /** Packages seen in this session */
    private final Map<String, SessionPackageInfo> changedPackages;
    /** When instrumentation started */
    private final long startTS;
    /** The version used during the session; is > startVersion */
    private final long version;
    /** When instrumentation ended - initially same as startTS */
    private long endTS;
    /** Package connected with the file currently being instrumented */
    private SessionPackageInfo currentPackage;
    /** File currently being instrumented */
    private FullFileInfo currentFile;
    /** The absolute index in the model for the file currently being instrumented */
    private int currentFileIndex;
    /** The index of the element last instrumented relative to the current file's absolute index */
    private int currentOffsetFromFile;
    /** Text encoding currently in use for instrumentation */
    private String activeEncoding;
    private int nextIndexForNewFile;

    public InstrumentationSessionImpl(Clover2Registry reg, String activeEncoding) throws CloverException {
        this.activeEncoding = activeEncoding;
        if (reg.isReadOnly()) {
            throw new ReadOnlyRegistryException();
        }

        this.reg = reg;
        this.startVersion = reg.getVersion();
        this.changedPackages = newHashMap();
        this.currentFileIndex = reg.getProject().getDataLength();
        this.nextIndexForNewFile = this.currentFileIndex;
        this.startTS = this.endTS = System.currentTimeMillis();
        //IMPORTANT! Ensure version is always increasing
        this.version = this.startTS > this.startVersion ? this.startTS : this.startVersion + 1;
        this.currentOffsetFromFile = 0;
    }

    public RegistryUpdate finishAndApply() throws ConcurrentInstrumentationException {
        return reg.applyUpdate(startVersion, finish());
    }

    public Update finish() {
        endTS = System.currentTimeMillis();

        if (currentPackage != null) {
            exitPackage();
        }

        return
            new Update(
                version,
                startTS,
                System.currentTimeMillis(),
                nextIndexForNewFile,
                toPackages(changedPackages.values()),
                reg.getContextStore());
    }

    private Collection<PackageInfo> toPackages(Collection<SessionPackageInfo> shadowPackageInfos) {
        List<PackageInfo> pkgInfos = newLinkedList();
        for (SessionPackageInfo shadowPackageInfo : shadowPackageInfos) {
            pkgInfos.add(shadowPackageInfo.getSessionPkg());
        }
        return pkgInfos;
    }

    @Override
    public int getCurrentIndex() {
        return currentFileIndex;
    }

    @Override
    public int getCurrentFileMaxIndex() {
        return currentFileIndex + currentOffsetFromFile;
    }

    @Override
    public int getCurrentOffsetFromFile() {
        return currentOffsetFromFile;
    }

    public Clover2Registry getRegistry() {
        return reg;
    }

    @Override
    public FullFileInfo enterFile(
        String packageName, File file, int lineCount, int ncLineCount,
        long timestamp, long filesize, long checksum) {

        currentOffsetFromFile = 0;

        enterPackage(packageName);
        FullFileInfo finfo = (FullFileInfo) currentPackage.getFileInPackage(file.getName());
        long minVersion = FullFileInfo.NO_VERSION;

        if (finfo != null) {
            if (finfo.getChecksum() == checksum && finfo.getFilesize() == filesize) {

                // can reuse the slots
                currentFileIndex = finfo.getDataIndex();
                minVersion = finfo.getMinVersion();
            } else {
                // different version of file we've seen previously
                // todo - the slot for the old version of the file becomes unused - could potentially reuse
                //  the range in the data array.
                // for the moment, put changed file at the end
                currentFileIndex = nextIndexForNewFile;
            }
        } else {
            //a new file record gets slots at the end of the data array
            currentFileIndex = nextIndexForNewFile;
        }

        finfo =
            new FullFileInfo(
                currentPackage.getSessionPkg(), file, activeEncoding, currentFileIndex,
                lineCount, ncLineCount, timestamp, filesize, checksum, version);

        if (minVersion != FullFileInfo.NO_VERSION) {
            finfo.addVersions(minVersion, startTS);
        }

        currentFile = finfo;
        return finfo;
    }

    @Override
    public void exitFile() {
        currentFile.setDataLength(currentOffsetFromFile);
        currentPackage.addFile(currentFile);
        //If we're revisiting an unchanged file, next index is unchanged otherwise, it's at the end of the changed file
        nextIndexForNewFile = Math.max(currentFileIndex + currentOffsetFromFile, nextIndexForNewFile);
        //Package length is either current package length if we revisited an unchanged file or the end of the changed file
        currentPackage.setDataLength(Math.max(currentPackage.getDataLength(), currentFileIndex + currentOffsetFromFile - currentPackage.getDataIndex()));
        currentFile = null;
    }

    @Override
    public FullClassInfo enterClass(
            String name, SourceInfo region, ModifiersInfo modifiers, boolean isInterface,
            boolean isEnum, boolean isAnnotation) {

        FullClassInfo clazz = new FullClassInfo(
                currentPackage.getSessionPkg(), currentFile,
                currentOffsetFromFile, name, region, new Modifiers(modifiers),
                isInterface, isEnum, isAnnotation);
        currentFile.addClass(clazz);
        pushCurrentClass(clazz);
        return clazz;
    }

    @Override
    public ClassInfo exitClass(int endLine, int endCol) {
        FullClassInfo clazz = popCurrentClass();
        clazz.setRegionEnd(endLine, endCol);
        clazz.setDataLength(currentOffsetFromFile - clazz.getRelativeDataIndex());

        // increase aggregatedStatements for parent class if not null (case for inner classes)
        // or do nothing (case for top-level classes)
        // note: anonymous inline classes do not have ClassInfo, so getCurrentMethod().increaseAggregatedStatementCount()
        // is not called - this case it already handled in exitMethod() code
        FullClassInfo currentClass = getCurrentClass();
        if (currentClass != null) {
            currentClass.increaseAggregatedStatements(clazz.getAggregatedStatementCount());
            currentClass.increaseAggregatedComplexity(clazz.getAggregatedComplexity());
        }

        return getCurrentClass();
    }

    public FullMethodInfo enterMethod(ContextSet context, SourceInfo region, MethodSignature signature,
                                      boolean isTest, int complexity) {
        return enterMethod(context, region, signature, isTest, null, false, complexity, LanguageConstruct.Builtin.METHOD);
    }

    @Override
    public FullMethodInfo enterMethod(@NotNull final ContextSet context, @NotNull SourceInfo region,
                                      @NotNull MethodSignature signature,
                                      boolean isTest, @Nullable String staticTestName,
                                      boolean isLambda, int complexity, @NotNull LanguageConstruct construct) {

        final BasicMethodInfo basicMethodInfo = new BasicMethodInfo(region, currentOffsetFromFile, complexity,
                signature, isTest, staticTestName, isLambda, construct);
        final AtomicReference<FullMethodInfo> method = new AtomicReference<>();

        getCurrentContainer().visit(new EntityVisitor() {
            @Override
            public void visitClass(ClassInfo parentClass) {
                // create method with a class as parent (standard methods)
                method.set(new FullMethodInfo((FullClassInfo) parentClass, context, basicMethodInfo));
            }

            @Override
            public void visitMethod(MethodInfo parentMethod) {
                // create method with a method as parent (inner functions)
                method.set(new FullMethodInfo((FullMethodInfo) parentMethod, context, basicMethodInfo));
            }
        });

        currentOffsetFromFile += method.get().getDataLength();
        pushCurrentMethod(method.get());
        return method.get();
    }

    public FullMethodInfo enterMethod(ContextSet context, SourceInfo region, MethodSignature signature, boolean test) {
        return enterMethod(context, region, signature, test, FullMethodInfo.DEFAULT_METHOD_COMPLEXITY);
    }

    @Override
    public void exitMethod(int endLine, int endCol) {
        final FullMethodInfo method = popCurrentMethod();
        method.setRegionEnd(endLine, endCol);
        method.setDataLength(currentOffsetFromFile - method.getRelativeDataIndex());

        // increase value of aggregated statements for current method and its parent
        final int statementCount = method.getRawMetrics().getNumStatements();
        final int complexity = method.getRawMetrics().getComplexity();
        method.increaseAggregatedStatementCount(statementCount);
        method.increaseAggregatedComplexity(complexity);

        getCurrentContainer().visit(new EntityVisitor() {
            @Override
            public void visitMethod(MethodInfo parentMethod) {
                FullMethodInfo fullParentMethod = (FullMethodInfo) parentMethod;
                // TODO remove this 'if !isLambda' workaround and implement ClassInfo for anonymous inline classes
                if (!method.isLambda()) {
                    // we're inside the method of an anonymous inline class; in such case increase
                    // aggregatedStatements for "parent" method, because anonymous classes do not have their own ClassInfo
                    fullParentMethod.increaseAggregatedStatementCount(method.getAggregatedStatementCount());
                    fullParentMethod.increaseAggregatedComplexity(method.getAggregatedComplexity());

                    // add anonymous class function to the parent class, not to the current method
                    getCurrentClass().addMethod(method);
                } else {
                    // we're inside lambda, no need to increase aggregated complexity, lambdas inside methods are
                    // already increasing the 'normal' complexity metric
                    fullParentMethod.addMethod(method);
                }
            }

            @Override
            public void visitClass(ClassInfo parentClass) {
                // otherwise we're leaving method inside top-level or inner class, increase value in ClassInfo
                FullClassInfo fullParentClass = (FullClassInfo)parentClass;
                fullParentClass.increaseAggregatedStatements(method.getAggregatedStatementCount());
                fullParentClass.increaseAggregatedComplexity(method.getAggregatedComplexity());
                fullParentClass.addMethod(method);
            }
        });
    }

    public FullStatementInfo addStatement(
        ContextSet context, SourceInfo region, int complexity) {
        return addStatement(context, region, complexity, LanguageConstruct.Builtin.STATEMENT);
    }

    @Override
    public FullStatementInfo addStatement(
        ContextSet context, SourceInfo region, int complexity, LanguageConstruct construct) {
        final FullStatementInfo stmt;
        final BasicElementInfo stmtBase = new BasicElementInfo(region, currentOffsetFromFile, complexity, construct);

        // Add a statement to the enclosing method, class or a file
        final FullMethodInfo currentMethod = getCurrentMethod();
        if (currentMethod == null) {
            // Print this warning because no language supported by Clover has statements outside methods
            // TODO Remove this (or set to verbose) as soon as we publish Service Provider Interface
            Logger.getInstance().warn("Trying to add a statement but current method is null. "
                    + "Did you put CLOVER:OFF before a method signature and CLOVER:ON inside a method body?");

            final FullClassInfo currentClass = getCurrentClass();
            if (currentClass == null) {
                final FullFileInfo currentFile = getCurrentFile();
                if (currentFile == null) {
                    throw new IllegalStateException("Trying to add a statement but current method/class/file are null");
                } else {
                    stmt = new FullStatementInfo(currentFile, context, stmtBase);
                    currentFile.addStatement(stmt);
                }
            } else {
                stmt = new FullStatementInfo(currentClass, context, stmtBase);
                currentClass.addStatement(stmt);
            }
        } else {
            stmt = new FullStatementInfo(currentMethod, context, stmtBase);
            currentMethod.addStatement(stmt);
        }

        currentOffsetFromFile += stmt.getDataLength();
        return stmt;
    }

    public FullBranchInfo addBranch(
        ContextSet context, SourceInfo region, boolean instrumented, int complexity) {
        return addBranch(context, region, instrumented, complexity, LanguageConstruct.Builtin.BRANCH);
    }

    @Override
    public FullBranchInfo addBranch(
        ContextSet context, SourceInfo region, boolean instrumented, int complexity, LanguageConstruct construct) {

        FullMethodInfo currentMethod = getCurrentMethod();
        FullBranchInfo branch = null;
        // TODO add handling of branches in a class and in a file
        if (currentMethod != null) {  // HACK - see CCD-317. ternary operators can occur outside methods
            branch = new FullBranchInfo(currentMethod, currentOffsetFromFile, context, region, complexity, instrumented, construct);
            currentOffsetFromFile += branch.getDataLength();
            currentMethod.addBranch(branch);
        }
        return branch;
    }

    @Override
    public void setSourceEncoding(String encoding) {
        this.activeEncoding = encoding;
    }

    @Override
    public PackageInfo enterPackage(String name) {
        if (currentPackage != null) {
            if (currentPackage.isNamed(name)) {
                // aready in this package
                return currentPackage.getSessionPkg();
            }
            exitPackage();
        }

        //Have we already instrumented a package for this file in this session?
        SessionPackageInfo pkg = changedPackages.get(name);
        if (pkg == null) {
            //It's the first time we've seen it this session
            final PackageInfo modelPkg = reg.getProject().getNamedPackage(name);
            pkg = new SessionPackageInfo(
                modelPkg,
                new FullPackageInfo(
                    reg.getProject(),
                    name,
                    modelPkg == null ? currentFileIndex : modelPkg.getDataIndex()));
        }
        currentPackage = pkg;
        
        return currentPackage.getSessionPkg();
    }

    @Override
    public void exitPackage() {
        changedPackages.put(currentPackage.getName(), currentPackage);
        currentPackage = null;
    }

    @Override
    public FullFileInfo getCurrentFile() {
        return currentFile;
    }

    @Override
    public PackageInfo getCurrentPackage() {
        return currentPackage.getSessionPkg();
    }

    @Override
    @Nullable
    public FullClassInfo getCurrentClass() {
        final AtomicReference<FullClassInfo> classFound = new AtomicReference<>(null);
        final EntityVisitor classVisitor = new ClassEntityVisitor(classFound);

        // look backwards, i.e. from top of the stack
        for (int i = parentStack.size() - 1; i >= 0 && classFound.get() == null; i--) {
            parentStack.get(i).visit(classVisitor);
        }

        // return first class found (or null)
        return classFound.get();
    }

    public void pushCurrentClass(ClassInfo clazz) {
        parentStack.add(clazz);
    }

    public FullClassInfo popCurrentClass() throws IllegalStateException {
        // integrity check - ensure that stack is not empty
        if (parentStack.isEmpty()) {
            throw new IllegalStateException("Trying to pop FullClassInfo but the stack is empty");
        }

        // integrity check - ensure that we pop a class
        final AtomicReference<FullClassInfo> classFound = new AtomicReference<>(null);
        final EntityVisitor classVisitor = new ClassEntityVisitor(classFound);
        int lastIndex = parentStack.size() - 1;
        parentStack.get(lastIndex).visit(classVisitor);
        if (classFound.get() == null) {
            throw new IllegalStateException("Trying to pop FullClassInfo but found "
                    + parentStack.get(lastIndex).getClass().getSimpleName() + " on the stack");
        }

        // ok, pop this class
        parentStack.remove(lastIndex); // remove last
        return classFound.get();
    }

    @Override
    @Nullable
    public FullMethodInfo getCurrentMethod() {
        final AtomicReference<FullMethodInfo> methodFound = new AtomicReference<>(null);
        final EntityVisitor methodVisitor = new MethodEntityVisitor(methodFound);

        // look backwards, i.e. from top of the stack
        for (int i = parentStack.size() - 1; i >= 0 && methodFound.get() == null; i--) {
            parentStack.get(i).visit(methodVisitor);
        }

        // return first method found (or null)
        return methodFound.get();
    }

    public void pushCurrentMethod(FullMethodInfo clazz) {
        parentStack.add(clazz);
    }

    public FullMethodInfo popCurrentMethod() throws IllegalStateException {
        // integrity check - ensure that stack is not empty
        if (parentStack.isEmpty()) {
            throw new IllegalStateException("Trying to pop FullMethodInfo but the stack is empty");
        }

        // integrity check - ensure that we pop a class
        final AtomicReference<FullMethodInfo> methodFound = new AtomicReference<>(null);
        final EntityVisitor methodVisitor = new MethodEntityVisitor(methodFound);
        int lastIndex = parentStack.size() - 1;
        parentStack.get(lastIndex).visit(methodVisitor);
        if (methodFound.get() == null) {
            throw new IllegalStateException("Trying to pop FullMethodInfo but found "
                    + parentStack.get(lastIndex).getClass().getSimpleName() + " on the stack");
        }

        // ok, pop this method
        parentStack.remove(lastIndex);
        return methodFound.get();
    }

    public EntityContainer getCurrentContainer() {
        return parentStack.isEmpty() ? null : parentStack.get(parentStack.size() - 1);
    }

    @Override
    public void close() throws ConcurrentInstrumentationException {
        finishAndApply();
    }

    @Override
    public long getStartTs() {
        return startTS;
    }

    @Override
    public long getEndTS() {
        return endTS;
    }

    @Override
    public long getVersion() {
        return version;
    }

    public static class Update implements RegistryUpdate {
        private final long version;
        private final long startTS;
        private final long endTS;
        private final int slotCount;
        private final Collection<PackageInfo> changedPkgInfos;
        private final ContextStore ctxStore;
        private final List<FileInfo> fileInfos;

        public Update(long version, long startTS, long endTS, int slotCount, Collection<PackageInfo> changedPkgInfos, ContextStore ctxStore) {
            this.version = version;
            this.startTS = startTS;
            this.endTS = endTS;
            this.slotCount = slotCount;
            this.changedPkgInfos = changedPkgInfos;
            this.ctxStore = ctxStore;
            this.fileInfos = collectFileInfos();
        }

        @SuppressWarnings("unchecked")
        private List<FileInfo> collectFileInfos() {
            List<FileInfo> fileInfos = newLinkedList();
            for (PackageInfo newPkgInfo : changedPkgInfos) {
                fileInfos.addAll(newPkgInfo.getFiles());
            }
            return fileInfos;
        }

        @Override
        public long getVersion() {
            return version;
        }

        @Override
        public long getStartTs() {
            return startTS;
        }

        @Override
        public long getEndTs() {
            return endTS;
        }

        @Override
        public int getSlotCount() {
            return slotCount;
        }

        @Override
        public List<FileInfo> getFileInfos() {
            return fileInfos;
        }

        public Collection<PackageInfo> getChangedPkgInfos() {
            return changedPkgInfos;
        }

        @Override
        public ContextStore getContextStore() {
            return ctxStore;
        }
    }

    /** Tracks packages created in the current session and their counterparts in the model, if one exists */
    public static class SessionPackageInfo {
        private PackageInfo modelPkg;
        private PackageInfo sessionPkg;

        public SessionPackageInfo(PackageInfo modelPkg, PackageInfo sessionPkg) {
            this.modelPkg = modelPkg;
            this.sessionPkg = sessionPkg;
        }

        public FileInfo getFileInPackage(String name) {
            FileInfo fileInfo = sessionPkg.getFileInPackage(name);
            if (fileInfo == null) {
                fileInfo = modelPkg == null ? null : modelPkg.getFileInPackage(name);
            }
            return fileInfo;
        }

        public PackageInfo getModelPkg() {
            return modelPkg;
        }

        public PackageInfo getSessionPkg() {
            return sessionPkg;
        }

        public int getDataIndex() {
            return sessionPkg.getDataIndex();
        }

        public int getDataLength() {
            return sessionPkg.getDataLength();
        }

        public void setDataLength(int len) {
            sessionPkg.setDataLength(len);
        }

        public void addFile(FullFileInfo currentFile) {
            sessionPkg.addFile(currentFile);
        }

        public boolean isNamed(String name) {
            return sessionPkg.isNamed(name);
        }

        public String getName() {
            return sessionPkg.getName();
        }
    }
}
