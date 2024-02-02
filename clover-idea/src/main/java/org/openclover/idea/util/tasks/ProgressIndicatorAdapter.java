package com.atlassian.clover.idea.util.tasks;

import com.atlassian.clover.ProgressListener;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;

public class ProgressIndicatorAdapter implements ProgressListener {
    private static final Logger LOG = Logger.getInstance("CloverProgressIndicator");

    private final ProgressIndicator progressIndicator;
    private String lastDesc;
    private float lastReportedPc;
    private final float step = 0.01f;

    public ProgressIndicatorAdapter(ProgressIndicator progressIndicator) {
        this.progressIndicator = progressIndicator;
        progressIndicator.setIndeterminate(true);
    }

    @Override
    public void handleProgress(String desc, float pc) {
        progressIndicator.checkCanceled();
        if (!desc.equals(lastDesc)) {
            // don't be THAT verbose
            LOG.info(desc);
            progressIndicator.setText2(desc);
            lastDesc = desc;
            lastReportedPc = -1f;
        }

        if (pc <= 0) {
            if (!progressIndicator.isIndeterminate()) {
                progressIndicator.setIndeterminate(true);
            }
        } else {
            if (progressIndicator.isIndeterminate()) {
                progressIndicator.setIndeterminate(false);
                lastReportedPc = pc;
                progressIndicator.setFraction(pc);
            } else if (pc < lastReportedPc || pc - lastReportedPc > step) {
                lastReportedPc = pc;
                progressIndicator.setFraction(pc);
            }

        }
    }
}
