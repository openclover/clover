package org.openclover.idea.testexplorer;

import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openclover.core.BitSetCoverageProvider;
import org.openclover.core.CloverDatabase;
import org.openclover.core.CoverageData;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.api.registry.CoverageDataProvider;
import org.openclover.core.api.registry.CoverageDataReceptor;
import org.openclover.core.api.registry.ProjectInfo;
import org.openclover.core.api.registry.TestCaseInfo;
import org.openclover.core.registry.entities.FullClassInfo;
import org.openclover.core.registry.entities.FullFileInfo;
import org.openclover.core.registry.entities.FullMethodInfo;
import org.openclover.core.registry.entities.FullPackageInfo;
import org.openclover.core.registry.entities.FullProjectInfo;
import org.openclover.core.registry.entities.FullTestCaseInfo;
import org.openclover.core.registry.entities.StackTraceInfo;
import org.openclover.core.api.registry.HasMetricsFilter;
import org.openclover.idea.coverage.CoverageManager;
import org.openclover.idea.util.tasks.AbstractExpirableTaskDelegate;
import org.openclover.idea.util.tasks.ExpirableTaskProcessor;

import java.util.BitSet;

interface HasCoverageInfo {
    float getCoverage();

    float getUniqueCoverage();
}

public class DecoratedTestCaseInfo extends FullTestCaseInfo implements HasCoverageInfo {
    @NotNull
    private final TestCaseInfo decorated;
    private float coverage = -1f;
    private float uniqueCoverage = -1f;
    private Calculator calculator;

    private class Calculator extends AbstractExpirableTaskDelegate {
        private final CoverageDataReceptor receptor;
        private final CloverDatabase cloverDatabase;
        private final CoverageManager coverageManager;
        private Runnable asyncUpdate;

        public Calculator(CoverageDataReceptor receptor, CloverDatabase cloverDatabase, CoverageManager coverageManager) {
            super("Calculating test unique coverage");
            this.receptor = receptor;
            this.cloverDatabase = cloverDatabase;
            this.coverageManager = coverageManager;
        }

        @Override
        public void run(@NotNull ProgressIndicator progressIndicator) {
            final CoverageDataReceptor receptorCopy = copyReceptor(receptor);

            final CoverageData coverageData = cloverDatabase.getCoverageData();
            final BitSet testCoverage = coverageData.getHitsFor(decorated);
            final CoverageDataProvider dataProvider = new BitSetCoverageProvider(testCoverage, coverageData);

            receptorCopy.setDataProvider(dataProvider);
            coverage = ((HasMetrics) receptorCopy).getMetrics().getPcCoveredElements();

            final CoverageData data = cloverDatabase.getCoverageData();
            final BitSet uniqueTestCoverage = data.getUniqueHitsFor(decorated);
            final CoverageDataProvider uniqueDataProvider = new BitSetCoverageProvider(uniqueTestCoverage, data);

            receptorCopy.setDataProvider(uniqueDataProvider);

            uniqueCoverage = ((HasMetrics) receptorCopy).getMetrics().getPcCoveredElements();
        }

        @Override
        public void onSuccess() {
            if (asyncUpdate != null) {
                asyncUpdate.run();
            }
            calculator = null;
        }

        @Override
        public void onCancel() {
            calculator = null;
        }

        @Override
        public boolean shouldProceed() {
            return coverage == -1f && cloverDatabase == coverageManager.getCoverage();
        }
    }


    public DecoratedTestCaseInfo(@NotNull TestCaseInfo decorated, CoverageDataReceptor receptor,
                                 CloverDatabase currentDatabase, CoverageManager coverageManager) {
        super(null, null, null, null);
        this.decorated = decorated;
        calculator = new Calculator(receptor, currentDatabase, coverageManager);
    }

    private void calcValues() {
        if (calculator != null) {
            ExpirableTaskProcessor.getInstance().queue(calculator);
        }
    }

    private static CoverageDataReceptor copyReceptor(CoverageDataReceptor receptor) {
            if (receptor instanceof FullProjectInfo) {
                return ((FullProjectInfo) receptor).copy();
            } else if (receptor instanceof FullFileInfo) {
                final FullFileInfo fileInfo = (FullFileInfo) receptor;
                return fileInfo.copy((FullPackageInfo) fileInfo.getContainingPackage(), HasMetricsFilter.ACCEPT_ALL);
            } else if (receptor instanceof FullClassInfo) {
                final FullClassInfo classInfo = (FullClassInfo) receptor;
                return classInfo.copy((FullFileInfo) classInfo.getContainingFile(), HasMetricsFilter.ACCEPT_ALL);
            } else if (receptor instanceof FullMethodInfo) {
                final FullMethodInfo methodInfo = (FullMethodInfo) receptor;
                if (methodInfo.getContainingClass() != null) {
                    return methodInfo.copy((FullClassInfo) methodInfo.getContainingClass());
                } else if (methodInfo.getContainingMethod() != null) {
                    return methodInfo.copy((FullMethodInfo) methodInfo.getContainingMethod());
                } else {
                    return methodInfo.copy((FullFileInfo) methodInfo.getContainingFile());
                }
            } else {
                return null;
            }
    }


    public void setAsyncUpdate(Runnable asyncUpdate) {
        if (calculator != null) {
            calculator.asyncUpdate = asyncUpdate;
        }
    }

    @NotNull
    public TestCaseInfo getNakedTestCaseInfo() {
        return decorated;
    }

    @Override
    public float getCoverage() {
        if (coverage == -1f) {
            calcValues();
        }
        return coverage;
    }

    @Override
    public float getUniqueCoverage() {
        if (uniqueCoverage == -1f) {
            calcValues();
        }
        return uniqueCoverage;
    }

    public void setCoverage(float coverage) {
        this.coverage = coverage;
    }

    public void setUniqueCoverage(float uniqueCoverage) {
        this.uniqueCoverage = uniqueCoverage;
    }

    @Override
    public Integer getId() {
        return decorated.getId();
    }

    @Override
    public boolean isResolved() {
        return decorated.isResolved();
    }

    @Override
    public String getKey() {
        return decorated.getKey();
    }

    @Override
    public String getTestName() {
        return decorated.getTestName();
    }

    @Override
    public String getClassName() {
        return decorated.getClassName();
    }

    @Override
    public double getDuration() {
        return decorated.getDuration();
    }

    @Override
    public boolean isError() {
        return decorated.isError();
    }

    @Override
    public boolean isHasResult() {
        return decorated.isHasResult();
    }

    @Override
    public boolean isFailure() {
        return decorated.isFailure();
    }

    @Override
    public boolean isSuccess() {
        return decorated.isSuccess();
    }

    @Override
    public String getFailMessage() {
        return decorated.getFailMessage();
    }

    @Override
    public String getFailType() {
        return decorated.getFailType();
    }

    @Override
    public String getFailFullMessage() {
        return decorated.getFailFullMessage();
    }

    @Nullable
    @Override
    public StackTraceInfo getStackTrace() {
        return decorated.getStackTrace();
    }

    @Nullable
    @Override
    public FullClassInfo getRuntimeType() {
        return decorated.getRuntimeType();
    }

    @Nullable
    @Override
    public FullMethodInfo getSourceMethod() {
        return decorated.getSourceMethod();
    }

    @Nullable
    @Override
    public String getQualifiedName() {
        return decorated.getQualifiedName();
    }

    @Nullable
    @Override
    public String getRuntimeTypeName() {
        return decorated.getRuntimeTypeName();
    }

    @Nullable
    @Override
    public String getSourceMethodName() {
        return decorated.getSourceMethodName();
    }

    @Override
    public long getStartTime() {
        return decorated.getStartTime();
    }

    @Override
    public long getEndTime() {
        return decorated.getEndTime();
    }

    // Avoid nasty surprises:
    @Override
    public void setId(Integer id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean resolve(ProjectInfo project) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setHasResult(boolean hasResult) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDuration(double duration) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setError(boolean error) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFailure(boolean failure) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFailMessage(String failMessage) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFailType(String failType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFailFullMessage(String failFullMessage) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setStartTime(long startTime) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setEndTime(long endTime) {
        throw new UnsupportedOperationException();
    }
}
