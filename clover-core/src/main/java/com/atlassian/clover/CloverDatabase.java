package com.atlassian.clover;

import com.atlassian.clover.api.CloverException;
import com.atlassian.clover.recorder.InMemPerTestCoverage;
import com.atlassian.clover.recorder.PerTestCoverage;
import com.atlassian.clover.recorder.PerTestCoverageStrategy;
import com.atlassian.clover.registry.CorruptedRegistryException;
import com.atlassian.clover.registry.NoSuchRegistryException;
import com.atlassian.clover.registry.entities.FullFileInfo;
import com.atlassian.clover.registry.entities.FullPackageInfo;
import com.atlassian.clover.registry.entities.FullProjectInfo;
import com.atlassian.clover.registry.format.RegAccessMode;
import com.atlassian.clover.registry.format.RegHeader;
import com.atlassian.clover.cfg.Interval;
import com.atlassian.clover.context.ContextSet;
import com.atlassian.clover.context.ContextStore;
import com.atlassian.clover.registry.Clover2Registry;
import com.atlassian.clover.registry.CoverageDataProvider;
import com.atlassian.clover.registry.CoverageDataRange;
import com.atlassian.clover.registry.metrics.HasMetricsFilter;
import com.atlassian.clover.registry.ProjectView;
import com.atlassian.clover.registry.entities.TestCaseInfo;
import com.atlassian.clover.util.CloverUtils;
import com.atlassian.clover.util.FileUtils;
import com.atlassian.clover.util.Formatting;
import com.atlassian.clover.util.Path;

import java.io.File;
import java.io.IOException;
import java.util.BitSet;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * encapsulates a Clover2 registry + recording files.
 *
 */
public class CloverDatabase {

    private String initstring;

    private Clover2Registry registry;

    private CoverageDataCollator collator;

    private CoverageData data;

    private ProjectView testOnlyModel = ProjectView.NONE;

    private ProjectView appOnlyModel = ProjectView.NONE;

    public CloverDatabase(String initstring) throws CloverException {
        this(initstring, null, null);
    }

    public CloverDatabase(String initstring, HasMetricsFilter includeFilter, String name) throws CloverException {
        this(initstring, includeFilter, name, null);
    }

    public CloverDatabase(String initstring, HasMetricsFilter includeFilter, String name, String filterSpec) throws CloverException {
        this(initstring, includeFilter, name, filterSpec, null);
    }

    /**
     * create a new instance
     * @param initstring the location of the database to load
     * @param includeFilter a filter that is applied to the loading of the registry. It controls what elements are visible to the database
     * @param name an alternative name for the model
     * @param filterSpec the filter to use when excluding metrics
     * @param progressListener progress listener
     * @throws CloverException if something goes wrong.
     */
    public CloverDatabase(
        String initstring,
        HasMetricsFilter includeFilter,
        String name,
        String filterSpec, ProgressListener progressListener) throws CloverException {

        this.initstring = initstring;

        registry = Clover2Registry.fromFile(new File(initstring), includeFilter, progressListener);
        if (registry == null) {
            throw new NoSuchRegistryException(initstring);
        }

        if (registry.isReadOnly()) {
            // db is the result of a merge, use the built-in data if it exists
            data = registry.getCoverageData();
        }
        if (data == null) {
            // empty coverage data
            data = new CoverageData(registry);
        }
        collator = new CoverageDataCollator(registry);
        registry.getProject().setDataProvider(data);
        ContextSet filter = getContextSet(filterSpec);
        registry.getProject().setContextFilter(filter);


        if (name == null) {
            name = "Clover database " + Formatting.formatDate(new Date(registry.getVersion()));
        }

        registry.getProject().setName(name);
    }

    /** Constructs a new database with an empty registry */
    public CloverDatabase(
        File regFile,
        boolean readOnly,
        String name,
        ContextSet contextFilter,
        CoverageDataSpec spec) {

        initstring = regFile.getAbsolutePath();
        registry =
            new Clover2Registry(
                regFile, readOnly ? RegAccessMode.READONLY : RegAccessMode.READWRITE,
                name == null ? ("Clover database " + Formatting.formatDate(new Date(registry.getVersion()))) : name);
        data = new CoverageData(registry);
        collator = new CoverageDataCollator(registry, spec);
        registry.getProject().setDataProvider(data);
        registry.getProject().setContextFilter(contextFilter);
        registry.getProject().setHasTestResults(false);

        if (spec.getTestFilter() != null) {
            testOnlyModel = registry.newProjectView(spec.getTestFilter());
            appOnlyModel = registry.newProjectView(spec.getTestFilter().invert());
        } else {
            testOnlyModel = appOnlyModel = ProjectView.NONE;
        }
    }

    public CloverDatabase(Clover2Registry registry) {
        this.initstring = registry.getInitstring();
        this.registry = registry;
        this.data = registry.getCoverageData() == null ? new CoverageData(registry) : registry.getCoverageData();
        this.collator = new CoverageDataCollator(registry);
    }

    // prepare database copy suitable for background coverage load
    // Note that the copy is not usable until loadCoverageData() is called
    private CloverDatabase(CloverDatabase templateDb, Clover2Registry newRegistry) {
        registry = newRegistry;

        initstring = templateDb.initstring;
        data = templateDb.data; // data would be copied by loadCoverageData()
        collator = templateDb.collator.copyWithNewRegistry(newRegistry); // this reuses old filter
    }

    public CloverDatabase copyForBackgroundCoverageDataLoad() {
        return new CloverDatabase(this, registry.copyForBackgroundCoverageLoad());
    }

    public CoverageData loadCoverageData() throws CloverException {
        return loadCoverageData(new CoverageDataSpec());
    }

    public CoverageData loadCoverageData(CoverageDataSpec spec) throws CloverException {
        return loadCoverageData(spec, null);
    }

    public CoverageData loadCoverageData(CoverageDataSpec spec, ProgressListener progressListener) throws CloverException {
        if (!registry.isReadOnly()) {
            data = collator.loadCoverageData(data, spec, progressListener);
        }
        registry.getProject().setDataProvider(data);
        registry.getProject().setHasTestResults(data.getTests().size() > 0);

        if (spec.isResolve()) {
            // if requested, resolve the loaded data against the registry
            data.resolve(registry);
        }

        if (!spec.isPreserveTestCaseCache()) {
            // unless requested, delete the testcaseinfo cache.
            TestCaseInfo.Factory.reset();
        }

        if (spec.getTestFilter() != null) {
            testOnlyModel = registry.newProjectView(spec.getTestFilter());
            appOnlyModel = registry.newProjectView(spec.getTestFilter().invert());
        } else {
            testOnlyModel = appOnlyModel = ProjectView.NONE;
        }
        return data;
    }

    public String getInitstring() {
        return initstring;
    }

    public String getName() {
        return registry.getProject().getName();
    }

    public FullProjectInfo getModel(CodeType codeType) {
        switch(codeType) {
            case APPLICATION:
                return getAppOnlyModel();
            case TEST:
                return getTestOnlyModel();
            case ALL:
                return getFullModel();
            default:
                return getAppOnlyModel();
        }
    }

    public FullProjectInfo getTestOnlyModel() {
        return testOnlyModel.getProject();
    }

    public FullProjectInfo getAppOnlyModel() {
        return appOnlyModel == ProjectView.NONE ? registry.getProject() : appOnlyModel.getProject();
    }

    public FullProjectInfo getFullModel() {
        return registry.getProject();
    }

    public boolean isOutOfDate() {
        return isRegistryOutOfDate() || isCoverageOutOfDate();
    }

    public boolean isRegistryOutOfDate() {
        return registry.isOutOfDate();
    }

    public boolean isCoverageOutOfDate() {
        return collator.isOutOfDate();
    }

    public boolean isRecordingInProgress() {
        return isRecordingInProgress(registry.getRegistryFile());
    }

    public static boolean isRecordingInProgress(File pathToRegistry) {
        return new File(pathToRegistry.getAbsolutePath() + CloverNames.LIVEREC_SUFFIX).exists();
    }

    public void resolve(Path sourcePath) {
        registry.resolve(sourcePath);
        appOnlyModel.resolve(sourcePath);
        testOnlyModel.resolve(sourcePath);
    }

    public ContextSet getContextSet(String spec) {
        return registry.getContextStore().createContextSetFilter(spec == null ? "" : spec);
    }

    /**
     * Gets test hits for a given range - FileInfo, ClassInfo etc
     * @return a set of TestCaseInfos that hit the given receptor
     * @see CoverageDataProvider
     */
    public Set<TestCaseInfo> getTestHits(CoverageDataRange range) {
        return data.getTestsCovering(range);
    }

    public Map<TestCaseInfo, BitSet> mapTestsAndCoverageForFile(FullFileInfo fileInfo) {
        return data.mapTestsAndCoverageForFile(fileInfo);
    }

    public Clover2Registry getRegistry() {
        return registry;
    }

    public ContextStore getContextStore() {
        return registry.getContextStore();
    }

    public CoverageData getCoverageData() {
        return data;
    }

    public TestCaseInfo getTestCase(int id) {
        return data.getTestById(id);
    }

    public boolean hasCoverage() {
        return !data.isEmpty();
    }

    // todo - need to return a RecordingsInfo object here that gives more information.
    public long getRecordingTimestamp() {
        return data.getTimestamp();
    }

    public static CloverDatabase loadWithCoverage(String initString, CoverageDataSpec spec) throws CloverException {
        CloverDatabase database = new CloverDatabase(initString);
        database.loadCoverageData(spec);
        return database;
    }

    public static void merge(List<CloverDatabaseSpec> dbspecs, String initString) throws CloverException, IOException {
        merge(dbspecs, initString, ProgressListener.NOOP_LISTENER);
    }

    public static void merge(List<CloverDatabaseSpec> dbspecs, String initString, ProgressListener listener) throws CloverException, IOException {
       merge(dbspecs, initString, false, Interval.DEFAULT_SPAN, listener);
    }


    /**
     * merge a list of databases to a new database, specified by initString. if update is true and a database exists at initString, it
     * is included in the merge, using updateSpan for the span. If update is set to false, any existing database at initString is overwritten.
     * Progress is reported via listener
     * @param dbspecs   list of databases to merge
     * @param initString destination database. If this exists and update is set to true, will be merged
     * @param update if true, merge any existing database at initString
     * @param updateSpan if update is true, the span to use when merging any existing database
     * @param listener gets progress callbacks
     */
    public static void merge(List<CloverDatabaseSpec> dbspecs, String initString, boolean update, Interval updateSpan, ProgressListener listener) throws CloverException, IOException {

        String originalInitString = null;
        File tmpDb = null;
        if (update) {
            try {
                // read the header to confirm there is a database to load
                RegHeader.readFrom(new File(initString));
            }
            catch (IOException e) {
                // no valid db, so bail on update
                Logger.getInstance().verbose("No database to update at " + initString);
                update = false;
            }

            if (update) {
                originalInitString = initString;
                // add the original db to the list to be merged
                dbspecs.add(new CloverDatabaseSpec(originalInitString, updateSpan));
                // create a tmp db to merge to.
                tmpDb = File.createTempFile("clovermerge",".db");
                tmpDb.delete();
                initString = tmpDb.getAbsolutePath();
                listener.handleProgress("updating existing database at " + originalInitString, 0.0f);
            }
        }

        if (dbspecs.size() < 1) {
            throw new CloverException("need to specify a non-zero number of databases to merge");
        }

        Clover2Registry destReg = new Clover2Registry(new File(initString), RegAccessMode.READONLY, "Merged Project"); // todo - pass in a name

        destReg.setVersion(System.currentTimeMillis());
        final FullProjectInfo baseProject = destReg.getProject();

        // todo - sort here to process the biggest first - might be significant speed improvement
        Map<CloverDatabaseSpec, CloverDatabase> speccedDbs = new LinkedHashMap<>();

        // load all registries early, to allow context store merging
        for (CloverDatabaseSpec spec : dbspecs) {
            CloverDatabase mergingDb = null;
            try {
                mergingDb = new CloverDatabase(spec.getInitString());
            } catch (CorruptedRegistryException | NoSuchRegistryException e) {
                Logger.getInstance().info(String.format("File %s doesn't seem to be Clover database, ignoring it.", spec.getInitString()));
            }
            speccedDbs.put(spec, mergingDb);
        }

        ContextStore.ContextMapper contextMapper =
            ContextStore.mergeContextStores(destReg, speccedDbs.values());

        // new aggregate coverage data
        int [] mergedCoverage = null;
        InMemPerTestCoverage mergedSliceHits = null;
        int projectDataLength = 0;

        float progress = 0.0f;
        float progressInc = 0.8f / dbspecs.size();
        int slotsUsed = 0;

        TestCaseInfo.Factory.reset();

        Iterator speccedDbEntries = speccedDbs.entrySet().iterator();

        for (int i = 0; speccedDbEntries.hasNext(); i++) {
            Map.Entry entry = (Map.Entry) speccedDbEntries.next();
            CloverDatabaseSpec spec = (CloverDatabaseSpec)entry.getKey();
            CloverDatabase mergingDb =(CloverDatabase)entry.getValue();
            listener.handleProgress(
                "Merging database " + (i + 1) + " of " + dbspecs.size() + ": " + mergingDb.getInitstring(),
                progress);

            // load actual coverage data for this database
            CoverageData mergingData =
                mergingDb.loadCoverageData(
                    new CoverageDataSpec(null, spec.getSpan().getValueInMillis(), false, false, true, true, PerTestCoverageStrategy.IN_MEMORY));


            final FullProjectInfo mergingProject = mergingDb.getFullModel();

            if (mergedCoverage == null) {
                mergedCoverage = new int [mergingProject.getDataLength()];
                mergedSliceHits = new InMemPerTestCoverage(mergingDb.getRegistry());
            }

            // get all files from mergingProject
            List<FullFileInfo> mergingFiles = (List<FullFileInfo>)mergingProject.getFiles(HasMetricsFilter.ACCEPT_ALL);

            for (FullFileInfo mergeFI : mergingFiles) {
                FullFileInfo baseFI = null;
                String mergePkgName = mergeFI.getContainingPackage().getName();

                FullPackageInfo basePkg = (FullPackageInfo) baseProject.getNamedPackage(mergePkgName);
                if (basePkg != null) {
                    baseFI = (FullFileInfo) basePkg.getFile(mergeFI.getPackagePath());
                }
                // default new location is append at the end
                int newDataIndex = baseProject.getDataLength();
                int newDataLength = mergeFI.getDataLength();
                int oldDataIndex = mergeFI.getDataIndex();

                // we check filesize here because checksum can theoretically have collisions
                // which still is no guarantee, but hey.
                if (baseFI != null && baseFI.getFilesize() == mergeFI.getFilesize() &&
                        baseFI.getChecksum() == mergeFI.getChecksum()) {
                    // baseProject has an entry for this file, and the file records are identical, so
                    // need to merge data only
                    newDataIndex = baseFI.getDataIndex();
                    newDataLength = baseFI.getDataLength();
                } else if (baseFI == null || baseFI.getTimestamp() < mergeFI.getTimestamp()) {
                    // baseProject doesn't have this file, or mergingProject has a newer record and different checksum
                    // - add file record (and possibly the containing package as well) to baseProject

                    mergeFI.setDataIndex(newDataIndex);
                    mergeFI.resetVersions(baseProject.getVersion());

                    if (basePkg == null) {
                        basePkg = new FullPackageInfo(baseProject, mergePkgName, newDataIndex);
                        baseProject.addPackage(basePkg);
                    }
                    basePkg.addFile(mergeFI);
                    projectDataLength = Math.max(projectDataLength, mergeFI.getDataIndex() + mergeFI.getDataLength());
                    baseProject.setDataLength(projectDataLength);
                    if (baseFI != null) {
                        slotsUsed -= baseFI.getDataLength();
                    }
                    slotsUsed += mergeFI.getDataLength();
                    // map this new node's contexts
                    contextMapper.applyContextMapping(mergingDb, mergeFI);
                } else {
                    // baseProject has this file, with a different checksum and newer timestamp, so keep the base record
                    // and do no further merge
                    continue;
                }
                mergedCoverage = addIntArrays(
                        mergingData.getHitCounts(), oldDataIndex, mergedCoverage, newDataIndex, newDataLength);
                mergedSliceHits = mergePerTestCoverage(
                        mergingData, oldDataIndex, mergedSliceHits, newDataIndex, newDataLength);
            }
            progress += progressInc;
        }

        // todo: instrumentation history

        // compact the new registry and recording.
        int [] compactedCoverage = new int[slotsUsed];
        InMemPerTestCoverage compactedSliceHits = new InMemPerTestCoverage(slotsUsed);
        int insertPoint = 0;
        List<FullFileInfo> mergedFiles = (List<FullFileInfo>)baseProject.getFiles(HasMetricsFilter.ACCEPT_ALL);

        for (FullFileInfo fileInfo : mergedFiles) {
            System.arraycopy(mergedCoverage, fileInfo.getDataIndex(), compactedCoverage, insertPoint, fileInfo.getDataLength());

            for (TestCaseInfo tci : mergedSliceHits.getTests()) {
                BitSet compactedSlice = compactedSliceHits.getHitsFor(tci);
                BitSet mergedSlice = mergedSliceHits.getHitsFor(tci);
                for (int i = 0; i < fileInfo.getDataLength(); i++) {
                    compactedSlice.set(insertPoint + i, mergedSlice.get(fileInfo.getDataIndex() + i));
                }
            }
            fileInfo.setDataIndex(insertPoint);
            insertPoint += fileInfo.getDataLength();
        }
        baseProject.setDataLength(slotsUsed);

        listener.handleProgress("Writing merged database registry", progress);

        destReg.setCoverageData(
            new CoverageData(
                System.currentTimeMillis(),
                compactedCoverage,
                compactedSliceHits));
        
        destReg.saveAndOverwriteFile();
        TestCaseInfo.Factory.reset();

        if (update) {
            // delete the original
            CloverUtils.scrubCoverageData(originalInitString, true, true, false);
            // and copy over the new one
            FileUtils.fileCopy(tmpDb, new File(originalInitString));
            tmpDb.delete();
        }

        listener.handleProgress("Merge complete", 1.0f);

    }

    private static InMemPerTestCoverage mergePerTestCoverage(PerTestCoverage src, int spos, InMemPerTestCoverage dest, int dpos, int length) {
        if (dpos + length > dest.getCoverageSize()) {
            dest = new InMemPerTestCoverage(dest, dpos + length);
        }

        for (TestCaseInfo tci : src.getTests()) {
            BitSet srcSlots = src.getHitsFor(tci);
            BitSet destSlots = dest.getHitsFor(tci);
            for (int i = 0; i < length; i++) {
                destSlots.set(dpos + i, srcSlots.get(spos + i));
            }
        }

        return dest;
    }

    /**
     * Adds the elements of source array to the elements of destination, growing the destination array if needed.
     * @param src  the source array
     * @param spos  the offset in the source array to begin from
     * @param dest the destination array
     * @param dpos the offset destination
     * @param length the number of elements to add
     * @return array resulting from addition
     */
    private static int [] addIntArrays(int [] src, int spos, int [] dest, int dpos, int length) {
       if (dpos + length > dest.length) {
           int [] tmp = new int [dpos + length];
           System.arraycopy(dest, 0, tmp, 0, dest.length);
           dest = tmp;
       }
       for (int i = 0; i < length; i++) {
           dest[dpos + i] += src[spos + i];
       }

       return dest;
    }
}
