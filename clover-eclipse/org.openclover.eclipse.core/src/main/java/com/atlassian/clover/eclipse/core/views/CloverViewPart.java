package com.atlassian.clover.eclipse.core.views;

import com.atlassian.clover.eclipse.core.views.widgets.ViewAlertContainer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

public abstract class CloverViewPart extends ViewPart {
    /**
     * Message container for alerting users to important information
     */
    protected Composite parent;
    protected ViewAlertContainer alertContainer;
    protected int viewOrientation;
    /**
     * Contains the tree etc
     */
    protected SashForm mainContent;

    @Override
    public void createPartControl(Composite parent) {
        this.parent = parent;
        this.parent.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent event) {
                int viewOrientation = calcViewOrientation();
                if (CloverViewPart.this.viewOrientation != viewOrientation) {
                    CloverViewPart.this.viewOrientation = viewOrientation;
                    onViewOrientationChange(viewOrientation);
                }
            }
        });
        this.viewOrientation = calcViewOrientation();
    }

    protected void onViewOrientationChange(int viewOrientation) {
        updateMainContentSashOrientation(viewOrientation);
    }

    protected int calcViewOrientation() {
        Point size = parent.getSize();
        if (size.x != 0 && size.y != 0) {
            if (size.x > size.y)
                return SWT.HORIZONTAL;
            else
                return SWT.VERTICAL;
        }
        return getAssumedViewOrientationIfUnknowable();
    }

    protected abstract int getAssumedViewOrientationIfUnknowable();

    protected void updateMainContentSashOrientation(int viewOrientation) {
        mainContent.setOrientation(viewOrientation);
    }
}
