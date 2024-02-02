package org.openclover.idea.util.vfs;

import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.vfs.DeprecatedVirtualFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class DummyVirtualFileSystem extends DeprecatedVirtualFileSystem implements ApplicationComponent {
    private static final String PROTOCOL = "DummyCloverVirtualFileSystem";
    static final String SCHEME = PROTOCOL + "://";

    @NotNull
    @NonNls
    @Override
    public String getProtocol() {
        return PROTOCOL;
    }

    @Nullable
    @Override
    public VirtualFile findFileByPath(@NotNull @NonNls String s) {
        return null;
    }

    @Override
    public void refresh(boolean b) {
    }

    @Nullable
    @Override
    public VirtualFile refreshAndFindFileByPath(String s) {
        return null;
    }

    @Override
    protected void deleteFile(Object o, @NotNull VirtualFile virtualFile) throws IOException {
        throw new UnsupportedOperationException("method deleteFile not implemented");
    }

    @Override
    protected void moveFile(Object o, @NotNull VirtualFile virtualFile, @NotNull VirtualFile newParent) throws IOException {
        throw new UnsupportedOperationException("method moveFile not implemented");
    }

    @Override
    protected void renameFile(Object o, @NotNull VirtualFile virtualFile, @NotNull String newName) throws IOException {
        throw new UnsupportedOperationException("method renameFile not implemented");
    }

    @NotNull
    @Override
    public VirtualFile createChildFile(Object o, @NotNull VirtualFile virtualFile, @NotNull String fileName) throws IOException {
        throw new UnsupportedOperationException("method createChildFile not implemented");
    }

    @NotNull
    @Override
    public VirtualFile createChildDirectory(Object o, @NotNull VirtualFile virtualFile, @NotNull String dirName) throws IOException {
        throw new UnsupportedOperationException("method createChildDirectory not implemented");
    }

    @NotNull
    @Override
    public VirtualFile copyFile(Object o, @NotNull VirtualFile virtualFile, @NotNull VirtualFile virtualFile1, @NotNull String s) throws IOException {
        throw new UnsupportedOperationException("method copyFile not implemented");
    }

    private static final DummyVirtualFileSystem INSTANCE = new DummyVirtualFileSystem();

    public static DummyVirtualFileSystem getInstance() {
        return INSTANCE;
    }

    @Override
    public void disposeComponent() {
    }

    @NonNls
    @NotNull
    @Override
    public String getComponentName() {
        return "DummyVirtualFileSystem";
    }

    @Override
    public void initComponent() {
    }
}
