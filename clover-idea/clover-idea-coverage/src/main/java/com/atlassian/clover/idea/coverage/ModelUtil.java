package com.atlassian.clover.idea.coverage;

import com.atlassian.clover.CloverDatabase;
import com.atlassian.clover.CoverageData;
import com.atlassian.clover.registry.entities.FullProjectInfo;
import com.atlassian.clover.idea.util.ModelScope;
import com.atlassian.clover.registry.CoverageDataProvider;

public final class ModelUtil {

    private ModelUtil() {
    }

    public static FullProjectInfo getModel(CloverDatabase cloverDatabase, ModelScope modelScope) {
        if (cloverDatabase == null) {
            return null;
        }
        switch (modelScope) {
            case APP_CLASSES_ONLY:
                return cloverDatabase.getAppOnlyModel();
            case TEST_CLASSES_ONLY:
                return cloverDatabase.getTestOnlyModel();
            case ALL_CLASSES:
                return cloverDatabase.getFullModel();
            default:
                throw new IllegalStateException(modelScope.toString());
        }
    }

    public static boolean isPassedTestsCoverageOnly(CloverDatabase cloverDatabase) {
        final CoverageData fullData = cloverDatabase.getCoverageData();
        final CoverageDataProvider currentProvider = cloverDatabase.getFullModel().getDataProvider();

        return fullData != currentProvider;
    }
}
