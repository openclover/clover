package com.atlassian.clover.idea.coverage;

import com.atlassian.clover.api.CloverException;
import com.atlassian.clover.CloverDatabase;
import com.atlassian.clover.CoverageData;
import com.atlassian.clover.CoverageDataSpec;
import com.atlassian.clover.Logger;
import com.atlassian.clover.MaskedBitSetCoverageProvider;
import com.atlassian.clover.idea.util.ModelScope;
import com.atlassian.clover.recorder.PerTestCoverageStrategy;
import com.atlassian.clover.ProgressListener;
import com.atlassian.clover.cfg.StorageSize;
import com.atlassian.clover.registry.entities.FullClassInfo;
import com.atlassian.clover.registry.entities.FullFileInfo;
import com.atlassian.clover.registry.entities.FullPackageInfo;
import com.atlassian.clover.registry.entities.FullProjectInfo;
import com.atlassian.clover.api.registry.BlockMetrics;
import com.atlassian.clover.api.registry.HasMetrics;
import com.atlassian.clover.api.registry.MethodInfo;
import com.atlassian.clover.registry.metrics.ClassMetrics;
import com.atlassian.clover.registry.Clover2Registry;
import com.atlassian.clover.registry.CoverageDataProvider;
import com.atlassian.clover.registry.CoverageDataReceptor;
import com.atlassian.clover.registry.metrics.FileMetrics;
import com.atlassian.clover.registry.metrics.HasMetricsFilter;
import com.atlassian.clover.registry.entities.PackageFragment;
import com.atlassian.clover.registry.entities.TestCaseInfo;
import com.atlassian.clover.reporters.filters.DefaultTestFilter;
import com.atlassian.clover.optimization.Snapshot;
import com.atlassian.clover.util.Formatting;
import com.atlassian.clover.util.Path;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static clover.com.google.common.collect.Lists.newLinkedList;
import static clover.com.google.common.collect.Maps.newHashMap;


/**
 * Encapsulates a CoverageModel and provides useful methods for showing coverage
 * data in a JTree
 */
public class CoverageTreeModel {
    private static final long MIN_STORAGE_SIZE = 16 * 1024 * 1024;

    /**
     * use to log messages *
     */
    private static final Logger LOG = Logger.getInstance();

    private CloverDatabase db;
    public final Semaphore dbLock = new Semaphore(1);

    // maps PackageFragment.getQName to PackageFragment in full model
    private Map<String, PackageFragment> packageFragmentMap;
    private CoverageDataProvider passedOnlyDataCache;

    /**
     * Root node for the coverage tree
     */
    private DefaultMutableTreeNode mTree;

    // whether or not to fragment packages in the tree
    private boolean mFragmented;

    private String filterSpec;

    private String mRootName;
    private String mInitString;
    private long mSpan;

    // where to look for source files
    private final Path sourcepath;
    private ModelScope modelScope;
    private boolean loadPerTestData;
    private HasMetricsFilter.Invertable testFilter;
    private final HasMetricsFilter includeFilter;

    private final Project project;

    public CoverageTreeModel(String rootname, String initString, long span, String filterSpec,
                             boolean fragment, Path sourcepath,
                             ModelScope modelScope, boolean loadPerTestData, HasMetricsFilter.Invertable testFilter,
                             HasMetricsFilter includeFilter, Project project) {
        mRootName = rootname;
        mInitString = initString;
        mSpan = span;
        this.filterSpec = filterSpec;
        mFragmented = fragment;
        this.sourcepath = sourcepath;
        this.modelScope = modelScope;
        this.loadPerTestData = loadPerTestData;
        this.project = project;
        this.testFilter = testFilter != null ? testFilter : new DefaultTestFilter();
        this.includeFilter = includeFilter;
    }

    public CoverageTreeModel safeCopy(ProgressIndicator progressIndicator) {
        final CoverageTreeModel copy = new CoverageTreeModel(
                mRootName,
                mInitString,
                mSpan,
                filterSpec,
                mFragmented,
                sourcepath,
                modelScope,
                loadPerTestData,
                testFilter,
                includeFilter,
                project);
        try {
            while (!dbLock.tryAcquire(500, TimeUnit.MILLISECONDS)) {
                progressIndicator.checkCanceled();
            }
        } catch (InterruptedException e) {
            throw new ProcessCanceledException(e);
        }
        try {
            copy.db = db == null ? null : db.copyForBackgroundCoverageDataLoad();
            copy.packageFragmentMap = newHashMap(packageFragmentMap);
        } finally {
            dbLock.release();
        }

        return copy;
    }

    public boolean canLoad() {
        return mInitString != null && mInitString.length() > 0 && new File(mInitString).exists();
    }

    /**
     * May return null.
     *
     * @return currently loaded Clover database.
     */
    @Nullable
    public CloverDatabase getCloverDatabase() {
        return db;
    }

    private static StorageSize calcAvailableStorageSize() {
        final long maxHeap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax();
        final long maxSize = maxHeap/4;
        return new StorageSize(maxSize > MIN_STORAGE_SIZE ? maxSize : MIN_STORAGE_SIZE);

    }

    public void load(@NotNull ProgressListener progressListener) {
        progressListener.handleProgress("Creating database", 0);
        try {
            db = new CloverDatabase(mInitString, includeFilter, null, filterSpec, progressListener);
            passedOnlyDataCache = null;
        } catch (CloverException e) {
            if (e.getCause() instanceof ProcessCanceledException) {
                throw (ProcessCanceledException) e.getCause();
            }
            LOG.info("Problem loading Coverage Data:", e);
            mRootName = "Problem loading Coverage Data. Please regenerate.";
        }
        if (db != null) {
            progressListener.handleProgress("Building Index", 0);
            rebuildPackageTreeIndex();
            loadCoverageData(loadPerTestData, progressListener);
            if (sourcepath != null) {
                progressListener.handleProgress("Resolving coverage data", 0);
                db.resolve(sourcepath);
            }
        }
        progressListener.handleProgress("Clover Database loaded", 0);
    }

    public void loadCoverageData(final boolean loadPerTestData, ProgressListener progressListener) {
        passedOnlyDataCache = null;
        if (db == null) {
            return;
        }
        progressListener.handleProgress("Loading coverage data", 0);
        try {
            db.loadCoverageData(prepareCoverageDataSpec(loadPerTestData), progressListener);
        } catch (CloverException e) {
            LOG.info("Coverage data load failed", e);
            throw new ProcessCanceledException(e);
        }
        progressListener.handleProgress("Looking for a test optimization snapshot file", 0);
        final Snapshot snapshot;
        final SnapshotFileMutex mutex = ServiceManager.getService(SnapshotFileMutex.class);
        final File snapshotFile = new File(Snapshot.fileNameForInitString(mInitString));
        mutex.lockFile(snapshotFile);
        try {
            snapshot = Snapshot.loadFrom(snapshotFile);
        } finally {
            mutex.releaseFile(snapshotFile);
        }

        if (snapshot != null) {
            progressListener.handleProgress("Updating test optimization snapshot file", 0);
            snapshot.updateFor(db);
            try {
                mutex.lockFile(snapshot.getLocation());
                snapshot.store();
            } catch (IOException e) {
                LOG.warn("Storing tespt optimization snapshot failed, deleting snapshot to prevent wrong optimization", e);
                snapshot.delete();
            } finally {
                mutex.releaseFile(snapshot.getLocation());
            }
        }
    }

    public CoverageDataSpec prepareCoverageDataSpec(boolean loadPerTestData) {
        final CoverageDataSpec spec = new CoverageDataSpec(testFilter, computeSpan());
        spec.setLoadPerTestData(loadPerTestData);
        spec.setPerTestStrategy(PerTestCoverageStrategy.SAMPLING);
        spec.setPerTestStorageSize(calcAvailableStorageSize());
        return spec;
    }

    private long computeSpan() {
        if (mSpan != 0) {
            return mSpan;
        }
        final long start = findFirstInstrTime(db);
        return start == Long.MAX_VALUE ? 0 : db.getRegistry().getVersion() - start;
    }

    public synchronized boolean applyIncludePassedTestCoverageOnlyFilter(boolean includePassedOnly) {
        if (db == null) {
            return false;
        }

        if (ModelUtil.isPassedTestsCoverageOnly(db) == includePassedOnly) {
            return false; // valid scope already
        } else {
            final CoverageData fullData = db.getCoverageData();
            applyDataProvider(
                db,
                includePassedOnly
                    ? new MaskedBitSetCoverageProvider(fullData.getPassOnlyAndIncidentalHits(), fullData, fullData)
                    : fullData);
            return true;
        }
    }

    private static void applyDataProvider(@NotNull CloverDatabase cloverDatabase, @NotNull CoverageDataProvider provider) {
        cloverDatabase.getFullModel().setDataProvider(provider);
        cloverDatabase.getAppOnlyModel().setDataProvider(provider);
        cloverDatabase.getTestOnlyModel().setDataProvider(provider);
    }


    private static long findFirstInstrTime(@NotNull CloverDatabase db) {
        @SuppressWarnings("unchecked")
        List<Clover2Registry.InstrumentationInfo> instrEvents = db.getRegistry().getInstrHistory();
        long minTime = Long.MAX_VALUE;
        for (Clover2Registry.InstrumentationInfo instrEvent : instrEvents) {
            final long ts = instrEvent.getEndTS();
            if (ts < minTime) {
                minTime = ts;
            }
        }
        return minTime;
    }

    /**
     * get the equivalent path for the reference path. This is used when the model is updated, and we have reference
     * to an older path we need to convert
     *
     * @param path original path to retrieve last element from
     * @return new path , or null if the last object in the old path can't be found in the model
     */
    public TreePath getEquivPath(TreePath path) {

        try {
            DefaultMutableTreeNode last = (DefaultMutableTreeNode) path.getPathComponent(path.getPathCount() - 1);
            NodeWrapper target = (NodeWrapper) last.getUserObject();
            for (Enumeration nodes = mTree.breadthFirstEnumeration(); nodes.hasMoreElements();) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();
                NodeWrapper wrapper = (NodeWrapper) node.getUserObject();
                if (target.getId().equals(wrapper.getId())) {
                    return new TreePath(node.getPath());
                }
            }
        } catch (ClassCastException e) {
            // ignore - return null
        }

        return null;
    }

    public TreePath getPathForFile(File file) {
        if (file == null) {
            return null;
        }
        try {
            file = file.getCanonicalFile();

            // traverse the entire tree looking for a node with a
            // containing file that mateches the specified file.
            for (Enumeration nodes = mTree.breadthFirstEnumeration(); nodes.hasMoreElements();) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();
                final Object userObject = node.getUserObject();
                if (userObject instanceof NodeWrapper) {
                    NodeWrapper wrapper = (NodeWrapper) userObject;
                    HasMetrics hasMetrics = wrapper.getHasMetrics();
                    if (hasMetrics instanceof FullClassInfo) {
                        FullFileInfo fileInfo = (FullFileInfo) ((FullClassInfo) hasMetrics).getContainingFile();
                        if (file.equals(fileInfo.getPhysicalFile())) {
                            return new TreePath(node.getPath());
                        }
                    }
                }
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    public TreePath getPathForHasMetrics(HasMetrics hasMetrics) {
        for (Enumeration nodes = mTree.breadthFirstEnumeration(); nodes.hasMoreElements();) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();
            final Object userObject = node.getUserObject();
            if (userObject instanceof NodeWrapper) {
                NodeWrapper wrapper = (NodeWrapper) userObject;
                HasMetrics nodeHasMetrics = wrapper.getHasMetrics();
                if (nodeHasMetrics.equals(hasMetrics)) {
                    return new TreePath(node.getPath());

                }
            }
        }
        return null;
    }

    public void startRegistryUpdate() {
        dbLock.acquireUninterruptibly(); // block coverage load thread ASAP, it's work would be discarded anyway
    }

    public void registryUpdated() {
        dbLock.release();
        rebuildPackageTreeIndex();
    }

    public CoverageDataProvider getCachedPassOnlyCoverage() {
        if (db == null) {
            return null;
        }
        if (passedOnlyDataCache == null) {
            final CoverageData fullData = db.getCoverageData();
            passedOnlyDataCache = new MaskedBitSetCoverageProvider(fullData.getPassOnlyAndIncidentalHits(), fullData, fullData);
        }
        return passedOnlyDataCache;
    }




/**
     * The class can no longer be static as it needs parent reference to lazily compute TestPassInfos.
     */
    public class NodeWrapper {
        private final String id;
        private final HasMetrics hasMetrics;
        private CoverageNodeViewer.TestPassInfo testPassInfo;

        public NodeWrapper(String id, HasMetrics hasMetrics, BaseCoverageNodeViewer.TestPassInfo testPassInfo) {
            this.id = id;
            this.hasMetrics = hasMetrics;
            this.testPassInfo = testPassInfo;
        }

        /**
         * TestPassInfo will be initialized lazily.
         *
         * @param id         node id
         * @param hasMetrics hasMetrics
         */
        public NodeWrapper(String id, HasMetrics hasMetrics) {
            this(id, hasMetrics, null);
        }

        /**
         * The id of a node is used for equivalence tests.
         *
         * @return id
         */
        public String getId() {
            return id;
        }

        public String getName() {
            return hasMetrics.getName();
        }

        public HasMetrics getHasMetrics() {
            return hasMetrics;
        }

        /**
         * calculate expensive ones lazily.
         *
         * @return cached or lazily calculated TestPassInfo
         */
        public CoverageNodeViewer.TestPassInfo getTestPassInfo() {
            if (testPassInfo == null) {
                testPassInfo = (hasMetrics instanceof PackageFragment) ?
                        calculatePFTestPasses((PackageFragment) hasMetrics) :
                        // ClassCastException here would be an Internal Error
                        calculateTestPasses((CoverageDataReceptor) hasMetrics);

            }
            return testPassInfo;
        }

        @Override
        public String toString() {
            final BlockMetrics metrics = hasMetrics.getMetrics();
            StringBuilder sb = new StringBuilder(getName());
            sb.append(" (");
            sb.append(Formatting.getPercentStr(hasMetrics.getMetrics().getPcCoveredElements()));
            if (metrics instanceof FileMetrics) {
                sb.append(" / ");
                sb.append(((FileMetrics) metrics).getNcLineCount());
                sb.append(" lines");
            }
            sb.append(" / ");
            if (metrics instanceof ClassMetrics) {
                // includes FileMetrics and above
                sb.append(Formatting.format2d(((ClassMetrics) metrics).getAvgMethodComplexity()));
            } else {
                // method level basically
                sb.append(metrics.getComplexity());
            }
            sb.append(" )");
            return sb.toString();
        }
    }

    private static final Comparator<HasMetrics> HASMETRICS_COMPARATOR = new Comparator<HasMetrics>() {
        @Override
        public int compare(HasMetrics o1, HasMetrics o2) {
            return o1.getName().compareToIgnoreCase(o2.getName());
        }
    };


    /**
     * Uses getAppModel() to getFullModel() mapping to reduce computation complexity.
     *
     * @param packageFragment package fragment to retrieve test classes for
     * @return TestPassInfo for this package fragment (including children)
     */
    private CoverageNodeViewer.TestPassInfo calculatePFTestPasses(PackageFragment packageFragment) {
        final PackageFragment globalPackageFragment = (modelScope == ModelScope.ALL_CLASSES) ? packageFragment :
                packageFragmentMap.get(packageFragment.getQualifiedName());
        return new BaseCoverageNodeViewer.TestPassInfo(globalPackageFragment.getMetrics());
    }

    /**
     * No easy way to map HasMetrics from getAppModel() to getFullModel(), so do it the hard way.
     *
     * @param codeReceptor element to calculate test coverage for
     * @return calculated TestPassInfo
     */
    private CoverageNodeViewer.TestPassInfo calculateTestPasses(CoverageDataReceptor codeReceptor) {
        @SuppressWarnings("unchecked")
        final Set<TestCaseInfo> hits = db.getTestHits(codeReceptor);
        return new BaseCoverageNodeViewer.TestPassInfo(hits);
    }

    private void addFragmentPackageNode(PackageFragment fragmentInfo, DefaultMutableTreeNode parentNode) {
        DefaultMutableTreeNode fragmentNode = new DefaultMutableTreeNode(
                new NodeWrapper(fragmentInfo.getName(), fragmentInfo, calculatePFTestPasses(fragmentInfo)));
        parentNode.add(fragmentNode);

        PackageFragment[] children = fragmentInfo.getChildren();
        Arrays.sort(children, HASMETRICS_COMPARATOR);

        for (PackageFragment aChildren : children) {
            addFragmentPackageNode(aChildren, fragmentNode);
        }

        if (fragmentInfo.isConcrete()) {
            addClassNodes(fragmentInfo.getConcretePackage().getClasses(), fragmentNode);
        }
    }

    private void addFlatPackageNode(PackageFragment fragmentInfo, DefaultMutableTreeNode parentNode) {
        if (fragmentInfo.isConcrete()) {
            FullPackageInfo concretePackage = fragmentInfo.getConcretePackage();
            DefaultMutableTreeNode fragmentNode = new DefaultMutableTreeNode(
                    new NodeWrapper(concretePackage.getName(), concretePackage, calculatePFTestPasses(fragmentInfo)));
            parentNode.add(fragmentNode);
            addClassNodes(concretePackage.getClasses(), fragmentNode);
        }


        PackageFragment[] children = fragmentInfo.getChildren();
        Arrays.sort(children, HASMETRICS_COMPARATOR);

        for (PackageFragment aChildren : children) {
            addFlatPackageNode(aChildren, parentNode);
        }
    }

    private void addClassNodes(List classes, DefaultMutableTreeNode fragmentNode) {
        FullClassInfo[] classesArray = new FullClassInfo[classes.size()];
        classes.toArray(classesArray);
        Arrays.sort(classesArray, HASMETRICS_COMPARATOR);

        for (FullClassInfo classInfo : classesArray) {
            DefaultMutableTreeNode classNode = new DefaultMutableTreeNode(
                    new NodeWrapper(
                            classInfo.getContainingFile() + ":" + classInfo.getName(),
                            classInfo));
            fragmentNode.add(classNode);
            List<? extends MethodInfo> methods = classInfo.getMethods();
            Collections.sort(methods, HASMETRICS_COMPARATOR);

            for (MethodInfo method : methods) {
                // do not show lambda functions, do not show inner functions too (no recursion here)
                if (!method.isLambda()) {
                    classNode.add(new DefaultMutableTreeNode(new NodeWrapper(
                            method.getContainingFile() + ":" + method.getContainingClass() + ":" + method.getName(),
                            method)));
                }
            }

        }
    }


    private void rebuildPackageTreeIndex() {
        packageFragmentMap = newHashMap();

        for (PackageFragment fragment : db.getFullModel().getPackageRoots()) {
            indexPackageFragment(fragment);
        }
    }

    private void indexPackageFragment(PackageFragment fragment) {
        packageFragmentMap.put(fragment.getQualifiedName(), fragment);
        for (PackageFragment child : fragment.getChildren()) {
            indexPackageFragment(child);
        }
    }

    public synchronized void createTree() {
        if (db == null) {
            mTree = new DefaultMutableTreeNode(mRootName, false);
        } else {
            final FullProjectInfo projectInfo = ModelUtil.getModel(db, modelScope);
            mTree = new DefaultMutableTreeNode(
                    new NodeWrapper(mRootName, projectInfo,
                                    new BaseCoverageNodeViewer.TestPassInfo(db.getFullModel().getMetrics())));

            PackageFragment[] roots = projectInfo.getPackageRoots();
            Arrays.sort(roots, HASMETRICS_COMPARATOR);

            for (PackageFragment root : roots) {
                if (mFragmented) {
                    addFragmentPackageNode(root, mTree);
                } else {
                    addFlatPackageNode(root, mTree);
                }
            }
        }
    }

    //needs synchronized because mFragmented is used inside createTree()
    public synchronized DefaultMutableTreeNode getClassTree(boolean frag, ModelScope modelScope) {
        if (frag != mFragmented || this.modelScope != modelScope || mTree == null) {
            mFragmented = frag;
            this.modelScope = modelScope;
            createTree();
            applyFilters();
        }

        return mTree;
    }

    /**
     * @param path path to retrieve last NodeWrapper from
     * @return the node at <code>path</code>, or null if there is no real node there
     */
    public static NodeWrapper getNodeForPath(final TreePath path) {
        if (path != null) {
            try {
                DefaultMutableTreeNode tmp = (DefaultMutableTreeNode) path.getLastPathComponent();
                return (NodeWrapper) tmp.getUserObject();
            } catch (ClassCastException cce) {
                return null;
            }
        }
        return null;
    }

    //TODO: efficiency improvements... dont filter the mTree directly. rather,
    //TODO: maintain a filtered copy - saves from recreating the tree all the time.

    @SuppressWarnings({"CollectionDeclaredAsConcreteClass"})
    private LinkedList<CoverageTreeFilter> filters = newLinkedList();

    public void addFilter(CoverageTreeFilter aFilter) {
        addFilterFirst(aFilter);
    }

    public void addFilterFirst(CoverageTreeFilter aFilter) {
        filters.addFirst(aFilter);
        mTree = null;
    }

    public void addFilterLast(CoverageTreeFilter aFilter) {
        filters.addLast(aFilter);
        mTree = null;
    }

    public void removeFilter(CoverageTreeFilter aFilter) {
        filters.remove(aFilter);
        mTree = null;
    }

    public boolean hasFilter(CoverageTreeFilter aFilter) {
        return filters.contains(aFilter);
    }

    private void applyFilters() {
        // class root is mTree
        filter(mTree);
    }

    private void filter(DefaultMutableTreeNode aNode) {
        // filter all children
        if (aNode.getChildCount() > 0) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) aNode.getFirstChild();
            while (child != null) {
                DefaultMutableTreeNode sibling = child.getNextSibling();
                filter(child);
                child = sibling;
            }
        }

        // filter aNode
        for (CoverageTreeFilter filter : filters) {
            if (!filter.accept(aNode)) {
                aNode.removeFromParent();
                return;
            }
        }
    }
}
