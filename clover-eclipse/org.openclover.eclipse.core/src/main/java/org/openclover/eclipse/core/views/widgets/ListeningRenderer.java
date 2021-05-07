package org.openclover.eclipse.core.views.widgets;

import org.eclipse.swt.widgets.Composite;

public interface ListeningRenderer {
    void startListening(Composite composite);
    void stopListening(Composite composite);
}
