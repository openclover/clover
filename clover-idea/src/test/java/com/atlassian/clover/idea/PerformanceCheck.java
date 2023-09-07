package com.atlassian.clover.idea;

import com.atlassian.clover.CloverDatabase;
import com.atlassian.clover.recorder.PerTestCoverage;
import com.atlassian.clover.ProgressListener;
import com.atlassian.clover.registry.entities.FullProjectInfo;
import com.atlassian.clover.idea.util.ModelScope;
import com.atlassian.clover.idea.coverage.CoverageTreeModel;
import com.atlassian.clover.idea.testexplorer.DecoratedTestCaseInfo;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;

import java.text.DecimalFormat;
import java.util.BitSet;
import java.util.Collection;

@SuppressWarnings({"UseOfSystemOutOrSystemErr"})
public class PerformanceCheck {
    private PerformanceCheck() {
    }

    public static void main(String[] args) {
        String fBase = args.length > 0 ? args[0] : "coverage.db";

        final ProgressListenerImpl progress = new ProgressListenerImpl();

        final CoverageTreeModel newModel = new CoverageTreeModel("Project", fBase,
                                                                 0, "", true,
                                                                 null, ModelScope.ALL_CLASSES, true, null, null, null);
        newModel.load(progress);
        CloverDatabase currentDatabase = newModel.getCloverDatabase();
        if (currentDatabase == null) {
            System.out.println("Could not load the database");
            System.exit(0);
        }
        FullProjectInfo projectInfo = currentDatabase.getFullModel();
//        @SuppressWarnings("unchecked")
//        Collection<DecoratedTestCaseInfo> testCases = TestRunExplorerTreeBuilder.wrap(currentDatabase.getCoverageData().getTests());
//
//        //checkSparseness(currentDatabase, testCases);
//
//        TestRunExplorerTreeBuilder builder = new TestRunExplorerTreeBuilder(null, new CoverageTreeTableModel(), new DefaultMutableTreeNode());
//        TestRunExplorerTreeBuilder.BackgroundCoverageCalculator calculator = builder.new BackgroundCoverageCalculator(testCases, projectInfo, currentDatabase);
//        long ts1 = System.currentTimeMillis();
//        calculator.run(new ProgressIndicatorImpl());
//        long ts2 = System.currentTimeMillis();
//
//        System.out.println(String.format("%,10d\t%s", ts2 - ts1, "Calculating unique coverage"));

    }

    private static void checkSparseness(CloverDatabase currentDatabase, Collection<DecoratedTestCaseInfo> testCases) {
        final PerTestCoverage perTestCoverage = currentDatabase.getCoverageData();
        for (DecoratedTestCaseInfo testCase : testCases) {
            BitSet bs = perTestCoverage.getHitsFor(testCase.getNakedTestCaseInfo());

            int empty = 0;
            int nonempty = 0;
            final int chunk = 4096;
            int start = 0;
            int firstIdx = -1;
            final int bsLength = bs.length();
            while (start < bsLength) {
                final int nextSet = bs.nextSetBit(start);
                if (firstIdx == -1) {
                    firstIdx = nextSet;
                }
                if (nextSet == -1) {
                    break;
                }
                int fulls = nextSet / chunk;
                empty += fulls;
                nonempty++;

                start += (fulls + 1) * chunk;
            }
            System.out.println("TCI " + firstIdx + "/" + bsLength + " (" + (100 * firstIdx / (bsLength > 0 ? bsLength : -1)) + ")\t" + nonempty + "/" + (empty + nonempty) + "\t" + testCase.getTestName());
        }
    }

    static class ProgressListenerImpl implements ProgressListener {
        private String lastMsg;
        private long startTime;

        DecimalFormat timeFormat = new DecimalFormat("# ###");

        @Override
        public void handleProgress(String desc, float pc) {
            //noinspection StringEquality
            if (desc != lastMsg) {
                final long currentTime = System.currentTimeMillis();
                if (lastMsg != null) {
                    System.out.println(String.format("%,10d\t%s", currentTime - startTime, lastMsg));
                }
                lastMsg = desc;
                startTime = currentTime;
            }
        }
    }

    static class ProgressIndicatorImpl implements ProgressIndicator {
        private long startTime = System.currentTimeMillis();
        private String message = "";
        private static final double DELTA = 0.001;


        @Override
        public void setText2(String text) {
            message = text;
            startTime = System.currentTimeMillis();
        }

        @Override
        public void setFraction(double fraction) {
            final long duration = System.currentTimeMillis() - startTime;
            final long estimate = fraction > DELTA ? (long) (duration / fraction) : 0;
            System.out.println(String.format("%,10d\t%s %5.2f%%, ETA: %,10d",
                                             duration,
                                             message,
                                             fraction * 100,
                                             estimate));
        }

        @Override
        public void start() {

        }

        @Override
        public void stop() {
        }

        @Override
        public boolean isRunning() {
            return false;
        }

        @Override
        public void cancel() {
        }

        @Override
        public boolean isCanceled() {
            return false;
        }

        @Override
        public void setText(String text) {
        }

        @Override
        public String getText() {
            return "";
        }

        @Override
        public String getText2() {
            return "";
        }

        @Override
        public double getFraction() {
            return 0;
        }

        @Override
        public void pushState() {
        }

        @Override
        public void popState() {
        }

        @Override
        public void startNonCancelableSection() {
        }

        @Override
        public void finishNonCancelableSection() {
        }

        @Override
        public boolean isModal() {
            return false;
        }

        @Override
        public ModalityState getModalityState() {
            //TODO: implement method getModalityState
            throw new UnsupportedOperationException("method getModalityState not implemented");
        }

        @Override
        public void setModalityProgress(ProgressIndicator modalityProgress) {
        }

        @Override
        public boolean isIndeterminate() {
            return true;
        }

        @Override
        public void setIndeterminate(boolean indeterminate) {
        }

        @Override
        public void checkCanceled() throws ProcessCanceledException {
        }

        //@Override commented out as method is not present before 10.5 version
        public boolean isPopupWasShown() {
            return false;
        }

        //@Override commented out as method is not present before 10.5 version
        public boolean isShowing() {
            return false;
        }
    }
}
