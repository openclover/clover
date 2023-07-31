package com.atlassian.clover.registry;

import com.atlassian.clover.registry.entities.BasePackageInfo;
import com.atlassian.clover.registry.entities.FullFileInfo;
import com.atlassian.clover.registry.entities.FullPackageInfo;
import com.atlassian.clover.registry.entities.FullProjectInfo;
import com.atlassian.clover.registry.metrics.HasMetricsFilter;
import com.atlassian.clover.api.instrumentation.ConcurrentInstrumentationException;
import com.atlassian.clover.api.instrumentation.InstrumentationSession;
import com.atlassian.clover.instr.InstrumentationSessionImpl;
import com.atlassian.clover.api.registry.CloverRegistryException;
import com.atlassian.clover.registry.format.CoverageSegment;
import com.atlassian.clover.registry.format.FileInfoRecord;
import com.atlassian.clover.registry.format.FreshRegFile;
import com.atlassian.clover.registry.format.InstrSessionSegment;
import com.atlassian.clover.registry.format.RegAccessMode;
import com.atlassian.clover.registry.format.RegContents;
import com.atlassian.clover.registry.format.RegContentsConsumer;
import com.atlassian.clover.registry.format.RegFile;
import com.atlassian.clover.registry.format.UpdatableRegFile;
import com.atlassian.clover.api.CloverException;
import com.atlassian.clover.CoverageData;
import com.atlassian.clover.Logger;
import com.atlassian.clover.ProgressListener;
import com.atlassian.clover.context.ContextStore;
import com.atlassian.clover.util.CloverUtils;
import com.atlassian.clover.util.FileUtils;
import com.atlassian.clover.util.Path;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.openclover.util.Lists.newLinkedList;
import static org.openclover.util.Maps.newHashMap;


public class Clover2Registry implements InstrumentationTarget {
    private final ProjectView.Original model;
    private final LinkedList<InstrumentationInfo> instrumentationHistory;
    private final List<InstrumentationSessionImpl.Update> updatesToSave;

    private volatile RegFile regFile;
    private ContextStore contexts;
    private CoverageData coverageData;

    /**
     * creates a fresh registry with no elements in it
     * @param regFile  the file that this registry will be written to when store() is called
     * @param name the project name to associate with this registry
     */
    public Clover2Registry(File regFile, String name) {
        this(regFile, RegAccessMode.READWRITE, name);
    }

    public Clover2Registry(File regFile, RegAccessMode accessMode, String name)  {
        this(
                new FreshRegFile(regFile, accessMode, name),
                new FullProjectInfo(name),
                new ArrayList<InstrumentationInfo>(),
                new ContextStore());
    }

    Clover2Registry(RegFile regFile, FullProjectInfo model, List<InstrumentationInfo> instrumentationHistory, ContextStore contexts) {
        this.regFile = regFile;
        this.model = new ProjectView.Original(model);
        this.instrumentationHistory = newLinkedList(instrumentationHistory);
        this.contexts = contexts;
        this.updatesToSave = new CopyOnWriteArrayList<>();
    }

    public Clover2Registry copyForBackgroundCoverageLoad() {
        return new Clover2Registry(regFile, model.getProject().copy(), instrumentationHistory, contexts);
    }

    public static Clover2Registry fromInitString(String initstring, String name) throws CloverException {
        File regFile = new File(initstring);
        Clover2Registry reg = fromFile(regFile);
        if (reg == null) {
            reg = new Clover2Registry(regFile, name);
        }
        return reg;
    }

    public static Clover2Registry fromFile(File registryFile) throws CloverException {
        return fromFile(registryFile, null, null);
    }

    @SuppressWarnings("unchecked")
    public static Clover2Registry fromFile(final File registryFile, final HasMetricsFilter filter, ProgressListener progressListener) throws CloverException {
        try {
            final UpdatableRegFile regFile = new UpdatableRegFile(registryFile);
            final List<InstrumentationInfo> instrHistory = newLinkedList();
            final FullProjectInfo projInfo = new FullProjectInfo(regFile.getName(), regFile.getVersion());
            final Map<String, FullFileInfo> fileInfos = newHashMap();
            final long version = regFile.getVersion();

            final Clover2Registry[] resultReg = new Clover2Registry[1];
            regFile.readContents(new RegContentsConsumer() {
                @Override
                public void consume(RegContents contents) {
                    ContextStore ctxStore = null;

                    //Sessions are ordered newest to oldest
                    for (InstrSessionSegment sessionSegment : contents.getSessions()) {
                        //Grab first context store ie the most recent store from the most recent session
                        ctxStore = ctxStore == null ? sessionSegment.getCtxStore() : ctxStore;

                        final Collection<FileInfoRecord> fileInfoRecs = sessionSegment.getFileInfoRecords();

                        instrHistory.add(
                            new InstrumentationInfo(
                                sessionSegment.getVersion(),
                                sessionSegment.getStartTs(),
                                sessionSegment.getEndTs()));

                        buildModel(version, filter, projInfo, fileInfos, sessionSegment, fileInfoRecs);
                    }

                    recreateDataIndicesAndLengths(regFile, projInfo);

                    Clover2Registry reg = new Clover2Registry(regFile, projInfo, instrHistory, ctxStore);

                    final CoverageSegment coverage = contents.getCoverage();
                    if (coverage != null) {
                        final CoverageData covData = new CoverageData(reg.getVersion(), coverage.getHitCounts(), coverage.getPerTestCoverage());
                        reg.setCoverageData(covData);
                        reg.getProject().setDataProvider(covData);
                    }

                    resultReg[0] = reg;
                }
            });

            return resultReg[0];
        } catch (RuntimeException | IOException e) {
            Logger.getInstance().debug("Exception reading registry file " + registryFile.getAbsolutePath(), e);
            throw new CorruptedRegistryException(registryFile.getAbsolutePath(), e);
        } catch (NoSuchRegistryException e) {
            //IMPORTANT: If the reg file doesn't exist, return null as this tells
            //other code paths to create a new in-memory registry
            return null;
        }
    }

    private static void buildModel(long version, HasMetricsFilter filter, final FullProjectInfo projInfo, Map<String, FullFileInfo> fileInfos, InstrSessionSegment sessionSegment, Collection<FileInfoRecord> fileInfoRecs) {
        class FosterPackageInfo extends BasePackageInfo {
            public String fosterName;

            FosterPackageInfo() {
                super(projInfo, "");
            }

            @Override
            public String getName() {
                return fosterName;
            }

            @Override
            public String getPath() {
                return CloverUtils.packageNameToPath(fosterName, isDefault());
            }

            @Override
            public boolean isDefault() {
                return isDefaultName(fosterName);
            }

            public FullFileInfo adopt(String fosterName, FullFileInfo fileInfo) {
                this.fosterName = fosterName;
                fileInfo.setContainingPackage(this);
                return fileInfo;
            }
        }

        final FosterPackageInfo surrogatePackage = new FosterPackageInfo();

        for (FileInfoRecord fileInfoRec : fileInfoRecs) {
            final String pkgName = fileInfoRec.getPackageName();
            final String filePath = fileInfoRec.getName() + "@" + pkgName;
            //If FileInfo not loaded from a previous instrumentation session...
            if (!fileInfos.containsKey(filePath)) {
                //Temporarily adopt the FileInfo so things like getPackagePath() work while filtering
                final FullFileInfo fileInfo = surrogatePackage.adopt(pkgName, fileInfoRec.getFileInfo());

                //If not filtered out
                if (filter == null || filter.accept(fileInfo)) {
                    fileInfos.put(filePath, fileInfo);
                    //Make the FileInfo support the very latest model version
                    fileInfo.addVersion(version);
                    FullPackageInfo pkgInfo = (FullPackageInfo)projInfo.getNamedPackage(pkgName);
                    if (pkgInfo == null) {
                        pkgInfo = new FullPackageInfo(projInfo, pkgName, Integer.MAX_VALUE);
                        projInfo.addPackage(pkgInfo);
                    }

                    pkgInfo.addFile(fileInfo);
                }
            } else {
                fileInfos.get(filePath).addVersion(sessionSegment.getVersion());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void recreateDataIndicesAndLengths(UpdatableRegFile regFile, FullProjectInfo projInfo) {
        int projLen = Integer.MIN_VALUE;
        for (FullPackageInfo pkgInfo : (List<FullPackageInfo>)projInfo.getAllPackages()) {
            int pkgStartIdx = Integer.MAX_VALUE;
            int pkgEndIdx = Integer.MIN_VALUE;
            for (FullFileInfo fileInfo : (List<FullFileInfo>)pkgInfo.getFiles()) {
                pkgStartIdx = Math.min(pkgStartIdx, fileInfo.getDataIndex());
                pkgEndIdx = Math.max(pkgEndIdx, fileInfo.getDataIndex() + fileInfo.getDataLength());
            }
            pkgInfo.setDataIndex(pkgStartIdx);
            pkgInfo.setDataLength(pkgEndIdx - pkgStartIdx);

            projLen = Math.max(projLen, pkgEndIdx);
        }

        //Data length
        projInfo.setDataLength(Math.max(projLen, regFile.getSlotCount()));
    }

    public static Clover2Registry createOrLoad(File registryFile, String projectName) throws IOException, CloverException {
        Clover2Registry registry;

        if (registryFile.exists()) {
            Logger.getInstance().info("Updating existing database at '" + registryFile + "'.");
            registry = Clover2Registry.fromFile(registryFile);
        }
        else {
            Logger.getInstance().info("Creating new database at '" + registryFile + "'.");
            File parentDir = registryFile.getParentFile();
            if (parentDir != null) {
                parentDir.mkdirs();
            }
            registry = new Clover2Registry(registryFile, projectName);
        }
        return registry;
    }

    public RegFile saveAndOverwriteFile() throws IOException, CloverRegistryException {
        return saveAndOverwriteFile(model.getProject(), instrumentationHistory, contexts, coverageData);
    }

    @SuppressWarnings("unchecked")
    protected RegFile saveAndOverwriteFile(FullProjectInfo project, List<InstrumentationInfo> instrumentationHistory, ContextStore contexts, CoverageData coverageData) throws IOException, CloverRegistryException {
        RegFile regFile = new FreshRegFile(this.regFile, coverageData);

        List<RegistryUpdate> updates = newLinkedList();
        //To preserve some semblence of history when writing as a new file,
        //add empty projects updates to record instrumentation history
        //This may go away in the future if we only ever append.
        for(int i = 0; i < instrumentationHistory.size() - 1; i++) {
            final InstrumentationInfo instrInfo = instrumentationHistory.get(i);
            updates.add(new EmptyProjectUpdate(instrInfo.getVersion(), instrInfo.getStartTS(), instrInfo.getEndTS(), project.getDataLength()));
        }

        long startTs = System.currentTimeMillis();
        long endTs = startTs;
        if (!instrumentationHistory.isEmpty()) {
            startTs = instrumentationHistory.get(instrumentationHistory.size() - 1).getStartTS();
            endTs = instrumentationHistory.get(instrumentationHistory.size() - 1).getEndTS();
        }
        updates.add(new FullProjectUpdate(project, contexts, startTs, endTs));

        this.regFile = regFile.save(updates);

        return regFile;
    }

    public RegFile saveAndAppendToFile() throws IOException, CloverRegistryException {
        if (regFile.isAppendable()) {
            this.regFile = regFile.save(updatesToSave);
        } else {
            saveAndOverwriteFile();
        }
        updatesToSave.clear();

        return regFile;
    }

    /**
     * Apply changes to the model and then append the changes to the registry on disk
     *
     * @return ExistingRegFile the new registry file representation suitable for further appending
     * of updates
     **/
    public UpdatableRegFile applyAndAppendToFile(UpdatableRegFile regFile, InstrumentationSessionImpl.Update update) throws IOException, CloverRegistryException {
        return regFile.save(applyUpdate(regFile.getVersion(), update));
    }

    /**
     * Applies the instrumentation session update to the registry. This method is not threadsafe and
     * should be called when interim model changes will not be visible to the environment.
     *
     * @throws ConcurrentInstrumentationException if another instrumentation session update
     * was applied between the start and application of the supplied instrumentation session
     * update.
     */
    @Override
    @SuppressWarnings("unchecked")
    public RegistryUpdate applyUpdate(long expectedVersion, InstrumentationSessionImpl.Update update) throws ConcurrentInstrumentationException {
        model.applyUpdate(expectedVersion, update);
        instrumentationHistory.addFirst(new InstrumentationInfo(update.getVersion(), update.getStartTs(), update.getEndTs()));
        updatesToSave.add(update);
        return update;
    }

    public ProjectView.Filtered newProjectView(HasMetricsFilter.Invertable filter) {
        return model.newProjection(filter);
    }

    public InstrumentationSession startInstr() throws CloverException {
        return startInstr(null);
    }

    public InstrumentationSession startInstr(String encoding) throws CloverException {
        return new InstrumentationSessionImpl(this, encoding);
    }

    public boolean fileExists() {
        return regFile.getFile().exists();
    }

    public boolean isOutOfDate() {
        final File file = getRegistryFile();
        return file.lastModified() == 0 || FileUtils.getInstance().compareLastModified(getVersion(), file) < 0;
    }

    public File getRegistryFile() {
        return regFile.getFile();
    }

    public String getProjectName() {
        return model.getProject().getName();
    }

    public void setProjectName(String name) {
        model.getProject().setName(name);
    }

    public long getVersion() {
        return model.getVersion();
    }

    public long getFirstVersion() {
        return
            instrumentationHistory.isEmpty()
                ? getVersion()
                : instrumentationHistory.get(instrumentationHistory.size() - 1).getVersion();
    }

    public void setVersion(long version) {
        model.setVersion(version);
    }

    public ProjectView.Original getModel() {
        return model;
    }

    public boolean isReadOnly() {
        return regFile.getAccessMode() == RegAccessMode.READONLY;
    }

    public void setCoverageData(CoverageData data) {
        coverageData = data;
    }

    public CoverageData getCoverageData() {
        return coverageData;
    }

    public int getDataLength() {
        return model.getProject().getDataLength();
    }

    public FullProjectInfo getProject() {
        return model.getProject();
    }

    public List getInstrHistory() {
        return instrumentationHistory;
    }

    public ContextStore getContextStore() {
        return contexts;
    }

    public void setContextStore(ContextStore contexts) {
        this.contexts = contexts;
    }

    public String getInitstring() {
        return getRegistryFile().getAbsolutePath();
    }

    public long getPastInstrTimestamp(int numPastInstrs) {
        long msec = 0;
        if (!instrumentationHistory.isEmpty()) {
            for (ListIterator history = instrumentationHistory.listIterator(instrumentationHistory.size() - 1); 0 < numPastInstrs-- && history.hasPrevious();) {
                msec = ((InstrumentationInfo) history.previous()).getEndTS();
            }
        }
        return msec;
    }

    public void resolve(Path sourcePath) {
        model.resolve(sourcePath);
    }

    /**
     * provides high level information about an instrumentation "session", where a session is defined as
     * a call to {@link Clover2Registry#startInstr}, zero or more instrumentation events
     * (e.g {@link com.atlassian.clover.instr.InstrumentationSessionImpl#enterFile} etc), then a call to {@link com.atlassian.clover.instr.InstrumentationSessionImpl#finishAndApply()}
     */
    public static class InstrumentationInfo {
        private long startTS = 0;
        private long endTS = 0;
        private long version = 0;

        public InstrumentationInfo(long version, long startTS, long endTS) {
            this.version = version;
            this.startTS = startTS;
            this.endTS = endTS;
        }

        public InstrumentationInfo(long startTS) {
            this.startTS = startTS;
        }

        public long getStartTS() {
            return startTS;
        }

        public void setStartTS(long startTS) {
            this.startTS = startTS;
        }

        public long getEndTS() {
            return endTS;
        }

        public void setEndTS(long endTS) {
            this.endTS = endTS;
        }

        public long getVersion() {
            return version;
        }

        @Override
        public String toString() {
            return "InstrumentationInfo{" +
                "startTS=" + startTS +
                ", endTS=" + endTS +
                ", version=" + version +
                '}';
        }
    }
}