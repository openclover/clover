package org.openclover.idea.autoupdater;

import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openclover.idea.util.ui.CloverIcons;

import javax.swing.Icon;
import java.awt.event.MouseEvent;

class StatusBarCloverIconPresentation implements StatusBarWidget.IconPresentation {

    private final Consumer<MouseEvent> clickConsumer;

    public StatusBarCloverIconPresentation(Consumer<MouseEvent> clickConsumer) {
        this.clickConsumer = clickConsumer;
    }

    @Nullable
    @Override
    public String getTooltipText() {
        return "New OpenClover version is available";
    }

    @Nullable
    @Override
    public Consumer<MouseEvent> getClickConsumer() {
        return clickConsumer;
    }

    @NotNull
    @Override
    public Icon getIcon() {
        return CloverIcons.CLOVER_MDL;
    }
}
