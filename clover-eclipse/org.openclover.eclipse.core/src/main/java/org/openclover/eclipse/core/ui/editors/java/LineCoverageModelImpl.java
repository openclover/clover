package org.openclover.eclipse.core.ui.editors.java;

import org.openclover.core.CloverDatabase;
import org.openclover.core.api.registry.BranchInfo;
import org.openclover.core.api.registry.ElementInfo;
import org.openclover.core.api.registry.MethodInfo;
import org.openclover.core.api.registry.StatementInfo;
import org.openclover.core.registry.entities.FullElementInfo;
import org.openclover.core.registry.entities.FullFileInfo;
import org.openclover.core.registry.entities.FullMethodInfo;
import org.openclover.core.registry.entities.FullStatementInfo;
import org.openclover.core.registry.entities.LineInfo;
import org.openclover.core.registry.entities.TestCaseInfo;

import java.util.Arrays;
import java.util.Set;

public class LineCoverageModelImpl implements ILineCoverageModel {

    private static class TestStatus {
        public boolean passed;
        public boolean failed;
    }

    private Entry[] entries;
    private boolean[] hasMisses;
    private boolean[] hasHits;
    private boolean[] hasPassedHits;
    private boolean[] hasFailedHits;
    private boolean[] isFiltered;

    public LineCoverageModelImpl(CloverDatabase cloverDatabase,
            FullFileInfo fileInfo) {
        final LineInfo[] lineInfos = fileInfo.getLineInfo(true, true);
        final TestStatus isPassedFailed = new TestStatus(); // buffer for searchForPassedAndFailed output
        entries = new Entry[lineInfos.length];
        // hasMisses/Hits/PassedHits/FailedHits accumulates to true, i.e. any element present in line
        // having given attribute will set it for the whole line
        hasMisses = new boolean[lineInfos.length];
        hasHits = new boolean[lineInfos.length];
        hasPassedHits = new boolean[lineInfos.length];
        hasFailedHits = new boolean[lineInfos.length];
        // isFiltered accumulates to false, i.e. any non-filtered element present in line
        // will set the whole line to be non-filtered
        // filtered  method  statement*   result
        //           true    true      => true
        //           true    false     => true
        //           false   true      => true
        //           false   false     => false
        // *at least one
        isFiltered = new boolean[lineInfos.length];
        Arrays.fill(isFiltered, true);

        // walk through all lines
        for (LineInfo lineInfo : lineInfos) {
            if (lineInfo == null) {
                continue;
            }

            // check if signature of any method starts at given line, if yes fetch information for first method
            final MethodInfo[] methodStarts = lineInfo.getMethodStarts();

            if (methodStarts != null && methodStarts.length > 0) {
                // take only first method as it's unusual to see multiple methods in one line
                final MethodInfo methodInfo = methodStarts[0];
                final Set<TestCaseInfo> testCaseInfos = cloverDatabase.getTestHits((FullMethodInfo) methodInfo);
                final Entry entry = new EntryImpl(methodInfo, testCaseInfos);

                // check if at least one test case has passed or failed for given method
                searchForPassedAndFailed(testCaseInfos, isPassedFailed);

                // check if the whole method is not filtered-out (e.g. by method regexp context)
                final boolean isMethodFiltered = methodInfo.isFiltered(fileInfo.getContextFilter());

                // mark all source lines belonging to given method
                int methodHitCount = methodInfo.getHitCount();
                for (int line = methodInfo.getStartLine(); line <= methodInfo.getEndLine(); line++) {
                    entries[line] = entry;
                    if (methodHitCount == 0) {
                        hasMisses[line] = true;
                    } else {
                        hasHits[line] = true;
                    }
                    if (isPassedFailed.passed) {
                        hasPassedHits[line] = true;
                    }
                    if (isPassedFailed.failed) {
                        hasFailedHits[line] = true;
                    }
                    if (!isMethodFiltered) {
                        isFiltered[line] = false;
                    }
                }
                continue;
            }

            // get all statements present in given source line
            final StatementInfo[] statements = lineInfo.getStatements();

            // calculate filter for line; if at least one statement is not filtered, then the whole line is not filtered
            boolean isLineWithStatementsFiltered = true;
            if (statements != null && statements.length > 0) {
                for (StatementInfo statementInfo : statements) {
                    // check if the whole statement is not filtered-out (e.g. by statement regexp context or by block context)
                    if ( !statementInfo.isFiltered(fileInfo.getContextFilter()) ) {
                        isLineWithStatementsFiltered = false;
                        break;
                    }
                }
            }

            if (statements != null && statements.length > 0) {
                for (StatementInfo statementInfo : statements) {
                    final Set<TestCaseInfo> testCaseInfos = cloverDatabase.getTestHits((FullStatementInfo) statementInfo);
                    final Entry entry = new EntryImpl(statementInfo, testCaseInfos);

                    // check if at least one test case has passed or failed for given statement
                    searchForPassedAndFailed(testCaseInfos, isPassedFailed);

                    for (int line = statementInfo.getStartLine(); line <= statementInfo.getEndLine(); line++) {
                        entries[line] = entry;
                        if (statementInfo.getHitCount() == 0) {
                            hasMisses[line] = true;
                        } else {
                            hasHits[line] = true;
                        }
                        if (isPassedFailed.passed) {
                            hasPassedHits[line] = true;
                        }
                        if (isPassedFailed.failed) {
                            hasFailedHits[line] = true;
                        }
                        // overwrite values in non-filtered method by values resulting from statement filter
                        if (!isFiltered[line]) {
                            isFiltered[line] = isLineWithStatementsFiltered;
                        }
                    }
                }
            }

            // get all branches present in given source line
            final BranchInfo[] branchInfos = lineInfo.getBranches();
            if (branchInfos != null && branchInfos.length > 0) {
                for (BranchInfo branchInfo : branchInfos) {
                    // note: there is no need to check pass/fail hits for test cases, as the branch is always a part of
                    // a statement, so it's already calculated in the code block above;
                    // we don't also check for hasHits as 'hasHits(statement) => hasHits(branch)'

                    // check for partial branch coverage
                    for (int line = branchInfo.getStartLine(); line <= branchInfo.getEndLine(); line++) {
                        if (branchInfo.getTrueHitCount() == 0 || branchInfo.getFalseHitCount() == 0) {
                            hasMisses[line] = true;
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean hasMissesInLine(int lineNumber) {
        final int idx = lineNumber + 1;
        return idx < hasMisses.length ? hasMisses[idx] : false;
    }

    @Override
    public boolean hasHitsInLine(int lineNumber) {
        final int idx = lineNumber + 1;
        return idx < hasHits.length ? hasHits[idx] : false;
    }

    @Override
    public boolean hasPassedHitsInLine(int lineNumber) {
        final int idx = lineNumber + 1;
        return idx < hasPassedHits.length ? hasPassedHits[idx] : false;
    }

    @Override
    public boolean hasFailedHitsInLine(int lineNumber) {
        final int idx = lineNumber + 1;
        return idx < hasFailedHits.length ? hasFailedHits[idx] : false;
    }

    @Override
    public boolean isFilteredInLine(int lineNumber) {
        final int idx = lineNumber + 1;
        return idx < isFiltered.length ? isFiltered[idx] : false;
    }

    @Override
    public Entry getForLine(int lineNumber) {
        final int idx = lineNumber + 1;
        return idx < entries.length ? entries[idx] : null;
    }

    /**
     * Return true if at least one of test cases was successful.
     * @param testCaseInfos set of test cases to be searched
     * @param out returns status in provided object
     *            out.passed==true when at least one TC passed, false otherwise
     *            out.failed==true when at least one TC failed, false otherwise
     */
    private void searchForPassedAndFailed(Set<TestCaseInfo> testCaseInfos, TestStatus out) {
        out.passed = false;
        out.failed = false;

        // avoid iterator instantiation for empty collections (speed-up)
        if (testCaseInfos.isEmpty()) {
            return;
        }

        for (TestCaseInfo testCaseInfo : testCaseInfos) {
            if (testCaseInfo.isSuccess()) {
                out.passed = true;
            } else {
                out.failed = true;
            }
        }
    }

    static class EntryImpl implements Entry {
        private final ElementInfo elementInfo;
        private final Set<TestCaseInfo> testCaseInfos;

        public EntryImpl(ElementInfo elementInfo, Set<TestCaseInfo> testCaseInfos) {
            this.elementInfo = elementInfo;
            this.testCaseInfos = testCaseInfos;
        }

        @Override
        public ElementInfo getElementInfo() {
            return elementInfo;
        }

        @Override
        public Set<TestCaseInfo> getTestCaseInfos() {
            return testCaseInfos;
        }

    }
}
