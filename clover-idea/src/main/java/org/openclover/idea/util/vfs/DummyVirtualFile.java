package org.openclover.idea.util.vfs;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DummyVirtualFile extends VirtualFile {
    protected final String name;

    public DummyVirtualFile(String name) {
        this.name = name;
    }

    @Override
    @NotNull
    public String getName() {
        return name;
    }

    @Override
    @NotNull
    public VirtualFileSystem getFileSystem() {
        return DummyVirtualFileSystem.getInstance();
    }

    @Override
    public String getPath() {
        return getName();
    }

    @Override
    @NotNull
    public String getUrl() {
        return DummyVirtualFileSystem.SCHEME + getName();
    }

    @Override
    public boolean isWritable() {
        return true; // to avoid the ugly padlock overlay on the file icon
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public VirtualFile getParent() {
        return null;
    }

    @Override
    public VirtualFile[] getChildren() {
        return new VirtualFile[0];
    }

    @Override
    public OutputStream getOutputStream(Object obj, long l, long l1) throws IOException {
        throw new UnsupportedOperationException("method getOutputStream not implemented");
    }

    @Override
    public byte[] contentsToByteArray() throws IOException {
        throw new UnsupportedOperationException("method contentsToByteArray not implemented");
    }

    @Override
    public long getTimeStamp() {
        return 0L;
    }

    @Override
    public long getModificationStamp() {
        return 0L;
    }

    @Override
    public long getLength() {
        return 0;
    }

    @Override
    public void refresh(boolean flag, boolean flag1, Runnable runnable) {
        //noop by default, we don't need to do anything here since we do listen to {{@link CoverageListener}}
        //but let's not thrown an exception since it's going to be show on the UI event log.
    }

    @Override
    public InputStream getInputStream() throws IOException {
        throw new UnsupportedOperationException("method getInputStream not implemented");
    }
}
