package org.openclover.eclipse.core.views.testcontributions;

import org.openclover.core.registry.entities.FullElementInfo;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.ITextSelection;

import java.util.Set;
import java.util.Collections;
import java.util.Iterator;

import org.openclover.eclipse.core.ui.editors.java.CoverageAnnotation;
import org.openclover.eclipse.core.ui.editors.java.CoverageAnnotationModel;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.model.DatabaseModel;
import org.openclover.eclipse.core.projects.model.MetricsScope;
import org.openclover.core.registry.entities.TestCaseInfo;
import org.openclover.core.CloverDatabase;

public class LineTestContributionsComputation extends TestContributionsComputation {
    private static final QualifiedName CURSOR_POSITION =
        new QualifiedName(CloverPlugin.ID, "TestContributionsCursorLocation");

    public LineTestContributionsComputation() {
        super("Syncing test contributions for editor cursor");
    }

    @Override
    public void prime(ITextEditor editor) {

        DatabaseModel oldCoverageModel = getDatabaseModel();
        DatabaseModel newCoverageModel = databaseModelForEditor(editor);

        CoverageAnnotationModel oldCoverageAnnotationModel = getCoverageAnnotationModel();
        CoverageAnnotationModel newCoverageAnnotationModel = CoverageAnnotationModel.getModel(editor);

        Position oldPosition = getCursorPosition();
        Position newPosition = cursorPositionOfEditor(editor);

        if (oldCoverageModel != newCoverageModel
            || oldCoverageAnnotationModel != newCoverageAnnotationModel
            || !postionsEqual(oldPosition, newPosition)) {

            setDatabaseModel(newCoverageModel);
            setCoverageAnnotationModel(newCoverageAnnotationModel);
            setCursorPosition(newPosition);
            setInputChanged(true);
        } else {
//          setCoverageModel(null);
//          setCoverageAnnotationModel(null);
//          setCursorPosition(null);
            setInputChanged(false);
        }
    }

    private void setCursorPosition(Position newPosition) {
        setProperty(CURSOR_POSITION, newPosition);
    }

    private boolean postionsEqual(Position oldPosition, Position newPosition) {
        return
            !(oldPosition == null ^ newPosition == null)
                && (oldPosition == newPosition
                || oldPosition.equals(newPosition));
    }

    private Position cursorPositionOfEditor(ITextEditor editor) {
        if (editor != null && editor.getSelectionProvider() != null) {
            ISelection selection = editor.getSelectionProvider().getSelection();
            if (selection instanceof ITextSelection) {
                return new Position(
                    ((ITextSelection) selection).getOffset(),
                    ((ITextSelection) selection).getLength());
            }
        }
        return null;
    }

    private Position getCursorPosition() {
        return (Position) getProperty(CURSOR_POSITION);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {

        try {
            if (isInputChanged()) {
                CoverageAnnotationModel coverageAnnotationModel = getCoverageAnnotationModel();
                DatabaseModel coverageModel = getDatabaseModel();
                Position cursorPosition = getCursorPosition();

                //No tests, by default
                setTestCases(Collections.<TestCaseInfo>emptySet());

                if (coverageAnnotationModel != null && coverageModel != null && cursorPosition != null) {
                    updateTestCases(coverageAnnotationModel, coverageModel, cursorPosition);
                }
            }
        } catch (Exception e) {
            CloverPlugin.logError("Error while syning editor and contributing tests view", e);
        }
        return Status.OK_STATUS;
    }

    private void updateTestCases(CoverageAnnotationModel coverageAnnotationModel, DatabaseModel coverageModel, Position cursorPosition) {
        Iterator annotationIterator = coverageAnnotationModel.getAnnotationIterator();
        while (annotationIterator.hasNext()) {
            CoverageAnnotation annotation = (CoverageAnnotation) annotationIterator.next();
            if (annotation.encloses(cursorPosition.getOffset() + cursorPosition.getLength())) {
                FullElementInfo coveredElement = annotation.getInfo();
                if (coveredElement != null) {
                    CloverDatabase database = coverageModel.getDatabase();
                    //Might be null if coverage model is being loaded
                    if (database != null) {
                        Set<TestCaseInfo> testHits = database.getTestHits(coveredElement);
                        testHits.removeIf(testCase -> !testCase.isResolved() && !testCase.resolve(MetricsScope.FULL.getProjectInfoFor(coverageModel.getDatabase())));

                        setTestCases(testHits);
                    }
                }

                //We've found the annotation, whether the system was in a state to display
                //the test cases or not, we're done
                break;
            }
        }
    }

}
