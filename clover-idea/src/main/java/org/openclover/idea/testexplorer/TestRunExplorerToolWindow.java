package org.openclover.idea.testexplorer;

import com.intellij.ide.SelectInContext;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openclover.core.CloverDatabase;
import org.openclover.core.registry.CoverageDataReceptor;
import org.openclover.core.registry.entities.FullFileInfo;
import org.openclover.core.registry.entities.FullProjectInfo;
import org.openclover.core.registry.entities.TestCaseInfo;
import org.openclover.idea.IProjectPlugin;
import org.openclover.idea.ProjectPlugin;
import org.openclover.idea.SelectInCloverTarget;
import org.openclover.idea.SelectInCloverView;
import org.openclover.idea.config.ConfigChangeEvent;
import org.openclover.idea.config.ConfigChangeListener;
import org.openclover.idea.config.IdeaCloverConfig;
import org.openclover.idea.config.TestCaseLayout;
import org.openclover.idea.config.TestViewScope;
import org.openclover.idea.content.StatementsAggregatingVisitor;
import org.openclover.idea.coverage.CoverageListener;
import org.openclover.idea.coverage.EventListenerInstallator;
import org.openclover.idea.util.ui.CloverIcons;
import org.openclover.idea.util.vfs.VfsUtil;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Set;

public class TestRunExplorerToolWindow extends JPanel implements CoverageListener, ConfigChangeListener,
        FileEditorManagerListener, CaretListener {

    public static final String TOOL_WINDOW_ID = "Test Runs";
    private static final String TOOL_WINDOW_NAME = "Explorer";

    private static final Key<TestRunExplorerToolWindow> WINDOW_PROJECT_KEY =
            Key.create(TestRunExplorerToolWindow.class.getName());
    private ToolWindow toolWindow;
    private final Project project;
    private final TestRunBrowserPanel testRunBrowserPanel;
    private boolean flattenPackages;
    private TestCaseLayout testCaseLayout;
    private final SelectInCloverHandler selectInCloverHandler = new SelectInCloverHandler();

    TestRunExplorerToolWindow(final Project project) {
        this.project = project;

        setLayout(new BorderLayout());

        final ActionGroup actionGroup = (ActionGroup) ActionManager.getInstance().getAction("CloverPlugin.TestExplorerToolBar");
        final ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("CloverTestExplorer", actionGroup, true);
        add(toolbar.getComponent(), BorderLayout.NORTH);

        final CoverageContributionPanel coverageContributionPanel = new CoverageContributionPanel(project);
        testRunBrowserPanel = new TestRunBrowserPanel(project);
        testRunBrowserPanel.addTestCaseSelectionListener(coverageContributionPanel);

        final Splitter splitPane = new Splitter(true);
        splitPane.setFirstComponent(testRunBrowserPanel);
        splitPane.setSecondComponent(coverageContributionPanel);
        splitPane.setHonorComponentsMinimumSize(true);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                final Dimension dimension = e.getComponent().getSize();
                final boolean doVertical = dimension.getWidth() < dimension.getHeight();
                if (doVertical != splitPane.getOrientation()) {
                    splitPane.setOrientation(doVertical);
                }

            }
        });
        add(splitPane, BorderLayout.CENTER);
    }


    public static TestRunExplorerToolWindow getInstance(Project project) {
        TestRunExplorerToolWindow window = project.getUserData(WINDOW_PROJECT_KEY);
        if (window == null) {
            window = new TestRunExplorerToolWindow(project);
            project.putUserData(WINDOW_PROJECT_KEY, window);
        }

        return window;
    }

    private CloverDatabase currentCloverDatabase;

    @Override
    public synchronized void update(final CloverDatabase db) {
        currentCloverDatabase = db;
        ApplicationManager.getApplication().invokeLater(this::doUpdate);
    }

    private TestViewScope testViewScope;

    private void setTestViewScope(TestViewScope newScope) {
        if (testViewScope != newScope) {
            testViewScope = newScope;
            testRunBrowserPanel.showCoverageColumns(calculateCoverage && testViewScope != TestViewScope.STATEMENT);
        }
    }

    private boolean calculateCoverage;

    public void setCalculateCoverage(boolean newCalculateCoverage) {
        if (this.calculateCoverage != newCalculateCoverage) {
            this.calculateCoverage = newCalculateCoverage;
            testRunBrowserPanel.showCoverageColumns(calculateCoverage && testViewScope != TestViewScope.STATEMENT);
        }
    }

    @Override
    public void configChange(ConfigChangeEvent evt) {
        if (evt.hasPropertyChange(IdeaCloverConfig.ENABLED)) {
            final PropertyChangeEvent propertyChange = evt.getPropertyChange(IdeaCloverConfig.ENABLED);
            final boolean enabled = (Boolean) propertyChange.getNewValue();
            if (toolWindow != null) {
                toolWindow.setAvailable(enabled, null);
            }
        }

        if (evt.hasPropertyChange(IdeaCloverConfig.TEST_VIEW_SCOPE)) {
            final PropertyChangeEvent propertyChange = evt.getPropertyChange(IdeaCloverConfig.TEST_VIEW_SCOPE);
            setTestViewScope((TestViewScope) propertyChange.getNewValue());
            scheduleUpdate();
        }
        if (evt.hasPropertyChange(IdeaCloverConfig.CALCULATE_TEST_COVERAGE)) {
            final boolean calculate = (Boolean) evt.getPropertyChange(IdeaCloverConfig.CALCULATE_TEST_COVERAGE).getNewValue();
            setCalculateCoverage(calculate);
            if (calculate) {
                // no need for refresh if columns just disappeared
                scheduleUpdate();
            }
        }

        if (evt.hasPropertyChange(IdeaCloverConfig.FLATTEN_PACKAGES)) {
            flattenPackages = (Boolean) evt.getPropertyChange(IdeaCloverConfig.FLATTEN_PACKAGES).getNewValue();
            scheduleUpdate();
        }
        if (evt.hasPropertyChange(IdeaCloverConfig.TEST_CASE_LAYOUT)) {
            testCaseLayout = (TestCaseLayout) evt.getPropertyChange(IdeaCloverConfig.TEST_CASE_LAYOUT).getNewValue();
            scheduleUpdate();
        }


        if (evt.hasPropertyChange(IdeaCloverConfig.ALWAYS_COLLAPSE_TEST_CLASSES)) {
            testRunBrowserPanel.setAlwaysCollapseTestCases(
                    (Boolean) evt.getPropertyChange(IdeaCloverConfig.ALWAYS_COLLAPSE_TEST_CLASSES).getNewValue());
        }
        if (evt.hasPropertyChange(IdeaCloverConfig.ALWAYS_EXPAND_TEST_CLASSES)) {
            testRunBrowserPanel.setAlwaysExpandTestCases(
                    (Boolean) evt.getPropertyChange(IdeaCloverConfig.ALWAYS_EXPAND_TEST_CLASSES).getNewValue());
        }
    }

    @Nullable
    private CoverageDataReceptor findSelectedReceptor(@NotNull CloverDatabase cloverDatabase, TestViewScope testViewScope) {
        if (testViewScope == TestViewScope.GLOBAL) {
            return cloverDatabase.getAppOnlyModel();
        }
        final Editor selectedTextEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (selectedTextEditor == null) {
            return null;
        }
        final Document document = selectedTextEditor.getDocument();
        final VirtualFile currentFile = FileDocumentManager.getInstance().getFile(document);
        if (currentFile == null) {
            return null;
        }

        final String path = VfsUtil.getRootRelativeFilename(project, currentFile);

        final FullFileInfo fileInfo = (FullFileInfo) cloverDatabase.getFullModel().findFile(path);
        if (fileInfo == null) {
            return null;
        }
        if (testViewScope == TestViewScope.FILE) {
            return fileInfo;
        } else {
            final LogicalPosition caretPosition = selectedTextEditor.getCaretModel().getLogicalPosition();
            final StatementsAggregatingVisitor sav = new StatementsAggregatingVisitor(caretPosition);

            fileInfo.visitElements(sav);
            switch (testViewScope) {
                case CLASS:
                    return sav.getMostNarrowClass();
                case METHOD:
                    return sav.getMostNarrowMethod();
                case STATEMENT:
                    return sav.getMostNarrowStatement();
                default:
                    throw new IllegalStateException(testViewScope.toString());
            }
        }
    }

    private static final CoverageDataReceptor NULL_MARKER = new FullProjectInfo("NULL MARKER");
    private WeakReference<CoverageDataReceptor> previousReceptor;
    private boolean prevCoverageCalculated;
    private boolean prevFlattenPackages;
    private TestCaseLayout prevTestCaseLayout;

    private void saveState(CoverageDataReceptor receptor) {
        this.previousReceptor = new WeakReference<>(receptor == null ? NULL_MARKER : receptor);
        this.prevCoverageCalculated = calculateCoverage;
        this.prevFlattenPackages = flattenPackages;
        this.prevTestCaseLayout = testCaseLayout;
    }

    private boolean needsRefresh(CoverageDataReceptor receptor) {
        return previousReceptor == null ||
                (previousReceptor.get() != (receptor == null ? NULL_MARKER : receptor)) ||
                (prevCoverageCalculated != calculateCoverage) ||
                (prevFlattenPackages != flattenPackages) ||
                (prevTestCaseLayout != testCaseLayout);
    }

    private void doUpdate() {
        final CoverageDataReceptor receptor = currentCloverDatabase == null ? null :
                findSelectedReceptor(currentCloverDatabase, testViewScope);

        if (needsRefresh(receptor)) {
            saveState(receptor);
            if (receptor != null) {
                testRunBrowserPanel.update(currentCloverDatabase, receptor, calculateCoverage, flattenPackages, testCaseLayout);
            } else {
                testRunBrowserPanel.clean();
            }
        }
    }

    private boolean scheduledCalculationPending;

    /**
     * Schedule the {@link #doUpdate()} to be invokedLater().<p> All subsequent scheduleUpdates will be
     * ignored until the calculateTestCoverage() is actually run.<br> This serves 2 purposes: reduce the amount of
     * refreshes and make it run in UI thread.
     */
    public synchronized void scheduleUpdate() {
        if (!scheduledCalculationPending) {
            scheduledCalculationPending = true;
            ApplicationManager.getApplication().invokeLater(() -> {
                synchronized (TestRunExplorerToolWindow.this) {
                    // dont loose updates scheduled when calculateTestCoverage is already running
                    scheduledCalculationPending = false;
                }
                doUpdate();
            });
        }
    }

    public void register() {
        if (toolWindow == null) {
            final ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
            final IProjectPlugin projectPlugin = ProjectPlugin.getPlugin(project);
            final IdeaCloverConfig config = projectPlugin.getConfig();

            toolWindow = toolWindowManager.registerToolWindow(TOOL_WINDOW_ID, false, ToolWindowAnchor.LEFT);

            final ContentManager contentManager = toolWindow.getContentManager();
            final Content content = contentManager.getFactory().createContent(this, TOOL_WINDOW_NAME, false);
            contentManager.addContent(content);
            toolWindow.setIcon(CloverIcons.TOOL_WINDOW);
            toolWindow.setAvailable(config.isEnabled(), null);

            projectPlugin.getCoverageManager().addCoverageListener(this);
            config.addConfigChangeListener(this);

            // attach to topic with notifications about file editor changes (open / close file events)
            EventListenerInstallator.install(project, FileEditorManagerListener.FILE_EDITOR_MANAGER, this);

            setTestViewScope(config.getTestViewScope());
            setCalculateCoverage(config.isCalculateTestCoverage());
            testRunBrowserPanel.setAlwaysCollapseTestCases(config.isAlwaysCollapseTestClasses());
            testRunBrowserPanel.setAlwaysExpandTestCases(config.isAlwaysExpandTestClasses());
            flattenPackages = config.isFlattenPackages();
            testCaseLayout = config.getTestCaseLayout();

            SelectInCloverTarget.getInstance(project).addView(selectInCloverHandler, 1);
        }
    }

    public void unregister() {
        SelectInCloverTarget.getInstance(project).removeView(selectInCloverHandler);
        if (toolWindow != null) {
            final IProjectPlugin projectPlugin = ProjectPlugin.getPlugin(project);

            projectPlugin.getCoverageManager().removeCoverageListener(this);
            projectPlugin.getConfig().removeConfigChangeListener(this);

            ToolWindowManager.getInstance(project).unregisterToolWindow(TOOL_WINDOW_ID);
        }
    }

    @Override
    public void fileOpened(FileEditorManager fileEditorManager, VirtualFile virtualFile) {
        // selectionChanged will be called first, no need to scheduleUpdate here
        for (final FileEditor editor : fileEditorManager.getEditors(virtualFile)) {
            if (editor instanceof TextEditor) {
                final TextEditor textEditor = (TextEditor) editor;
                textEditor.getEditor().getCaretModel().addCaretListener(this);
            }
        }
    }

    @Override
    public void fileClosed(FileEditorManager fileEditorManager, VirtualFile virtualFile) {
        scheduleUpdate(); // in case this is the last one - selectionChanged is called too early

        // don't unregister, the editor is going away anyway. right?
    }

    @Override
    public void selectionChanged(FileEditorManagerEvent event) {
        scheduleUpdate();
    }

    @Override
    public void caretPositionChanged(CaretEvent event) {
        // TODO what about multiple cursors and "show per line" ???
        if (testViewScope != TestViewScope.GLOBAL && testViewScope != TestViewScope.FILE) {
            scheduleUpdate();
        }
    }

    /** @since IDEA 13.1 in CaretListener */
    public void caretAdded(CaretEvent caretEvent) {

    }

    /** @since IDEA 13.1 in CaretListener */
    public void caretRemoved(CaretEvent caretEvent) {

    }

    interface TestCaseSelectionListener {
        void valueChanged(TestCaseInfo testCaseInfo);
    }

    private class SelectInCloverHandler implements SelectInCloverView {
        private PsiMethod getMethod(PsiElement element) {
            PsiElement e = element;
            while (e != null && !(e instanceof PsiMethod)) {
                e = e.getParent();
            }
            return (PsiMethod) e;
        }

        @Override
        public boolean canSelect(SelectInContext context) {
            final Object selector = context.getSelectorInFile();
            return findTestCase(selector) != null;
        }

        private TestCaseInfo findTestCase(Object selector) {
            final CloverDatabase database = currentCloverDatabase;
            if (database != null && selector instanceof PsiElement) {
                final PsiMethod method = getMethod((PsiElement) selector);
                if (method != null) {
                    final PsiClass clazz = method.getContainingClass();
                    @SuppressWarnings("unchecked")
                    final Set<TestCaseInfo> tests = database.getCoverageData().getTests();
                    for (TestCaseInfo test : tests) {
                        if (test.getRuntimeTypeName().equals(clazz.getQualifiedName()) &&
                                test.getSourceMethod().getSimpleName().equals(method.getName())) {
                            return test;
                        }
                    }
                }
            }
            return null;
        }

        @Override
        public boolean selectIn(SelectInContext context) {
            final Object selector = context.getSelectorInFile();
            final TestCaseInfo testCase = findTestCase(selector);
            if (testCase != null) {
                final Set<TestCaseInfo> tests = Collections.singleton(testCase);
                testRunBrowserPanel.update(currentCloverDatabase, tests, flattenPackages, testCaseLayout);
                toolWindow.activate(null);
                final IdeaCloverConfig ideaCloverConfig = ProjectPlugin.getPlugin(project).getConfig();
                ideaCloverConfig.setTestViewScope(TestViewScope.METHOD);
                ideaCloverConfig.notifyListeners();
            }

            return testCase != null;
        }
    }
}

