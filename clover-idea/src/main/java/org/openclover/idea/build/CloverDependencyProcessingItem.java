package org.openclover.idea.build;

import com.intellij.openapi.compiler.FileProcessingCompiler;
import com.intellij.openapi.compiler.ValidityState;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class CloverDependencyProcessingItem implements FileProcessingCompiler.ProcessingItem {
    private final VirtualFile virtualFile;
    private final ValidityState validityState;

    public CloverDependencyProcessingItem(VirtualFile virtualFile, ValidityState validityState) {
        this.virtualFile = virtualFile;
        this.validityState = validityState;
    }

    @Override
    @NotNull
    public VirtualFile getFile() {
        return virtualFile;
    }

    @Override
    public ValidityState getValidityState() {
        return validityState;
    }
}
