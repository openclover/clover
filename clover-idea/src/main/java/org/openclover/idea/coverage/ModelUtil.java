package org.openclover.idea.coverage;

import org.openclover.core.CloverDatabase;
import org.openclover.core.CoverageData;
import org.openclover.core.api.registry.CoverageDataProvider;
import org.openclover.core.api.registry.ProjectInfo;
import org.openclover.core.registry.entities.FullProjectInfo;
import org.openclover.idea.util.ModelScope;

public final class ModelUtil {

    private ModelUtil() {
    }

    public static ProjectInfo getModel(CloverDatabase cloverDatabase, ModelScope modelScope) {
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
