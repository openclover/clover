package com.atlassian.clover.idea.testexplorer;

import com.atlassian.clover.BitSetCoverageProvider;
import com.atlassian.clover.CloverDatabase;
import com.atlassian.clover.CoverageData;
import com.atlassian.clover.api.registry.PackageInfo;
import com.atlassian.clover.registry.entities.FullClassInfo;
import com.atlassian.clover.registry.entities.FullFileInfo;
import com.atlassian.clover.registry.entities.FullMethodInfo;
import com.atlassian.clover.registry.entities.FullPackageInfo;
import com.atlassian.clover.registry.entities.FullProjectInfo;
import com.atlassian.clover.idea.config.TestCaseLayout;
import com.atlassian.clover.idea.treetables.SortableListTreeTableModelOnColumns;
import com.atlassian.clover.idea.coverage.CoverageManager;
import com.atlassian.clover.idea.ProjectPlugin;
import com.atlassian.clover.registry.CoverageDataProvider;
import com.atlassian.clover.registry.CoverageDataReceptor;
import com.atlassian.clover.api.registry.HasMetrics;
import com.atlassian.clover.registry.metrics.HasMetricsFilter;
import com.atlassian.clover.registry.entities.PackageFragment;
import com.atlassian.clover.registry.entities.TestCaseInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static clover.com.google.common.collect.Lists.newArrayList;
import static clover.com.google.common.collect.Maps.newHashMap;

public class TestRunExplorerTreeBuilder {
    private final Project project;
    private final SortableListTreeTableModelOnColumns sortableModel;
    private final DefaultMutableTreeNode rootNode;

    public TestRunExplorerTreeBuilder(Project project, SortableListTreeTableModelOnColumns sortableModel, DefaultMutableTreeNode rootNode) {
        this.project = project;
        this.sortableModel = sortableModel;
        this.rootNode = rootNode;
    }

    private Map<PackageInfo, Map<FullClassInfo, Collection<TestCaseInfo>>> indexPerPackage(Collection<? extends TestCaseInfo> testCases) {

        final Map<PackageInfo, Map<FullClassInfo, Collection<TestCaseInfo>>> index =
                new IdentityHashMap<PackageInfo, Map<FullClassInfo, Collection<TestCaseInfo>>>();
        for (TestCaseInfo testCase : testCases) {
            FullClassInfo classInfo = testCase.getRuntimeType();
            if (classInfo == null) {
                // test case cannot be mapped to a test class - class has been renamed or removed
                continue;
            }
            PackageInfo packageInfo = classInfo.getPackage();

            Collection<TestCaseInfo> tciList;
            Map<FullClassInfo, Collection<TestCaseInfo>> clsMap = index.get(packageInfo);
            if (clsMap == null) {
                clsMap = new IdentityHashMap<FullClassInfo, Collection<TestCaseInfo>>();
                index.put(packageInfo, clsMap);
                tciList = null;
            } else {
                tciList = clsMap.get(classInfo);
            }
            if (tciList == null) {
                tciList = newArrayList();
                clsMap.put(classInfo, tciList);
            }

            tciList.add(testCase);

        }
        return index;
    }

    private Map<SourceFolderDescription, Collection<TestCaseInfo>> sortBySourceRoot(
            Collection<? extends TestCaseInfo> testCases) {
        List<SourceFolder> sourceFolders = newArrayList();
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            for (ContentEntry contentEntry : ModuleRootManager.getInstance(module).getContentEntries()) {
                sourceFolders.addAll(Arrays.asList(contentEntry.getSourceFolders()));
            }
        }

        Map<SourceFolderDescription, Collection<TestCaseInfo>> map = newHashMap();
        for (SourceFolder sourceFolder : sourceFolders) {
            final VirtualFile sourceVirtualFile = sourceFolder.getFile();
            if (sourceVirtualFile == null) {
                continue;
            }

            final SourceFolderDescription sourceFolderDescription = new SourceFolderDescription(
                    com.atlassian.clover.idea.util.vfs.VfsUtil.calcRelativeToProjectPath(sourceVirtualFile, project),
                    sourceFolder.isTestSource());
            final Collection<TestCaseInfo> perFolder = newArrayList();
            final File rootDir = VfsUtil.virtualToIoFile(sourceVirtualFile);

            for (TestCaseInfo testCase : testCases) {
                final File file = ((FullFileInfo) testCase.getRuntimeType().getContainingFile()).getPhysicalFile();
                if (VfsUtil.isAncestor(rootDir, file, false)) {
                    perFolder.add(testCase);
                }
            }
            if (!perFolder.isEmpty()) {
                map.put(sourceFolderDescription, perFolder);
            }
        }
        return map;
    }

    private void addSourceRoots(DefaultMutableTreeNode rootNode, Collection<? extends TestCaseInfo> testCases, boolean flat, CloverDatabase cloverDatabase) {
        Map<SourceFolderDescription, Collection<TestCaseInfo>> srcRootIndex = sortBySourceRoot(testCases);
        for (Map.Entry<SourceFolderDescription, Collection<TestCaseInfo>> entry : srcRootIndex.entrySet()) {
            DefaultMutableTreeNode srcRootNode = new DefaultMutableTreeNode(entry.getKey());
            rootNode.add(srcRootNode);
            if (flat) {
                addFlatPackages(srcRootNode, entry.getValue());
            } else {
                addPackageFragments(srcRootNode, entry.getValue(), cloverDatabase);
            }

        }
    }

    private void addFlatPackages(DefaultMutableTreeNode rootNode, Collection<? extends TestCaseInfo> testCases) {
        Map<PackageInfo, Map<FullClassInfo, Collection<TestCaseInfo>>> index = indexPerPackage(testCases);
        for (Map.Entry<PackageInfo, Map<FullClassInfo, Collection<TestCaseInfo>>> entry : index.entrySet()) {
            PackageInfo packageInfo = entry.getKey();
            DefaultMutableTreeNode packageNode = new DefaultMutableTreeNode(packageInfo);
            rootNode.add(packageNode);
            addClasses(entry.getValue(), packageNode);
        }

    }

    private void addClasses(Map<FullClassInfo, Collection<TestCaseInfo>> classes, DefaultMutableTreeNode packageNode) {
        for (Map.Entry<FullClassInfo, Collection<TestCaseInfo>> entry : classes.entrySet()) {
            FullClassInfo classInfo = entry.getKey();
            Collection<TestCaseInfo> classCases = entry.getValue();
            DefaultMutableTreeNode classNode = new DefaultMutableTreeNode(classInfo);
            packageNode.add(classNode);
            addSorted(classNode, classCases);
        }
    }

    private void addPackageFragments(DefaultMutableTreeNode rootNode, Collection<? extends TestCaseInfo> testCases, CloverDatabase cloverDatabase) {
        Map<PackageInfo, Map<FullClassInfo, Collection<TestCaseInfo>>> index = indexPerPackage(testCases);
        PackageFragment[] packageRoots = cloverDatabase.getFullModel().getPackageRoots();
        for (PackageFragment packageRoot : packageRoots) {
            DefaultMutableTreeNode childNode = getPackageFragmentNode(index, packageRoot);
            if (childNode != null) {
                rootNode.add(childNode);
            }
        }
    }

    private DefaultMutableTreeNode getPackageFragmentNode(Map<PackageInfo, Map<FullClassInfo, Collection<TestCaseInfo>>> index, PackageFragment packageFragment) {
        DefaultMutableTreeNode node = null;
        for (PackageFragment fragment : packageFragment.getChildren()) {
            DefaultMutableTreeNode childNode = getPackageFragmentNode(index, fragment);
            if (childNode != null) {
                if (node == null) {
                    node = new DefaultMutableTreeNode(packageFragment);
                }
                node.add(childNode);
            }
        }
        if (packageFragment.isConcrete()) {
            FullPackageInfo concrete = packageFragment.getConcretePackage();
            Map<FullClassInfo, Collection<TestCaseInfo>> classes = index.get(concrete);
            if (classes != null) {
                if (node == null) {
                    node = new DefaultMutableTreeNode(packageFragment);
                }
                addClasses(classes, node);
            }
        }

        return node;
    }

    public static Collection<DecoratedTestCaseInfo> wrap(Collection<? extends TestCaseInfo> testCases,
                                                         CoverageDataReceptor receptor,
                                                         CloverDatabase currentDatabase,
                                                         CoverageManager coverageManager) {
        final Collection<DecoratedTestCaseInfo> decorated = new ArrayList<DecoratedTestCaseInfo>(testCases.size());
        for (TestCaseInfo testCase : testCases) {
            decorated.add(new DecoratedTestCaseInfo(testCase, receptor, currentDatabase, coverageManager));
        }

        return decorated;
    }


    void populate(CloverDatabase currentDatabase, CoverageDataReceptor receptor, TestCaseLayout layout, boolean flatten, boolean addCoverage) {
        @SuppressWarnings("unchecked")
        Collection<? extends TestCaseInfo> testCases = receptor instanceof FullProjectInfo ?
                currentDatabase.getCoverageData().getTests() :
                currentDatabase.getTestHits(receptor);

        if (addCoverage && receptor instanceof HasMetrics) {
            final CoverageManager coverageManager = ProjectPlugin.getPlugin(project).getCoverageManager();
            testCases = wrap(testCases, receptor, currentDatabase, coverageManager);
        }
        populate(currentDatabase, testCases, layout, flatten);
    }

    private WeakReference<BackgroundCoverageCalculator> lastCalculator;

    private void scheduleCalculator(Collection<DecoratedTestCaseInfo> testCases, CoverageDataReceptor receptor, CloverDatabase currentDatabase) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        if (WindowManager.getInstance().getFrame(project) != null) {
            final BackgroundCoverageCalculator calculator = new BackgroundCoverageCalculator(testCases, receptor, currentDatabase);
            lastCalculator = new WeakReference<BackgroundCoverageCalculator>(calculator);
            calculator.queue();
        }

    }

    void cancelLastCalculator() {
        ApplicationManager.getApplication().assertIsDispatchThread();
        final BackgroundCoverageCalculator last = lastCalculator != null ? lastCalculator.get() : null;
        if (last != null) {
            last.cancel();
        }
    }

    void populate(CloverDatabase currentDatabase, Collection<? extends TestCaseInfo> testCases, TestCaseLayout layout, boolean flatten) {

        switch (layout) {
            case TEST_CASES:
                addSorted(rootNode, testCases);
                break;
            case PACKAGES:
                if (flatten) {
                    addFlatPackages(rootNode, testCases);
                } else {
                    addPackageFragments(rootNode, testCases, currentDatabase);
                }
                break;
            case SOURCE_ROOTS:
                addSourceRoots(rootNode, testCases, flatten, currentDatabase);
                break;
        }
    }

    private void addSorted(DefaultMutableTreeNode root, Collection<? extends TestCaseInfo> testCases) {
        List<DefaultMutableTreeNode> nodes = new ArrayList<DefaultMutableTreeNode>(testCases.size());
        for (TestCaseInfo tci : testCases) {
            final DefaultMutableTreeNode node = new DefaultMutableTreeNode(tci);
            nodes.add(node);
            if (tci instanceof DecoratedTestCaseInfo) {
                ((DecoratedTestCaseInfo)tci).setAsyncUpdate(new Runnable() {
                    @Override
                    public void run() {
                        sortableModel.nodeChanged(node);
                    }
                });
            }
        }
        sortableModel.sortNodes(nodes);
        for (DefaultMutableTreeNode node : nodes) {
            root.add(node);
        }
    }

    public class BackgroundCoverageCalculator extends Task.Backgroundable {
        private ProgressIndicator progressIndicator;
        private boolean alreadyCancelled;
        private final Collection<DecoratedTestCaseInfo> testCases;
        private final CoverageDataReceptor receptor;
        private final CloverDatabase cloverDatabase;

        public BackgroundCoverageCalculator(Collection<DecoratedTestCaseInfo> testCases, CoverageDataReceptor receptor, CloverDatabase cloverDatabase) {
            super(project, "Calculating per-test coverage");
            //noinspection AssignmentToCollectionOrArrayFieldFromParameter
            this.testCases = testCases;
            this.receptor = receptor;
            this.cloverDatabase = cloverDatabase;
        }

        @Override
        public void run(@NotNull ProgressIndicator indicator) {
            setProgressIndicator(indicator);

            indicator.setIndeterminate(true);
            indicator.setText2("Copying receptor data");

            final CoverageDataReceptor receptorCopy = copyReceptor(receptor);

            indicator.setText2("Indexing per test coverage");

            final int step = testCases.size() / 100 + 1; // update progress about 100 times
            int counter = 0;

            indicator.setIndeterminate(false);
            indicator.setFraction(0);
            indicator.setText2("Calculating per test coverage");

            for (final DecoratedTestCaseInfo testCase : testCases) {
                final Set<TestCaseInfo> testSet = Collections.singleton(testCase.getNakedTestCaseInfo());
                final CoverageData data = cloverDatabase.getCoverageData();
                final BitSet testCoverage = data.getHitsFor(testSet, receptor);
                final CoverageDataProvider dataProvider = new BitSetCoverageProvider(testCoverage, data);

                receptorCopy.setDataProvider(dataProvider);
                final float coverage = ((HasMetrics) receptorCopy).getMetrics().getPcCoveredElements();

                testCoverage.and(cloverDatabase.getCoverageData().getUniqueHitsFor(testCase.getNakedTestCaseInfo()));
                receptorCopy.setDataProvider(dataProvider); //reset data provider

                final float uniqueCoverage = ((HasMetrics) receptorCopy).getMetrics().getPcCoveredElements();

                testCase.setCoverage(coverage);
                testCase.setUniqueCoverage(uniqueCoverage);
                indicator.checkCanceled();
                if (++counter % step == 0) {
                    indicator.setFraction(((double) counter) / testCases.size());
                }
            }
        }

        @Override
        public void onSuccess() {
            sortableModel.nodesChanged(rootNode, null);
        }

        @Override
        public void onCancel() {
            sortableModel.nodesChanged(rootNode, null);
        }

        private CoverageDataReceptor copyReceptor(CoverageDataReceptor receptor) {
            if (receptor instanceof FullProjectInfo) {
                return ((FullProjectInfo) receptor).copy();
            } else if (receptor instanceof FullFileInfo) {
                final FullFileInfo fileInfo = (FullFileInfo) receptor;
                return fileInfo.copy((FullPackageInfo) fileInfo.getContainingPackage(), HasMetricsFilter.ACCEPT_ALL);
            } else if (receptor instanceof FullClassInfo) {
                final FullClassInfo classInfo = (FullClassInfo) receptor;
                return classInfo.copy((FullFileInfo) classInfo.getContainingFile(), HasMetricsFilter.ACCEPT_ALL);
            } else if (receptor instanceof FullMethodInfo) {
                final FullMethodInfo methodInfo = (FullMethodInfo) receptor;
                if (methodInfo.getContainingClass() != null) {
                    return methodInfo.copy((FullClassInfo) methodInfo.getContainingClass());
                } else if (methodInfo.getContainingMethod() != null) {
                    return methodInfo.copy((FullMethodInfo) methodInfo.getContainingMethod());
                } else {
                    return methodInfo.copy((FullFileInfo) methodInfo.getContainingFile());
                }
            } else {
                return null;
            }
        }

        private synchronized void setProgressIndicator(ProgressIndicator indicator) throws ProcessCanceledException {
            if (alreadyCancelled) {
                throw new ProcessCanceledException();
            }
            progressIndicator = indicator;
        }

        public synchronized void cancel() {
            alreadyCancelled = true;
            if (progressIndicator != null) {
                progressIndicator.cancel();
            }
        }
    }
}
