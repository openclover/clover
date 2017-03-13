package com.atlassian.clover.eclipse.core.views.testcontributions;

import com.atlassian.clover.eclipse.core.projects.model.DatabaseModel;
import com.atlassian.clover.eclipse.core.ui.editors.java.CoverageAnnotationModel;

public class CoverageSelection {
    private int offset;
    private int length;
    private DatabaseModel coverageModel;
    private CoverageAnnotationModel annotationModel;

    public CoverageSelection(int offset, int length, DatabaseModel coverageModel, CoverageAnnotationModel annotationModel) {
        this.offset = offset;
        this.length = length;
        this.coverageModel = coverageModel;
        this.annotationModel = annotationModel;
    }

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    public DatabaseModel getCoverageModel() {
        return coverageModel;
    }

    public CoverageAnnotationModel getAnnotationModel() {
        return annotationModel;
    }
}
