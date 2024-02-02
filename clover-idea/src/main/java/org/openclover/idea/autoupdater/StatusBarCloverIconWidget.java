package org.openclover.idea.autoupdater;

import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.MouseEvent;

class StatusBarCloverIconWidget implements StatusBarWidget {

    private final StatusBarCloverIconPresentation iconPresentation;

    public StatusBarCloverIconWidget(Consumer<MouseEvent> clickConsumer) {
        iconPresentation = new StatusBarCloverIconPresentation(clickConsumer);
    }

    @NotNull
    @Override
    public String ID() {
        return this.getClass().getName();
    }

    @Nullable
    @Override
    public WidgetPresentation getPresentation(@NotNull PlatformType platformType) {
        return iconPresentation;
    }

    @Override
    public void install(@NotNull StatusBar statusBar) {

    }

    @Override
    public void dispose() {

    }
}
