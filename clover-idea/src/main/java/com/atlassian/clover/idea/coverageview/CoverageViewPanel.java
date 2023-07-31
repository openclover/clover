package com.atlassian.clover.idea.coverageview;

import com.atlassian.clover.CloverDatabase;
import com.atlassian.clover.api.registry.ClassInfo;
import com.atlassian.clover.api.registry.HasMetrics;
import com.atlassian.clover.api.registry.MethodInfo;
import com.atlassian.clover.api.registry.MethodSignatureInfo;
import com.atlassian.clover.api.registry.ParameterInfo;
import com.atlassian.clover.idea.NodeWrapperSelectionListener;
import com.atlassian.clover.idea.ProjectPlugin;
import com.atlassian.clover.idea.SelectInCloverTarget;
import com.atlassian.clover.idea.SelectInCloverView;
import com.atlassian.clover.idea.actions.Constants;
import com.atlassian.clover.idea.config.ConfigChangeEvent;
import com.atlassian.clover.idea.config.ConfigChangeListener;
import com.atlassian.clover.idea.config.IdeaCloverConfig;
import com.atlassian.clover.idea.coverage.CoverageManager;
import com.atlassian.clover.idea.coverage.CoverageTreeFilter;
import com.atlassian.clover.idea.coverage.CoverageTreeListener;
import com.atlassian.clover.idea.coverage.CoverageTreeModel;
import com.atlassian.clover.idea.coverage.EventListenerInstallator;
import com.atlassian.clover.idea.feature.CloverFeatures;
import com.atlassian.clover.idea.report.cloud.CloudVirtualFile;
import com.atlassian.clover.idea.report.jfc.FullyCoveredFilter;
import com.atlassian.clover.idea.treetables.TreeTableModelFactory;
import com.atlassian.clover.idea.treetables.TreeTablePanel;
import com.atlassian.clover.idea.util.ModelScope;
import com.atlassian.clover.idea.util.ui.TreeExpansionHelper;
import com.atlassian.clover.idea.util.vfs.VfsUtil;
import com.atlassian.clover.registry.FileInfoRegion;
import com.atlassian.clover.registry.entities.FullFileInfo;
import com.atlassian.clover.registry.entities.FullPackageInfo;
import com.atlassian.clover.registry.entities.PackageFragment;
import com.intellij.ide.DataManager;
import com.intellij.ide.SelectInContext;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerAdapter;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.util.Collection;

import static org.openclover.util.Sets.newHashSet;

public class CoverageViewPanel extends TreeTablePanel implements ConfigChangeListener, CoverageTreeListener,
        DataProvider {
    private boolean autoScrollToSource;
    private boolean autoScrollFromSource;
    private boolean flattenPackages;
    private ModelScope currentModelScope;
    private boolean hideFullyCovered;

    private final Project project;
    private final SelectionChangeNotifier selectionChangeNotifier = new SelectionChangeNotifier();
    private final PackageInCloudAutoSelector packageInCloudAutoSelector;

    private CoverageTreeModel coverageTreeModel;

    private final String containingToolWindowId;
    private HasMetrics selectionOverride; // use when forcing tree rebuild to select a Class/Method that has not been visible

    public CoverageViewPanel(Project project, String containingToolWindowId) {
        super(project, TreeTableModelFactory.getCoverageTreeTableModel(null));
        this.project = project;
        this.containingToolWindowId = containingToolWindowId;
        packageInCloudAutoSelector = new PackageInCloudAutoSelector(project);

        setLayout(new BorderLayout());
        add(new JScrollPane(treeTableView), BorderLayout.CENTER);

        initListeners();
    }

    private void initListeners() {

        final AutoScrollToSourceHandler scrollToSourceHandler = new AutoScrollToSourceHandler();
        treeTableView.addMouseListener(scrollToSourceHandler);

        treeTableView.getTree().addTreeSelectionListener(selectionChangeNotifier);

        final IdeaCloverConfig config = ProjectPlugin.getPlugin(project).getConfig();
        config.addConfigChangeListener(this);


        final CoverageManager coverageManager = ProjectPlugin.getPlugin(project).getCoverageManager();
        coverageManager.addCoverageTreeListener(this);

        autoScrollToSource = config.isAutoScroll();
        autoScrollFromSource = config.isAutoScrollFromSource();
        flattenPackages = config.isFlattenPackages();
        currentModelScope = config.getModelScope();
        hideFullyCovered = config.isHideFullyCovered();

        // attach to topic with notifications about file editor changes (open / close file events)
        EventListenerInstallator.install(project, FileEditorManagerListener.FILE_EDITOR_MANAGER, new AutoScrollFromSourceHandler());

        enableAutoCloudPackage(config.isAutoViewInCloudReport());

        SelectInCloverTarget.getInstance(project).addView(new SelectInCloverHandler(), 9);

        update(coverageManager.getCoverageTree());
    }

    private void enableAutoCloudPackage(boolean enable) {
        if (enable) {
            selectionChangeNotifier.addListener(packageInCloudAutoSelector);
        } else {
            selectionChangeNotifier.removeListener(packageInCloudAutoSelector);
        }
    }

    @Override
    public void configChange(ConfigChangeEvent evt) {
        if (evt.hasPropertyChange(IdeaCloverConfig.AUTO_SCROLL_TO_SOURCE)) {
            final PropertyChangeEvent propertyChange = evt.getPropertyChange(IdeaCloverConfig.AUTO_SCROLL_TO_SOURCE);
            autoScrollToSource = (Boolean) propertyChange.getNewValue();
        }

        if (evt.hasPropertyChange(IdeaCloverConfig.AUTO_SCROLL_FROM_SOURCE)) {
            final PropertyChangeEvent propertyChange = evt.getPropertyChange(IdeaCloverConfig.AUTO_SCROLL_FROM_SOURCE);
            autoScrollFromSource = (Boolean) propertyChange.getNewValue();
        }

        if (evt.hasPropertyChange(IdeaCloverConfig.FLATTEN_PACKAGES)) {
            final PropertyChangeEvent propertyChange = evt.getPropertyChange(IdeaCloverConfig.FLATTEN_PACKAGES);
            flattenPackages = (Boolean) propertyChange.getNewValue();
            update(coverageTreeModel);
        }

        if (evt.hasPropertyChange(IdeaCloverConfig.MODEL_SCOPE)) {
            final PropertyChangeEvent propertyChange = evt.getPropertyChange(IdeaCloverConfig.MODEL_SCOPE);
            currentModelScope = (ModelScope) propertyChange.getNewValue();
            update(coverageTreeModel);
        }

        if (evt.hasPropertyChange(IdeaCloverConfig.CLOUD_AUTO_VIEW)) {
            final PropertyChangeEvent propertyChange = evt.getPropertyChange(IdeaCloverConfig.CLOUD_AUTO_VIEW);
            enableAutoCloudPackage((Boolean) propertyChange.getNewValue());
        }

        if (evt.hasPropertyChange(IdeaCloverConfig.HIDE_FULLY_COVERED)) {
            final PropertyChangeEvent propertyChange = evt.getPropertyChange(IdeaCloverConfig.HIDE_FULLY_COVERED);
            hideFullyCovered = (Boolean) propertyChange.getNewValue();
            update(coverageTreeModel);
        }

    }

    private static final CoverageTreeFilter FULLY_COVERED_FILTER = new FullyCoveredFilter();

    @Override
    public void update(final CoverageTreeModel model) {

        coverageTreeModel = model;
        // UI Changes can only be made from the EventDispatch thread.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (model == null) {
                    selectionChangeNotifier.notify(null);
                    tableModel.setRoot(new DefaultMutableTreeNode(" Coverage data not loaded."));
                } else {
                    if (hideFullyCovered) {
                        if (!model.hasFilter(FULLY_COVERED_FILTER)) {
                            model.addFilter(FULLY_COVERED_FILTER); // see above
                        }
                    } else {
                        if (model.hasFilter(FULLY_COVERED_FILTER)) {
                            model.removeFilter(FULLY_COVERED_FILTER); // removeFilter has side effect of clearing the tree, avoid when not necessary
                        }
                    }
                    // turn off autoScrolling - refreshing will change the coverage
                    // tree but should not trigger the autoScroll features.
                    final boolean toSavedValue = autoScrollToSource;
                    try {
                        autoScrollToSource = false;
                        TreeExpansionHelper teh = new TreeExpansionHelper(treeTableView.getTree());

                        // create new tree.
                        tableModel.setRoot(model.getClassTree(!flattenPackages, currentModelScope));

                        teh.restore(treeTableView.getTree());
                        if (selectionOverride != null) {
                            selectHasMetrics(selectionOverride);
                            selectionOverride = null;
                        }
                    } finally {
                        autoScrollToSource = toSavedValue;
                    }
                }
            }
        });

    }

    public void ensureSelectionVisible() {
        final JTree tree = treeTableView.getTree();
        tree.scrollPathToVisible(tree.getSelectionPath());
    }

    public void addElementSelectionListener(NodeWrapperSelectionListener listener) {
        selectionChangeNotifier.addListener(listener);
    }

    private void scrollToSource(TreePath path) {
        final CoverageTreeModel.NodeWrapper node = CoverageTreeModel.getNodeForPath(path);
        if (node == null || !(node.getHasMetrics() instanceof FileInfoRegion)) {
            return;
        }
        final FileInfoRegion region = (FileInfoRegion) node.getHasMetrics();

        final File srcFile = ((FullFileInfo) region.getContainingFile()).getPhysicalFile();
        final VirtualFile vf = LocalFileSystem.getInstance().findFileByIoFile(srcFile);
        if (vf == null) {
            return;
        }

        // about to open a file, so turn on coverage reporting.
        ProjectPlugin.getPlugin(this.project).getFeatureManager().setCategoryEnabled(CloverFeatures.CLOVER_REPORTING, true);

        final FileEditorManager fem = FileEditorManager.getInstance(project);

        final int line = region.getStartLine() - 1;
        final int column = region.getStartColumn() - 1;

        final OpenFileDescriptor ofd = new OpenFileDescriptor(project, vf, line, column);
        // disable scroll from source;
        final boolean savedValue = autoScrollFromSource;
        try {
            autoScrollFromSource = false;
            fem.openTextEditor(ofd, true);
        } finally {
            // reinable autoscroll from source
            autoScrollFromSource = savedValue;
        }
    }

    TreePath getPathForFile(VirtualFile file) {
        final File f = VfsUtil.convertToFile(file);
        return f == null || coverageTreeModel == null ? null : coverageTreeModel.getPathForFile(f);
    }

    @Nullable
    TreePath getPathForHasMetrics(@Nullable HasMetrics hasMetrics) {
        return hasMetrics == null || coverageTreeModel == null ? null : coverageTreeModel.getPathForHasMetrics(hasMetrics);
    }

    /**
     * Scroll From Source: select and scroll to the specified file within
     * the coverage tree. If the file does not have coverage, then no change
     * is made.
     *
     * @param file currently selected (open) file
     * @return true if file was found
     */
    boolean scrollFromSource(VirtualFile file) {
        return scrollToPath(getPathForFile(file));
    }

    boolean selectHasMetrics(@Nullable HasMetrics hasMetrics) {
        return scrollToPath(getPathForHasMetrics(hasMetrics));
    }

    private boolean scrollToPath(@Nullable TreePath path) {
        if (path != null) {
            final boolean savedValue = autoScrollToSource;
            try {
                // while we are updating the selected node, we need to disable the
                // autoScrollToSource functionality
                autoScrollToSource = false;

                // if node found, select it.
                treeTableView.getTree().expandPath(path.getParentPath());
                final int row = treeTableView.getTree().getRowForPath(path);
                // make sure treeTableView actually displays the selection - ui + scroll
                treeTableView.getSelectionModel().setSelectionInterval(row, row);
                final Rectangle r = treeTableView.getCellRect(row, 0, false);
                treeTableView.scrollRectToVisible(r);

            } finally {
                autoScrollToSource = savedValue;
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    @Nullable
    public Object getData(@NonNls String dataId) {
        if (Constants.SELECTED_HAS_METRICS.getName().equals(dataId)) {
            final TreePath selection = treeTableView.getTree().getSelectionPath();
            final Object object = selection == null ? null : ((DefaultMutableTreeNode) selection.getLastPathComponent()).getUserObject();
            return object instanceof CoverageTreeModel.NodeWrapper ? ((CoverageTreeModel.NodeWrapper) object).getHasMetrics() : null;
        }

        return null;
    }

    /**
     * The AutoScrollToSource functionality supports the selection of nodes within
     * the coverage tree and opening the selected source file.
     */
    private class AutoScrollToSourceHandler extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() >= 2 || autoScrollToSource) {
                final TreePath path = treeTableView.getTree().getPathForLocation(e.getX(), e.getY());
                scrollToSource(path);
            }
        }
    }

    /**
     * The AutoScrollFromSource functionality supports the CloverToolWindow
     * updating its selected node based upon which file is currently visible
     * in the editor.
     */
    private class AutoScrollFromSourceHandler extends FileEditorManagerAdapter implements CaretListener {

        @Override
        public void caretPositionChanged(CaretEvent e) {
            // TODO what about multiple cursors introduced in IDEA 13.1 ?
            if (autoScrollFromSource && isShowing()) {
                final DataContext dataContext = DataManager.getInstance().getDataContext(e.getEditor().getComponent());
                final PsiFile psiFile = DataKeys.PSI_FILE.getData(dataContext);
                if (psiFile != null) {
                    final int pos = e.getEditor().getCaretModel().getOffset();
                    final PsiElement selectedPsi = psiFile.getViewProvider().findElementAt(pos, psiFile.getLanguage());
                    selectHasMetrics(getElement(selectedPsi));
                }
            }
        }

        /** @since IDEA 13.1 in CaretListener */
        public void caretAdded(CaretEvent caretEvent) {

        }

        /** @since IDEA 13.1 in CaretListener */
        public void caretRemoved(CaretEvent caretEvent) {

        }

        @Override
        public void fileOpened(FileEditorManager source, VirtualFile file) {
            for (final FileEditor editor : source.getEditors(file)) {
                if (editor instanceof TextEditor) {
                    final TextEditor textEditor = (TextEditor) editor;
                    textEditor.getEditor().getCaretModel().addCaretListener(this);
                }
            }
        }
    }

    @Nullable
    private static PsiElement getEnclosingMethodOrClass(@Nullable PsiElement element) {
        PsiElement e = element;
        while (e != null && !(e instanceof PsiMethod || e instanceof PsiClass)) {
            e = e.getParent();
        }
        return e;
    }

    @Nullable
    private HasMetrics getElement(@Nullable PsiElement selectedElement) {
        if (coverageTreeModel == null) {
            return null;
        }
        final CloverDatabase database = coverageTreeModel.getCloverDatabase();
        if (database == null) {
            return null;
        }

        final PsiElement selected = getEnclosingMethodOrClass(selectedElement);
        if (selected == null) {
            return null;
        }

        final PsiClass psiClass = selected instanceof PsiClass
                ? (PsiClass) selected
                : ((PsiMethod) selected).getContainingClass();
        
        if (psiClass == null) {
            return null;
        }
        final PsiMethod psiMethod = selected instanceof PsiMethod ? (PsiMethod) selected : null;

        final ClassInfo classInfo = database.getFullModel().findClass(psiClass.getQualifiedName());
        if (classInfo == null) {
            return null;
        }

        if (psiMethod == null) {
            return classInfo;
        } else {
            final PsiParameter[] psiParameters = psiMethod.getParameterList().getParameters();
            final String psiName = psiMethod.getName();

            methodsInClass:
            for (MethodInfo methodInfo : classInfo.getMethods()) {
                final MethodSignatureInfo signature = methodInfo.getSignature();
                final ParameterInfo[] parameters = signature.getParameters();
                if (parameters != null && parameters.length == psiParameters.length && psiName.equals(signature.getName())) {
                    for (int i = 0; i < parameters.length; i++) {
                        final PsiType psiType = psiParameters[i].getType();
                        final String sigType = parameters[i].getType();
                        if (!sigType.equals(psiType.getCanonicalText())
                                && !sigType.equals(psiType.getPresentableText())) {
                            continue methodsInClass;
                        }
                    }
                    return methodInfo;
                }

            }
            return classInfo;
        }

    }

    private class SelectInCloverHandler implements SelectInCloverView {

        @Override
        public boolean canSelect(SelectInContext context) {
            final Object selector = context.getSelectorInFile();
            return selector instanceof PsiElement && getElement((PsiElement) selector) != null;
        }

        @Override
        public boolean selectIn(SelectInContext context) {
            final Object selector = context.getSelectorInFile();
            if (!(selector instanceof PsiElement)) {
                return false;
            }
            final HasMetrics selected = getElement((PsiElement) selector);
            if (selected == null) {
                return false;
            }

            ToolWindowManager.getInstance(project).getToolWindow(containingToolWindowId).activate(null);

            final boolean done = selectHasMetrics(selected);
            if (!done && currentModelScope != ModelScope.ALL_CLASSES) {
                // getElement found it in the full model
                selectionOverride = selected;
                final IdeaCloverConfig cloverConfig = ProjectPlugin.getPlugin(project).getConfig();
                cloverConfig.setModelScope(ModelScope.ALL_CLASSES);
                cloverConfig.notifyListeners();
                return false;
            }

            return done;
        }
    }
}

class SelectionChangeNotifier implements TreeSelectionListener {
    private Collection<NodeWrapperSelectionListener> listeners = newHashSet();

    @Override
    public void valueChanged(TreeSelectionEvent evt) {
        if (evt.isAddedPath()) {
            final TreePath path = evt.getPath();
            final CoverageTreeModel.NodeWrapper node = CoverageTreeModel.getNodeForPath(path);
            notify(node);
        }
    }

    void notify(CoverageTreeModel.NodeWrapper node) {
        for (NodeWrapperSelectionListener listener : listeners) {
            listener.elementSelected(node);
        }
    }

    public void addListener(NodeWrapperSelectionListener listener) {
        listeners.add(listener);
    }

    public void removeListener(NodeWrapperSelectionListener listener) {
        listeners.remove(listener);
    }
}

class PackageInCloudAutoSelector implements NodeWrapperSelectionListener {
    private final Project project;

    public PackageInCloudAutoSelector(Project project) {
        this.project = project;
    }

    @Override
    public void elementSelected(CoverageTreeModel.NodeWrapper nodeWrapper) {
        final HasMetrics hasMetrics = nodeWrapper != null ? nodeWrapper.getHasMetrics() : null;
        if (hasMetrics instanceof FullPackageInfo || hasMetrics instanceof PackageFragment) {
            if (project != null) {
                final CloudVirtualFile vf = CloudVirtualFile.getInstance(project);
                vf.setSelectedElement(hasMetrics);
            }
        }
    }
}
