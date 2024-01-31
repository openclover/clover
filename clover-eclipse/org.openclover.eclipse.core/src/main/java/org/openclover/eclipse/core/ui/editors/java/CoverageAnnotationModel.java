package org.openclover.eclipse.core.ui.editors.java;

import com.atlassian.clover.api.registry.BranchInfo;
import com.atlassian.clover.api.registry.ClassInfo;
import com.atlassian.clover.api.registry.MethodInfo;
import com.atlassian.clover.api.registry.SourceInfo;
import com.atlassian.clover.CloverDatabase;
import com.atlassian.clover.api.registry.StatementInfo;
import com.atlassian.clover.context.ContextSet;
import com.atlassian.clover.registry.entities.FullFileInfo;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.PluginOptionKeys;
import org.openclover.eclipse.core.ui.editors.java.annotations.strategies.CoverageAnnotationBuilder;
import org.openclover.eclipse.core.ui.editors.java.annotations.strategies.space.CoverageAnnotationSpaceBuilder;
import org.openclover.eclipse.core.ui.projects.DatabaseChangeListener;
import org.openclover.eclipse.core.ui.projects.DatabaseChangeEvent;
import org.openclover.eclipse.core.projects.CloverProject;
import org.openclover.eclipse.core.projects.model.MetricsScope;
import org.openclover.eclipse.core.settings.InstallationSettings;
import com.atlassian.clover.registry.entities.LineInfo;
import com.atlassian.clover.registry.entities.TestCaseInfo;
import org.openclover.util.function.Function;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModelEvent;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.jface.text.source.IAnnotationModelListener;
import org.eclipse.jface.text.source.IAnnotationModelListenerExtension;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.swt.widgets.Display;
import org.openclover.util.function.TransformingIterator;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.BitSet;
import java.util.Map;
import java.util.regex.Pattern;

import static org.openclover.util.Lists.newLinkedList;
import static org.openclover.util.Sets.newHashSet;
import static org.openclover.util.Sets.newTreeSet;

public class CoverageAnnotationModel implements IAnnotationModel, IDocumentListener, DatabaseChangeListener, AnnotationDisplayListener, ISchedulingRule {
    private static final Object MODEL_KEY = new Object();
    private static final CoverageEdgeComparator EDGE_COMPARATOR = new CoverageEdgeComparator();
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("^(\\s)+$");
    private static final List<CoverageAnnotation> ANNOTATIONS_OFF =
        Collections.unmodifiableList(new LinkedList<>());

    private final ITextEditor editor;
    private final IDocument document;
    private final EditorCoverageSynchronizer synchronizer;
    private final List<IAnnotationModelListener> listeners;
    private final AnnotationUpdateRule updateRule;
    private List<CoverageAnnotation> annotations;
    private ILineCoverageModel lineCoverageModel;
    private int connectionCount;

    public CoverageAnnotationModel(ITextEditor editor, IDocument document, EditorCoverageSynchronizer synchronizer) {
        this.editor = editor;
        this.document = document;
        this.synchronizer = synchronizer;
        this.listeners = Collections.synchronizedList(new LinkedList<>());
        this.annotations = Collections.emptyList();
        this.updateRule = new AnnotationUpdateRule();
    }

    public void refreshAsync() {
        new AnnotationCalculationJob(updateRule) {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    refreshSync();
                } catch (Throwable t) {
                    //Catch anything here so as not to corrupt the editor
                    CloverPlugin.logError("Error when rebuilding annotation model for changed document", t);
                }
                //Return OK status so as to not interrupt users with error message boxes
                return Status.OK_STATUS;
            }
        }.schedule();
    }

    private void refreshSync() throws BadLocationException {
        CloverPlugin.logVerbose("Refreshing annotations for editor " + editor.getTitle());
        fireAnnotationsChanged(rebuildAnnotations(editor.isDirty()));
    }

    private CoverageAnnotationFilter syncFilter(CloverDatabase database, FullFileInfo fileInfo, Map<TestCaseInfo, BitSet> tcisAndHitsForFile, ContextSet blockFilter) {
        IResource editedResource = (IResource) editor.getEditorInput().getAdapter(IResource.class);
        if (editedResource != null) {
            return CoverageAnnotationFilter.loadFor(
                database,
                fileInfo,
                tcisAndHitsForFile,
                editedResource,
                CloverPlugin.getInstance().getInstallationSettings().getEditorCoverageStyle() == InstallationSettings.Values.SHOW_ONLY_UNCOVERED_IN_EDITORS,
                blockFilter);
        } else {
            return CoverageAnnotationFilter.NULL;
        }
    }

    @Override
    public void connect(IDocument document) {
        try {
            if (document == this.document) {
                if (incrementConnectionCount()) {
                    createAnnotations();
                    document.addDocumentListener(this);
                    CloverPlugin.getInstance().getCoverageMonitor().addCoverageChangeListener(this);
                    synchronizer.addAnnotationDisplayListener(this);
                }
                createDocumentPositions();
            }
        } catch (Throwable t) {
            //Catch anything here so as not to corrupt the editor
            CloverPlugin.logError("Error when annotation model connects to source file", t);
        }
    }

    @Override
    public void disconnect(IDocument document) {
        try {
            if (document == this.document) {
                clearDocumentPositions();
                if (decrementConnectionCount()) {
                    CloverPlugin.getInstance().getCoverageMonitor().removeCoverageChangeListener(this);
                    synchronizer.removeAnnotationDisplayListener(this);
                    document.removeDocumentListener(this);
                    new AnnotationCalculationJob(updateRule) {
                        @Override
                        protected IStatus run(IProgressMonitor monitor) {
                            try {
                                fireAnnotationsChanged(turnAnnotationsOff());
                            } catch (Throwable t) {
                                //Catch anything here so as not to corrupt the editor
                                CloverPlugin.logError("Error when rebuilding annotation model for changed document", t);
                            }
                            //Return OK status so as to not interrupt users with error message boxes
                            return Status.OK_STATUS;
                        }
                    }.schedule();
                }
            }
        } catch (Throwable t) {
            //Catch anything here so as not to corrupt the editor
            CloverPlugin.logError("Error when annotation model disconnects from source file", t);
        }
    }

    private void createDocumentPositions() {
        try {
            List<CoverageAnnotation> annotations = this.annotations;
            for (CoverageAnnotation annotation : annotations) {
                document.addPosition(annotation.getPosition());
            }
        } catch (BadLocationException e) {
            CloverPlugin.logError("Unable to add position for coverage annotation", e);
        }
    }

    private void clearDocumentPositions() {
        List<CoverageAnnotation> annotations = this.annotations;
        for (CoverageAnnotation annotation : annotations) {
            document.removePosition(annotation.getPosition());
        }
    }

    private boolean incrementConnectionCount() {
        return connectionCount++ == 0;
    }

    private boolean decrementConnectionCount() {
        return --connectionCount == 0;
    }

    private FullFileInfo cloverFileInfoForEditorInput() {
        IJavaElement element = JavaUI.getEditorInputJavaElement(editor.getEditorInput());
        return
            element != null
                ? (FullFileInfo)MetricsScope.FULL.getHasMetricsFor(element, FullFileInfo.class)
                : null;
    }

    public CloverDatabase cloverDatabaseForEditorInput() {
        CloverProject project = cloverProjectForEditorInput();

        return project == null ? null : project.getModel().getDatabase();
    }

    private CloverProject cloverProjectForEditorInput() {
        IJavaElement element = JavaUI.getEditorInputJavaElement(editor.getEditorInput());

        return cloverProjectFor(element);
    }

    private CloverProject cloverProjectFor(IJavaElement element) {
        return element != null
            ? ((CloverProject) element.getAdapter(CloverProject.class))
            : null;
    }

    public ContextSet blockFilterForEditorInput() {
        CloverProject project = cloverProjectForEditorInput();

        return project == null ? new ContextSet() : project.getSettings().getContextFilter();
    }

    private AnnotationModelEvent createAnnotations() throws BadLocationException {
        return createAnnotations(new AnnotationModelEvent(this, true));
    }

    @SuppressWarnings("unchecked")
    private AnnotationModelEvent createAnnotations(AnnotationModelEvent event) throws BadLocationException {
        CloverDatabase database = cloverDatabaseForEditorInput();
        FullFileInfo fileInfo = cloverFileInfoForEditorInput();

        if (database != null && fileInfo != null && fileInfo.validatePhysicalFile()) {
            //Test cases and their hits for this FileInfo - important to keep around as expensive to calculate often
            Map<TestCaseInfo,BitSet> tcisAndHitsForFile = Collections.unmodifiableMap(database.mapTestsAndCoverageForFile(fileInfo));

            //Used to filter out uninteresting SourceRegions
            CoverageAnnotationFilter filter =
                syncFilter(
                    database, fileInfo,
                    tcisAndHitsForFile,
                    blockFilterForEditorInput());

            try {
                LineInfo[] lineInfos = fileInfo.getLineInfo(true, true);
                lineCoverageModel = new LineCoverageModelImpl(database, fileInfo);
                final Set<CoverageEdge> beginningsThisLine = newHashSet();
                final Set<CoverageEdge>[] endingsPerLine = (Set<CoverageEdge>[])new Set[lineInfos.length];
                final CoverageAnnotationBuilder annotationBuilder = new CoverageAnnotationSpaceBuilder(database, document, tcisAndHitsForFile);

                for (int i = 0; i < lineInfos.length; i++) {
                    LineInfo lineInfo = lineInfos[i];

                    //We're interested in beginning or ending edges for this line
                    if (lineInfo != null || endingsPerLine[i] != null) {
                        ClassInfo[] classInfos = lineInfo == null ? null : lineInfo.getClassStarts();
                        MethodInfo[] methodInfos = lineInfo == null ? null : lineInfo.getMethodStarts();
                        StatementInfo[] statementInfos = lineInfo == null ? null : lineInfo.getStatements();
                        BranchInfo[] branchInfos = lineInfo == null ? null : lineInfo.getBranches();

                        assembleEdges(
                            filter,
                            new SourceInfo[][]{
                                classInfos,
                                methodInfos,
                                statementInfos,
                                branchInfos},
                            beginningsThisLine,
                            endingsPerLine);

                        //Order edges based on their column position etc
                        SortedSet<CoverageEdge> edgesThisLine = newTreeSet(EDGE_COMPARATOR);
                        edgesThisLine.addAll(beginningsThisLine);
                        edgesThisLine.addAll(endingsPerLine[i] == null ? Collections.<CoverageEdge>emptySet() : endingsPerLine[i]);

                        //Register the edge with the builder in sequence
                        for (CoverageEdge edgeThisLine : edgesThisLine) {
                            edgeThisLine.register(annotationBuilder, filter);
                        }

                        //Reset collections for next iteration
                        beginningsThisLine.clear();
                        Set<CoverageEdge> endingsForThisLine = endingsPerLine[i];
                        if (endingsForThisLine != null) {
                            endingsForThisLine.clear();
                            endingsPerLine[i] = null;
                        }
                    }
                }

                //TODO: Hack! We need the annotations in order so we can shift them and their neighbour
                //around if we detect ^(\s)+
                SortedSet<CoverageAnnotation> annotations = newTreeSet(
                        (annotation1, annotation2) -> {
                            Position position1 = annotation1.getPosition();
                            Position position2 = annotation2.getPosition();
                            if (position1.getOffset() < position2.getOffset()) {
                                return -1;
                            } else if (position1.getOffset() > position2.getOffset()) {
                                return 1;
                            } else {
                                if (position1.getLength() < position2.getLength()) {
                                    return -1;
                                } else if (position1.getLength() > position2.getLength()) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            }
                        });

                annotationBuilder.toAnnotations(annotations);

                //TODO: Fix this hack. We shouldn't be consuming ^(\s)+$ here
                CoverageAnnotation previous = null;
                for (CoverageAnnotation current : annotations) {
                    IRegion lineRegion = document.getLineInformationOfOffset(current.getPosition().getOffset());
                    int lineStartOffset = lineRegion.getOffset();

                    String leadingSpace = document.get(lineRegion.getOffset(), current.getPosition().getOffset() - lineRegion.getOffset());
                    if (WHITESPACE_PATTERN.matcher(leadingSpace).matches()) {
                        int displacement = (current.getPosition().getOffset() - lineStartOffset);

                        current.reposition(lineStartOffset, current.getPosition().getLength() + displacement);
                        if (previous != null && previous.encloses(lineStartOffset)) {
                            previous.reposition(
                                    previous.getPosition().getOffset(),
                                    Math.max(0, previous.getPosition().getLength() - displacement));
                        }
                    }
                    previous = current;
                }

                for (CoverageAnnotation annotation : annotations) {
                    event.annotationAdded(annotation);
                }
                this.annotations = Collections.unmodifiableList(newLinkedList(annotations));

            } catch (BadLocationException e) {
                CloverPlugin.logError("Unable to annotate " + fileInfo.getName(), e);
                throw e;
            }
        }

        return event;
    }

    private void assembleEdges(CoverageAnnotationFilter filter, SourceInfo[][] collectionOfInfos, Set<CoverageEdge> beginnings, Set<CoverageEdge>[] endingsByLine) {
        boolean shouldLogErrors = CloverPlugin.isLoggingDebugFor(PluginOptionKeys.ANNOTATION_ERRORS_DEBUG);

        for (SourceInfo[] infos : collectionOfInfos) {
            if (infos != null) {
                for (SourceInfo info : infos) {
                    if (!smellsBad(info) && !(info instanceof ClassInfo) && filter.includes(info)) {

                        Set<CoverageEdge> infoSet = endingsByLine[info.getEndLine()];
                        if (infoSet == null) {
                            endingsByLine[info.getEndLine()] = infoSet = newHashSet();
                        }

                        CoverageBeginning beginning = new CoverageBeginning(info);
                        CoverageEnd ending = new CoverageEnd(beginning);

                        beginnings.add(beginning);
                        infoSet.add(ending);
                    } else if (shouldLogErrors && smellsBad(info)) {
                        String startLine;
                        try {
                            IRegion startLineRegion = document.getLineInformation(info.getStartLine());
                            startLine = document.get(startLineRegion.getOffset(), startLineRegion.getLength());
                        } catch (BadLocationException e) {
                            startLine = "*unavailable*";
                        }
                        String endLine;
                        try {
                            IRegion endLineRegion = document.getLineInformation(info.getEndLine());
                            endLine = document.get(endLineRegion.getOffset(), endLineRegion.getLength());
                        } catch (BadLocationException e) {
                            endLine = "*unavailable*";
                        }
                        CloverPlugin.logError(
                                "Bad coverage info dimension: ["
                                        + info.getStartLine() + "," + info.getStartColumn() + "]=>["
                                        + info.getEndLine() + "," + info.getEndColumn() + "] text on start line = \n"
                                        + startLine + "\ntext on end line = \n" + endLine);
                    }
                }
            }
        }
    }

    private boolean smellsBad(SourceInfo info) {
        return
            info.getStartLine() > info.getEndLine()
                || (info.getStartLine() == info.getEndLine() && info.getStartColumn() > info.getEndColumn());
    }

    private AnnotationModelEvent turnAnnotationsOff() {
        try {
            return recordAnnotationsAsRemoved(new AnnotationModelEvent(this, true));
        } finally {
            annotations = ANNOTATIONS_OFF;
        }
    }

    private AnnotationModelEvent clearAnnotations() {
        try {
            return recordAnnotationsAsRemoved(new AnnotationModelEvent(this, true));
        } finally {
            annotations = Collections.emptyList();
        }
    }

    private AnnotationModelEvent recordAnnotationsAsRemoved(AnnotationModelEvent event) {
        for (CoverageAnnotation annotation : annotations) {
            event.annotationRemoved(annotation, annotation.getPosition());
        }
        return event;
    }

    private void fireAnnotationsChanged(final AnnotationModelEvent event) {
        if (!event.isEmpty()) {
            event.markSealed();

            final List<IAnnotationModelListener> listeners;
            synchronized (CoverageAnnotationModel.this.listeners) {
                listeners = newLinkedList(this.listeners);
            }

            Display.getDefault().asyncExec(() -> {
                for (IAnnotationModelListener listener : listeners) {
                    fireAnnotationsChanged(listener, event);
                }
            });
        }
    }

    private void fireAnnotationsChanged(IAnnotationModelListener listener, AnnotationModelEvent event) {
        if (listener instanceof IAnnotationModelListenerExtension) {
            ((IAnnotationModelListenerExtension) listener).modelChanged(event);
        } else {
            listener.modelChanged(this);
        }
    }

    @Override
    public void addAnnotationModelListener(IAnnotationModelListener listener) {
        synchronized (listeners) {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
                fireAnnotationsChanged(listener, new AnnotationModelEvent(this, true));
            }
        }
    }

    @Override
    public void removeAnnotationModelListener(IAnnotationModelListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void addAnnotation(Annotation annotation, Position position) {
        throw new UnsupportedOperationException("External addition of Clover annotations is not supported");
    }

    @Override
    public void removeAnnotation(Annotation annotation) {
        throw new UnsupportedOperationException("External removal of Clover annotations is not supported");
    }

    @Override
    public Iterator<Annotation> getAnnotationIterator() {
        // since Eclipse 4.6, the getAnnotationIterator returns Iterator<Annotation> instead of Iterator
        // so we convert Iterator<CoverageAnnotation> to Iterator<Annotation>
        return new TransformingIterator<>(annotations.iterator(), coverageAnnotation -> coverageAnnotation);
    }

    @Override
    public Position getPosition(Annotation annotation) {
        return (annotation instanceof CoverageAnnotation)
            ? ((CoverageAnnotation) annotation).getPosition()
            : null;
    }

    public static void applyTo(final ITextEditor editor, EditorCoverageSynchronizer synchronizer) {
        final IAnnotationModelExtension compositeModel = getAnnotationModel(editor);
        if (compositeModel != null) {
            final CoverageAnnotationModel[] model = new CoverageAnnotationModel[]{getModel(compositeModel)};
            if (model[0] == null) {
                model[0] = new CoverageAnnotationModel(editor, getEditorDocument(editor), synchronizer);
                new AnnotationCalculationJob(model[0].updateRule) {
                    @Override
                    protected IStatus run(IProgressMonitor monitor) {
                        try {
                            compositeModel.addAnnotationModel(MODEL_KEY, model[0]);
                        } catch (Throwable t) {
                            CloverPlugin.logError("Unable to apply annotations to source file", t);
                        }
                        //Return OK status so as to not interrupt users with error message boxes
                        return Status.OK_STATUS;
                    }
                }.schedule();
            }
        }
    }

    public static void asyncRemoveFrom(ITextEditor editor) {
        final IAnnotationModelExtension compositeModel = getAnnotationModel(editor);
        if (compositeModel != null) {
            final IAnnotationModel model = compositeModel.getAnnotationModel(MODEL_KEY);
            if (model != null && model instanceof CoverageAnnotationModel) {
                new AnnotationCalculationJob(((CoverageAnnotationModel)model).updateRule) {
                    @Override
                    protected IStatus run(IProgressMonitor monitor) {
                        try {
                            try {
                                ((CoverageAnnotationModel)model).refreshSync();
                            } finally {
                                compositeModel.removeAnnotationModel(MODEL_KEY);
                            }
                        } catch (Throwable t) {
                            CloverPlugin.logError("Unable to remove annotations from source file", t);
                        }
                        //Return OK status so as to not interrupt users with error message boxes
                        return Status.OK_STATUS;
                    }
                }.schedule();
            }
        }
    }

    public static void removeFrom(ITextEditor editor) {
        final IAnnotationModelExtension compositeModel = getAnnotationModel(editor);
        if (compositeModel != null) {
            final IAnnotationModel model = compositeModel.getAnnotationModel(MODEL_KEY);
            if (model != null && model instanceof CoverageAnnotationModel) {
//                new AnnotationCalculationJob(((CoverageAnnotationModel)model).updateRule) {
//                    protected IStatus run(IProgressMonitor monitor) {
                        try {
                            try {
                                ((CoverageAnnotationModel)model).refreshSync();
                            } finally {
                                compositeModel.removeAnnotationModel(MODEL_KEY);
                            }
                        } catch (Throwable t) {
                            CloverPlugin.logError("Unable to remove annotations from source file", t);
                        }
                        //Return OK status so as to not interrupt users with error message boxes
//                        return Status.OK_STATUS;
//                    }
//                }.schedule();
            }
        }
    }

    public static void removeFrom(ITextEditor editor, boolean async) {
        if (async) {
            asyncRemoveFrom(editor);
        } else {
            removeFrom(editor);
        }
    }
    
    public static CoverageAnnotationModel getModel(IAnnotationModelExtension model) {
        return (CoverageAnnotationModel) model.getAnnotationModel(MODEL_KEY);
    }

    public static CoverageAnnotationModel getModel(ITextEditor editor) {
        IAnnotationModelExtension model = getAnnotationModel(editor);
        return model == null ? null : getModel(model);
    }

    public static IAnnotationModelExtension getAnnotationModel(ITextEditor editor) {
        IDocumentProvider documentProvider = editor == null ? null : editor.getDocumentProvider();
        IEditorInput input = editor == null ? null : editor.getEditorInput();
        IAnnotationModel model =
            (documentProvider == null | input == null)
                ? null
                : documentProvider.getAnnotationModel(input);

        if (model instanceof IAnnotationModelExtension) {
            return (IAnnotationModelExtension) model;
        } else {
            return null;
        }
    }

    private static IDocument getEditorDocument(ITextEditor editor) {
        return editor.getDocumentProvider().getDocument(editor.getEditorInput());
    }

    @Override
    public void documentChanged(DocumentEvent documentEvent) {
        FullFileInfo info = cloverFileInfoForEditorInput();
        if (!ignoreDocumentUpdates()) {
            refreshAsync();
        }
    }

    private boolean ignoreDocumentUpdates() {
        return
            annotations == ANNOTATIONS_OFF
            && editor.isDirty();
    }

    private AnnotationModelEvent rebuildAnnotations(boolean dirty) throws BadLocationException {
        lineCoverageModel = null;
        FullFileInfo info = cloverFileInfoForEditorInput();
        if (info == null
            || dirty
            || !info.validatePhysicalFile()
            || (CloverPlugin.getInstance().isInWorkingSetMode() && !editorInWorkingSet())
            || CloverPlugin.getInstance().getInstallationSettings().getEditorCoverageStyle() == InstallationSettings.Values.SHOW_NO_COVERAGE_IN_EDITORS) {
            return turnAnnotationsOff();
        } else {
            return refreshAnnotations();
        }
    }

    private AnnotationModelEvent refreshAnnotations() throws BadLocationException {
        return createAnnotations(clearAnnotations());
    }

    private boolean editorInWorkingSet() {
        IJavaElement element = JavaUI.getEditorInputJavaElement(editor.getEditorInput());
        return CloverPlugin.getInstance().getCloverWorkingSet().includesCU(element);
    }

    @Override
    public void documentAboutToBeChanged(DocumentEvent documentEvent) {
        //No impl
    }

    @Override
    public void databaseChanged(DatabaseChangeEvent event) {
        //Only refresh if coverage change resulted from a successful load,
        //unload or close - reducing spurious refreshes for intermediate
        //state changes. Also refresh for a workspace event
        if ((event.isApplicableTo(cloverProjectForEditorInput()) && event.isSubstantiveProjectChange()) || event.isForWorkspace()) {
            refreshAsync();
        }
    }

    @Override
    public void displayOptionChanged() {
        refreshAsync();
    }

    public CoverageAnnotationFilter.TestFilter getExcludedTests() {
        return CoverageAnnotationFilter.TestFilter.loadFor(
            (IResource) editor.getEditorInput().getAdapter(IResource.class),
            CoverageAnnotationFilter.EXCLUDED_TEST_NAMES);
    }

    public void setExcludedTests(CoverageAnnotationFilter.TestFilter testNames) {

        if (testNames.isEmpty()) {
            CoverageAnnotationFilter.TestFilter.removeFor(
                ((IResource) editor.getEditorInput().getAdapter(IResource.class)),
                CoverageAnnotationFilter.EXCLUDED_TEST_NAMES);

        } else {
            testNames.saveFor(
                (IResource) editor.getEditorInput().getAdapter(IResource.class),
                CoverageAnnotationFilter.EXCLUDED_TEST_NAMES);

        }
        refreshAsync();
    }

    @Override
    public boolean contains(ISchedulingRule schedulingRule) {
        return false;
    }

    @Override
    public boolean isConflicting(ISchedulingRule schedulingRule) {
        return this == schedulingRule;
    }
    
    IDocument getDocument() {
        return document;
    }
    
    public ILineCoverageModel getLineCoverageModel() {
        return lineCoverageModel;
    }
}
