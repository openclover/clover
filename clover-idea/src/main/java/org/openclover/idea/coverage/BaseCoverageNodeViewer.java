package org.openclover.idea.coverage;

import com.atlassian.clover.api.registry.BlockMetrics;
import com.atlassian.clover.api.registry.HasMetrics;
import com.atlassian.clover.registry.metrics.ClassMetrics;
import com.atlassian.clover.registry.metrics.FileMetrics;
import com.atlassian.clover.registry.metrics.PackageMetrics;
import com.atlassian.clover.registry.metrics.ProjectMetrics;
import com.atlassian.clover.registry.entities.TestCaseInfo;
import com.atlassian.clover.util.Formatting;

import java.util.Collection;

public abstract class BaseCoverageNodeViewer {

    public static class TestPassInfo {
        private int testPasses;
        private int testFailures;
        private int testErrors;

        private final int testsRun;

        public TestPassInfo(Collection<TestCaseInfo> testRuns) {
            for (TestCaseInfo hit : testRuns) {
                if (hit.isSuccess()) {
                    ++testPasses;
                } else if (hit.isFailure()) {
                    ++testFailures;
                } else if (hit.isError()) {
                    ++testErrors;
                }
            }

            testsRun = testPasses + testFailures + testErrors;
        }

        public TestPassInfo(BlockMetrics metrics) {
            testPasses = metrics.getNumTestPasses();
            testFailures = metrics.getNumTestFailures();
            testErrors = metrics.getNumTestErrors();
            testsRun = metrics.getNumTestsRun();
        }

        public int getTestFailures() {
            return testFailures;
        }

        public int getTestPasses() {
            return testPasses;
        }

        public int getTestErrors() {
            return testErrors;
        }

        public int getTestsRun() {
            return testsRun;
        }

        public boolean hasResults() {
            return testsRun != 0;
        }

        public float getPcTestPasses() {
            return getFraction(testPasses, testsRun);
        }

        public float getPcTestErrors() {
            return getFraction(testErrors, testsRun);
        }

        public float getPcTestFailures() {
            return getFraction(testFailures, testsRun);
        }

        private float getFraction(int num, int dem) {
            if (dem != 0) {
                return (float) num / dem;
            } else {
                return -1;
            }
        }


    }


    public void setNode(HasMetrics hasMetrics, TestPassInfo testPassInfo) {
        BlockMetrics metrics = hasMetrics != null ? hasMetrics.getMetrics() : null;

        if (testPassInfo != null) {
            if (testPassInfo.hasResults()) {
                setTestsPassedText(Formatting.formatInt(testPassInfo.getTestPasses()));
                setTestsRunText(Formatting.formatInt(testPassInfo.getTestsRun()));
            } else {
                setTestsPassedText("-");
                setTestsRunText("-");
            }
            setTestsFailedText(Formatting.formatInt(testPassInfo.getTestFailures()));
            setTestsErrorsText(Formatting.formatInt(testPassInfo.getTestErrors()));

            setPCTestsError(testPassInfo.getPcTestErrors());
            setPCTestsFailed(testPassInfo.getPcTestFailures());
            setPCTestsPassed(testPassInfo.getPcTestPasses());
        } else {
            setTestsPassedText("-");
            setTestsRunText("-");

            setTestsText(Formatting.formatInt(0));
            setTestsFailedText(Formatting.formatInt(0));
            setTestsErrorsText(Formatting.formatInt(0));

            setPCTestsError(-1);
            setPCTestsFailed(-1);
            setPCTestsPassed(-1);
        }

        if (metrics != null) {
            setStmtsCoveredText(Formatting.formatInt(metrics.getNumCoveredStatements()) + " / "
                    + Formatting.formatInt(metrics.getNumStatements()));
            setCondsCoveredText(Formatting.formatInt(metrics.getNumCoveredBranches()) + " / "
                    + Formatting.formatInt(metrics.getNumBranches()));
            setTotalComplexityText(Formatting.formatInt(metrics.getComplexity()));
            setComplexityDensityText(Formatting.format2d(metrics.getComplexityDensity()));
            setStmtsPercent(metrics.getPcCoveredStatements());
            setCondsPercent(metrics.getPcCoveredBranches());
            setTotalPercent(metrics.getPcCoveredElements());
            setNumStatementsText(Formatting.formatInt(metrics.getNumStatements()));
            setNumBranchesText(Formatting.formatInt(metrics.getNumBranches()));

            setTestsText(Formatting.formatInt(metrics.getNumTests()));

            if (metrics instanceof ClassMetrics) {
                final ClassMetrics classMetrics = (ClassMetrics) metrics;
                setMethodsCoveredText(Formatting.formatInt(classMetrics.getNumCoveredMethods()) + " / "
                        + Formatting.formatInt(classMetrics.getNumMethods()));
                setMethodsPercent(classMetrics.getPcCoveredMethods());
                setNumMethodsText(Formatting.formatInt(classMetrics.getNumMethods()));
                setAvgComplexityText(Formatting.format2d(classMetrics.getAvgMethodComplexity()));

                setTestMethodsText(Formatting.formatInt(classMetrics.getNumTestMethods()));
            } else {
                setMethodsCoveredText("- / -");
                setMethodsPercent(-1);
                setNumMethodsText("-");
                setAvgComplexityText("-");
            }

            if (metrics instanceof FileMetrics) {
                final FileMetrics fileMetrics = (FileMetrics) metrics;
                setLOCText(Formatting.formatInt(fileMetrics.getLineCount()));
                setNCLOCText(Formatting.formatInt(fileMetrics.getNcLineCount()));
                setNumClassesText(Formatting.formatInt(fileMetrics.getNumClasses()));
            } else {
                setLOCText("-");
                setNCLOCText("-");
                setNumClassesText("-");
            }

            if (metrics instanceof ProjectMetrics) {
                setNumPackagesText(Formatting.formatInt(((ProjectMetrics) metrics).getNumPackages()));
            } else {
                setNumPackagesText("-");
            }

            if (metrics instanceof PackageMetrics) {
                setNumFilesText(Formatting.formatInt(((PackageMetrics) metrics).getNumFiles()));
            } else {
                setNumFilesText("-");
            }

            setExtraStatsVisible(true);
        } else {
            final String nothing = "- / -";
            setMethodsCoveredText(nothing);
            setStmtsCoveredText(nothing);
            setCondsCoveredText(nothing);
            setMethodsPercent(-1);
            setStmtsPercent(-1);
            setCondsPercent(-1);
            setTotalPercent(-1);
            setExtraStatsVisible(false);
        }
    }


    /**
     * whether LOC/NLOC/#files/#methods, etc, should be shown
     *
     * @param enabled enabled
     */
    protected abstract void setExtraStatsVisible(boolean enabled);


    protected abstract void setNumPackagesText(String s);


    protected abstract void setNumClassesText(String s);


    protected abstract void setNumFilesText(String s);


    protected abstract void setNumMethodsText(String i);

    protected abstract void setNumStatementsText(String s);

    protected abstract void setNumBranchesText(String s);

    protected abstract void setNCLOCText(String i);


    protected abstract void setLOCText(String i);


    protected abstract void setTotalPercent(float i);


    protected abstract void setCondsPercent(float setCondsPercent);


    protected abstract void setStmtsPercent(float i);


    protected abstract void setMethodsPercent(float i);


    protected abstract void setCondsCoveredText(String s);


    protected abstract void setStmtsCoveredText(String s);

    protected abstract void setMethodsCoveredText(String s);

    protected abstract void setTotalComplexityText(String s);

    protected abstract void setAvgComplexityText(String s);

    protected abstract void setComplexityDensityText(String s);

    protected abstract void setTestMethodsText(String s);

    protected abstract void setTestsText(String s);

    protected abstract void setTestsRunText(String s);

    protected abstract void setTestsPassedText(String s);

    protected abstract void setTestsFailedText(String s);

    protected abstract void setTestsErrorsText(String s);

    protected abstract void setPCTestsPassed(float p);

    protected abstract void setPCTestsFailed(float p);

    protected abstract void setPCTestsError(float p);

}
