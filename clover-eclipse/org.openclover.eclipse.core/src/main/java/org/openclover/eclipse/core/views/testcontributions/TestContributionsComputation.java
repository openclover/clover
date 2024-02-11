package org.openclover.eclipse.core.views.testcontributions;

import org.openclover.core.registry.entities.TestCaseInfo;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.ui.JavaUI;

import java.util.Set;

import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.ui.editors.java.CoverageAnnotationModel;
import org.openclover.eclipse.core.projects.model.DatabaseModel;
import org.openclover.eclipse.core.projects.CloverProject;

public abstract class TestContributionsComputation extends Job {
    private static final QualifiedName CONTRIBUTIONS =
        new QualifiedName(CloverPlugin.ID, "TestContributionsAtCursor");
    protected static final QualifiedName DATABASE_MODEL =
        new QualifiedName(CloverPlugin.ID, "TestContributionsDatabaseModel");
    protected static final QualifiedName COVERAGE_ANNOTATION_MODEL =
        new QualifiedName(CloverPlugin.ID, "TestContributionsCoverageAnnotationModel");
    protected static final QualifiedName INPUT_CHANGED =
        new QualifiedName(CloverPlugin.ID, "TestContributionsInputChanged");

    public TestContributionsComputation(String name) {
        super(name);
        setPriority(Job.DECORATE);
        setUser(false);
        setSystem(true);
        setProperty(INPUT_CHANGED, Boolean.TRUE);
    }

    public Set<TestCaseInfo> getTestCases() {
        return (Set<TestCaseInfo>) getProperty(CONTRIBUTIONS);
    }

    protected void setTestCases(Set<TestCaseInfo> testCases) {
        setProperty(CONTRIBUTIONS, testCases);
    }

    protected DatabaseModel getDatabaseModel() {
        return (DatabaseModel)getProperty(DATABASE_MODEL);
    }

    protected CoverageAnnotationModel getCoverageAnnotationModel() {
        return (CoverageAnnotationModel)getProperty(COVERAGE_ANNOTATION_MODEL);
    }

    protected DatabaseModel databaseModelForEditor(ITextEditor editor) {
        IJavaElement element =
            editor == null
                ? null
                : JavaUI.getEditorInputJavaElement(editor.getEditorInput());

        if (CloverPlugin.getInstance().isInWorkingSetMode()
            && !CloverPlugin.getInstance().getCloverWorkingSet().includesCU(element)) {
        
            return null;
        } else {
            CloverProject cloverProject =
                element != null
                    ? ((CloverProject) element.getAdapter(CloverProject.class))
                    : null;

            return cloverProject == null ? null : cloverProject.getModel();
        }
    }

    public boolean isInputChanged() {
        return (Boolean) getProperty(INPUT_CHANGED);
    }

    protected void setInputChanged(boolean changed) {
        setProperty(INPUT_CHANGED, changed);
    }

    protected void setCoverageAnnotationModel(CoverageAnnotationModel newCoverageAnnotationModel) {
        setProperty(COVERAGE_ANNOTATION_MODEL, newCoverageAnnotationModel);
    }

    protected void setDatabaseModel(DatabaseModel newDatabaseModel) {
        setProperty(DATABASE_MODEL, newDatabaseModel);
    }

    public abstract void prime(ITextEditor editor);
}
