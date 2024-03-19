package org.openclover.eclipse.core.views.testcontributions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.openclover.core.CloverDatabase;
import org.openclover.core.api.registry.TestCaseInfo;
import org.openclover.core.registry.entities.FullFileInfo;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.CloverProject;
import org.openclover.eclipse.core.projects.model.DatabaseModel;
import org.openclover.eclipse.core.projects.model.MetricsScope;
import org.openclover.eclipse.core.ui.editors.java.CoverageAnnotationFilter;
import org.openclover.eclipse.core.ui.editors.java.CoverageAnnotationModel;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.openclover.core.util.Maps.newHashMap;
import static org.openclover.core.util.Sets.newHashSet;
import static org.openclover.eclipse.core.CloverPlugin.logError;

public class AllTestContributionsComputation extends TestContributionsComputation {
    private static final QualifiedName COMPILATION_UNIT = new QualifiedName(CloverPlugin.ID, "TestContributionsCompilationUnit");
    private static final QualifiedName CHECKED_TEST_CASES_AND_CLASSES = new QualifiedName(CloverPlugin.ID, "TestContributionsCheckedTestCasesAndClasses");

    public AllTestContributionsComputation() {
        super("Syncing test contributions for open editor");
    }

    @Override
    public void prime(ITextEditor editor) {
        DatabaseModel oldDatabaseModel = getDatabaseModel();
        DatabaseModel newDatabaseModel = databaseModelForEditor(editor);

        CoverageAnnotationModel oldAnnotationModel = getCoverageAnnotationModel();
        CoverageAnnotationModel newAnnotationModel = CoverageAnnotationModel.getModel(editor);

        ICompilationUnit oldCompilationUnit = getCompilationUnit();
        ICompilationUnit newCompilationUnit = compilationUnitForEditor(editor);

        if (oldAnnotationModel != newAnnotationModel
            || oldDatabaseModel != newDatabaseModel) {
            setInputChanged(true);
            setDatabaseModel(newDatabaseModel);
            setCoverageAnnotationModel(newAnnotationModel);
            setCompilationUnit(newCompilationUnit);
        } else {
            setInputChanged(false);
//          setDatabaseModel(null);
//          setCoverageAnnotationModel(null);
//          setCompilationUnit(null);
        }
    }

    private ICompilationUnit compilationUnitForEditor(ITextEditor editor) {
        IJavaElement javaElement = editor == null ? null : JavaUI.getEditorInputJavaElement(editor.getEditorInput());
        return (javaElement instanceof ICompilationUnit) ? (ICompilationUnit) javaElement : null;
    }

    private void setCompilationUnit(ICompilationUnit compilationUnit) {
        setProperty(COMPILATION_UNIT, compilationUnit);
    }

    private ICompilationUnit getCompilationUnit() {
        return (ICompilationUnit) getProperty(COMPILATION_UNIT);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        try {
            if (isInputChanged()) {

                setTestCases(Collections.<TestCaseInfo>emptySet());
                setCheckedTestCasesAndClasses(Collections.emptySet());

                CoverageAnnotationModel annotationModel = getCoverageAnnotationModel();
                DatabaseModel databaseModel = getDatabaseModel();
                ICompilationUnit cu = getCompilationUnit();

                if (annotationModel == null) {
                    clearTestCases();
                } else if (databaseModel != null && cu != null) {
                    updateTestCases(cu);
                }
            }
        } catch (Exception e) {
            logError("Unable to calculate file-based test contributions", e);
        }

        return Status.OK_STATUS;
    }

    private void updateTestCases(ICompilationUnit cu) {
        CloverProject project = (CloverProject) cu.getAdapter(CloverProject.class);
        if (project != null) {
            CloverDatabase database = project.getModel().getDatabase();
            if (database != null) {
                FullFileInfo fileInfo = (FullFileInfo) MetricsScope.FULL.getHasMetricsFor(cu, FullFileInfo.class);

                if (fileInfo != null) {
                    Set allTests = newHashSet();
                    Set checkedTests = newHashSet();

                    CoverageAnnotationFilter.TestFilter excludedTestNames =
                        CoverageAnnotationFilter.TestFilter.loadFor(
                            cu.getResource(),
                            CoverageAnnotationFilter.EXCLUDED_TEST_NAMES);

                    Set<TestCaseInfo> testHits = database.getTestHits(fileInfo);
                    for (Iterator<TestCaseInfo> iterator = testHits.iterator(); iterator.hasNext();) {
                        TestCaseInfo testCase = iterator.next();
                        if (testCase.isResolved() || testCase.resolve(MetricsScope.FULL.getProjectInfoFor(database))) {
                            allTests.add(testCase);
                            if (!excludedTestNames.matches(testCase)) {
                                checkedTests.add(testCase);
                            }
                        } else {
                            iterator.remove();
                        }
                    }

                    setTestCases(allTests);
                    setCheckedTestCasesAndClasses(
                        computeCheckedTestCasesAndClasses(allTests, checkedTests));
                }
            }
        }
    }

    private void clearTestCases() {
        setTestCases(Collections.<TestCaseInfo>emptySet());
        setCheckedTestCasesAndClasses(Collections.singleton(AllTestCaseInfoProvider.ALL_TEST_CASES));
    }

    public static Set computeCheckedTestCasesAndClasses(Set<TestCaseInfo> allTestCases, Set<TestCaseInfo> checkedTestCases) {
        Set checkedTestCasesAndClasses = newHashSet(checkedTestCases);

        //Map all classes to their tests
        Map allTestClassToTestCases = newHashMap();
        for (TestCaseInfo testCase : allTestCases) {
            Set testCasesForClass = (Set) allTestClassToTestCases.get(testCase.getRuntimeType());
            if (testCasesForClass == null) {
                testCasesForClass = newHashSet();
                allTestClassToTestCases.put(testCase.getRuntimeType(), testCasesForClass);
            }
            testCasesForClass.add(testCase);
        }

        //Map all checked classes to their tests
        Map checkedTestClassToTestCases = new HashMap();
        for (TestCaseInfo testCase : checkedTestCases) {
            Set testCasesForClass = (Set) checkedTestClassToTestCases.get(testCase.getRuntimeType());
            if (testCasesForClass == null) {
                testCasesForClass = newHashSet();
                checkedTestClassToTestCases.put(testCase.getRuntimeType(), testCasesForClass);
            }
            testCasesForClass.add(testCase);
        }

        //Check and classes with less than 100% of their tests checked
        for (Object o : checkedTestClassToTestCases.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            if (((Collection) entry.getValue()).size()
                    == ((Collection) allTestClassToTestCases.get(entry.getKey())).size()) {
                checkedTestCasesAndClasses.add(entry.getKey());
            }
        }

        if (allTestCases.size() == checkedTestCases.size()) {
            checkedTestCasesAndClasses.add(AllTestCaseInfoProvider.ALL_TEST_CASES);
        }

        return checkedTestCasesAndClasses;
    }

    private void setCheckedTestCasesAndClasses(Set tests) {
        setProperty(CHECKED_TEST_CASES_AND_CLASSES, tests);
    }

    public Set getCheckedTestCasesAndClasses() {
        return (Set) getProperty(CHECKED_TEST_CASES_AND_CLASSES);
    }
}
