package org.openclover.idea.testexplorer;

import com.intellij.openapi.project.Project;
import com.intellij.ui.dualView.TreeTableView;
import org.openclover.core.BitSetCoverageProvider;
import org.openclover.core.CloverDatabase;
import org.openclover.core.CoverageData;
import org.openclover.core.api.registry.MethodInfo;
import org.openclover.core.api.registry.CoverageDataProvider;
import org.openclover.core.registry.entities.FullClassInfo;
import org.openclover.core.registry.entities.FullFileInfo;
import org.openclover.core.registry.entities.FullMethodInfo;
import org.openclover.core.registry.entities.FullPackageInfo;
import org.openclover.core.registry.entities.FullProjectInfo;
import org.openclover.core.registry.entities.TestCaseInfo;
import org.openclover.core.api.registry.HasMetricsFilter;
import org.openclover.idea.IProjectPlugin;
import org.openclover.idea.ProjectPlugin;
import org.openclover.idea.config.ConfigChangeEvent;
import org.openclover.idea.config.ConfigChangeListener;
import org.openclover.idea.config.IdeaCloverConfig;
import org.openclover.idea.coverage.CoverageListener;
import org.openclover.idea.coverage.CoverageManager;
import org.openclover.idea.treetables.SortableListTreeTableModelOnColumns;
import org.openclover.idea.treetables.TreeTableModelFactory;
import org.openclover.idea.util.ui.ScrollToSourceMouseAdapter;
import org.openclover.idea.util.ui.TreeExpansionHelper;
import org.openclover.idea.util.ui.TreeUtil;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import static org.openclover.core.util.Lists.newArrayList;
import static org.openclover.core.util.Maps.newHashMap;

public class CoverageContributionPanel extends JPanel implements TestRunExplorerToolWindow.TestCaseSelectionListener,
        CoverageListener, ConfigChangeListener {

    private CloverDatabase currentDatabase;
    private boolean alwaysCollapseTestClasses;
    private boolean alwaysExpandTestClasses;
    private boolean flattenPackages;
    private final Project project;

    public CoverageContributionPanel(Project project) {
        this.project = project;
        initComponents();
        registerListeners(project);
        IdeaCloverConfig config = ProjectPlugin.getPlugin(project).getConfig();
        alwaysCollapseTestClasses = config.isAlwaysCollapseTestClasses();
        alwaysExpandTestClasses = config.isAlwaysExpandTestClasses();
        flattenPackages = config.isFlattenPackages();
    }

    private final DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode();
    private final SortableListTreeTableModelOnColumns tableModel = TreeTableModelFactory.getTestCaseInfoTreeTableModel(rootNode);
    private final TreeTableView treeTableView = new TreeTableView(tableModel);
    private final CoverageContributionTreeBuilder treeBuilder = new CoverageContributionTreeBuilder(rootNode, tableModel);

    private void initComponents() {
        setLayout(new BorderLayout());

        treeTableView.setRootVisible(false);
        treeTableView.setTreeCellRenderer(new ContribTreeCellRenderer());
        treeTableView.getTree().setShowsRootHandles(true);

        treeTableView.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        treeTableView.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        treeTableView.setAutoscrolls(true);
        treeTableView.setMinimumSize(treeTableView.getPreferredSize());
        treeTableView.addMouseListener(ScrollToSourceMouseAdapter.getInstance(project));
        treeTableView.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                final TableColumnModel columnModel = treeTableView.getTableHeader().getColumnModel();
                int column = columnModel.getColumnIndexAtX(e.getX());
                int modelColumn = columnModel.getColumn(column).getModelIndex();
                
                tableModel.sortByColumn(modelColumn);
                sortNodes();
            }
        });


        add(new JScrollPane(treeTableView), BorderLayout.CENTER);
    }

    private void sortNodes() {
        final boolean customExpand = !alwaysExpandTestClasses && !alwaysCollapseTestClasses;
        TreeExpansionHelper teh = customExpand ? new TreeExpansionHelper(treeTableView.getTree()) : null;
        TreeUtil.sortNodes(rootNode, tableModel);
        tableModel.reload();
        if (customExpand) {
            teh.restore(treeTableView.getTree());
        } else {
            if (alwaysExpandTestClasses) {
                expandOrCollapseAll();
            }
        }
    }

    private void registerListeners(Project project) {
        final IProjectPlugin projectPlugin = ProjectPlugin.getPlugin(project);

        final CoverageManager coverageManager = projectPlugin.getCoverageManager();
        currentDatabase = coverageManager.getCoverage();
        coverageManager.addCoverageListener(this);

        projectPlugin.getConfig().addConfigChangeListener(this);
    }

    @Override
    public void configChange(ConfigChangeEvent evt) {
        if (evt.hasPropertyChange(IdeaCloverConfig.ALWAYS_COLLAPSE_TEST_CLASSES)) {
            PropertyChangeEvent propertyChange = evt.getPropertyChange(IdeaCloverConfig.ALWAYS_COLLAPSE_TEST_CLASSES);
            alwaysCollapseTestClasses = (Boolean) propertyChange.getNewValue();
            expandOrCollapseAll();
        }
        if (evt.hasPropertyChange(IdeaCloverConfig.ALWAYS_EXPAND_TEST_CLASSES)) {
            PropertyChangeEvent propertyChange = evt.getPropertyChange(IdeaCloverConfig.ALWAYS_EXPAND_TEST_CLASSES);
            alwaysExpandTestClasses = (Boolean) propertyChange.getNewValue();
            expandOrCollapseAll();
        }
        if (evt.hasPropertyChange(IdeaCloverConfig.FLATTEN_PACKAGES)) {
            PropertyChangeEvent propertyChange = evt.getPropertyChange(IdeaCloverConfig.FLATTEN_PACKAGES);
            flattenPackages = (Boolean) propertyChange.getNewValue();
            valueChanged(lastTestCaseInfo);
        }
    }

    private void expandOrCollapseAll() {
        if (alwaysCollapseTestClasses != alwaysExpandTestClasses) {
            final JTree treeTableTree = treeTableView.getTree();

            Enumeration children = rootNode.postorderEnumeration();
            while (children.hasMoreElements()) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
                if (rootNode != child && !child.isLeaf()) {
                    TreePath path = new TreePath(child.getPath());
                    if (alwaysExpandTestClasses) {
                        treeTableTree.expandPath(path);
                    } else {
                        treeTableTree.collapsePath(path);
                    }
                }
            }
        }
    }

    private TestCaseInfo lastTestCaseInfo;
    private boolean lastFlattenPackages;

    @Override
    public void valueChanged(TestCaseInfo testCaseInfo) {
        if (lastTestCaseInfo == testCaseInfo && lastFlattenPackages == flattenPackages) {
            return;
        }
        lastTestCaseInfo = testCaseInfo;
        lastFlattenPackages = flattenPackages;

        final boolean customExpand = !alwaysExpandTestClasses && !alwaysCollapseTestClasses;
        TreeExpansionHelper teh = customExpand ? new TreeExpansionHelper(treeTableView.getTree()) : null;

        rootNode.removeAllChildren();
        if (testCaseInfo != null) {
            treeBuilder.processClassesFor(currentDatabase, testCaseInfo, flattenPackages);
        }

        tableModel.reload();
        if (customExpand) {
            teh.restore(treeTableView.getTree());
        } else {
            if (alwaysExpandTestClasses) {
                // tree is created in collapsed state, expand when necessary
                expandOrCollapseAll();
            }
        }
    }


    @Override
    public void update(final CloverDatabase db) {
        currentDatabase = db;
    }

}


class CoverageContributionTreeBuilder {
    private final SortableListTreeTableModelOnColumns tableModel;
    private final DefaultMutableTreeNode rootNode;

    public CoverageContributionTreeBuilder(DefaultMutableTreeNode rootNode, SortableListTreeTableModelOnColumns tableModel) {
        this.rootNode = rootNode;
        this.tableModel = tableModel;
    }

    void processClassesFor(CloverDatabase currentDatabase, final TestCaseInfo testCaseInfo, boolean flattenPackages) {
        if (currentDatabase == null) {
            return;
        }

        final FullProjectInfo appOnlyProject = currentDatabase.getAppOnlyModel();
        final CoverageData data = currentDatabase.getCoverageData();
        final CoverageDataProvider testDataProvider = new BitSetCoverageProvider(data.getHitsFor(testCaseInfo), data);
        final CoverageDataProvider uniqueTestDataProvider = new BitSetCoverageProvider(currentDatabase.getCoverageData().getUniqueHitsFor(testCaseInfo), data);
        final Map<FullPackageInfo, DefaultMutableTreeNode> packageMapping = newHashMap();

        appOnlyProject.getClasses(hasMetrics -> {
            final FullClassInfo classInfo = (FullClassInfo) hasMetrics;
            FullClassInfo classInfoCopy = classInfo.copy((FullFileInfo) classInfo.getContainingFile(), HasMetricsFilter.ACCEPT_ALL);
            classInfoCopy.setDataProvider(testDataProvider);

            if (classInfoCopy.getMetrics().getNumCoveredElements() > 0) {
                FullPackageInfo packageInfo = (FullPackageInfo) classInfo.getPackage();
                DefaultMutableTreeNode pkgNode = packageMapping.get(packageInfo);
                if (pkgNode == null) {
                    pkgNode = new DefaultMutableTreeNode(packageInfo);
                    packageMapping.put(packageInfo, pkgNode);
                }
                CoverageDataHolder classCoverage = new CoverageDataHolder(classInfo);
                DefaultMutableTreeNode classNode = new DefaultMutableTreeNode(classCoverage);
                pkgNode.add(classNode);
                List<DefaultMutableTreeNode> methodNodes = newArrayList();
                classCoverage.setCoverage(classInfoCopy.getMetrics().getPcCoveredElements());

                for (MethodInfo info : classInfoCopy.getMethods()) {
                    FullMethodInfo methodInfo = (FullMethodInfo)info;

                    if (methodInfo.getHitCount() > 0) {
                        final float methodCoverage =
                                methodInfo.isEmpty() ? -1f : methodInfo.getMetrics().getPcCoveredElements();

                        methodInfo.setDataProvider(uniqueTestDataProvider);
                        final float methodUniqueCoverage = methodInfo.getMetrics().getPcCoveredElements();
                        methodInfo.setDataProvider(null); // clean up BitSet references

                        final CoverageDataHolder methodCoverageData =
                                new CoverageDataHolder(methodInfo, methodCoverage, methodUniqueCoverage);

                        methodNodes.add(new DefaultMutableTreeNode(methodCoverageData));
                    }
                }

                tableModel.sortNodes(methodNodes);
                for (DefaultMutableTreeNode methodNode : methodNodes) {
                    classNode.add(methodNode);
                }

                classInfoCopy.setDataProvider(uniqueTestDataProvider);
                classCoverage.setUniqueCoverage(classInfoCopy.getMetrics().getPcCoveredElements());
            }
            return false;
        });

        if (flattenPackages) {
            for (DefaultMutableTreeNode pkgNode : packageMapping.values()) {
                rootNode.add(pkgNode);
            }
        } else {
            new PackageTreeBuilder(packageMapping).fillTree(rootNode);
        }

    }

    private static class PackageTreeBuilder {

        private final SimplePackageFragment root = new SimplePackageFragment("");
        private final Map<FullPackageInfo, DefaultMutableTreeNode> flatTree;

        PackageTreeBuilder(Map<FullPackageInfo, DefaultMutableTreeNode> flatTree) {
            //noinspection AssignmentToCollectionOrArrayFieldFromParameter
            this.flatTree = flatTree;

            for (DefaultMutableTreeNode pkgNode : flatTree.values()) {
                FullPackageInfo packageInfo = (FullPackageInfo) pkgNode.getUserObject();
                addPackageToTree(packageInfo);
            }
        }

        private void addPackageToTree(FullPackageInfo packageInfo) {
            String pkgFQName = packageInfo.getName();
            String[] nameParts = pkgFQName.split("\\.");
            SimplePackageFragment parent = root;

            for (String namePart : nameParts) {
                SimplePackageFragment fragment = parent.getChild(namePart);
                if (fragment == null) {
                    fragment = parent.add(namePart);
                }
                parent = fragment;
            }
            parent.setConcretePackage(packageInfo);
        }

        void fillTree(DefaultMutableTreeNode rootNode) {
            copy(rootNode, root);
        }

        void copy(DefaultMutableTreeNode toNode, SimplePackageFragment fromNode) {
            for (SimplePackageFragment fragment : fromNode.getChildren()) {
                final DefaultMutableTreeNode pkgNode = new DefaultMutableTreeNode(fragment);
                copy(pkgNode, fragment);
                toNode.add(pkgNode);
            }
            final FullPackageInfo packageInfo = fromNode.getConcretePackage();
            if (packageInfo != null) {
                // concrete package - copy Class nodes
                DefaultMutableTreeNode origNode = flatTree.get(packageInfo);
                while (!origNode.isLeaf()) {
                    toNode.add((MutableTreeNode) origNode.getChildAt(0)); // O(n^2) :-(
                }
            }
            fromNode.cleanup();
        }
    }


}
