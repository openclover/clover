package com.atlassian.clover.idea.build;

import com.atlassian.clover.instr.java.InstrumentationSource;
import com.atlassian.clover.idea.util.CharsetUtil;
import com.atlassian.clover.idea.util.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Instrumentation source which reads from a VirtualFile.
 */
public class VirtualFileInstrumentationSource implements InstrumentationSource {

    private final VirtualFile virtualFile;

    /**
     *
     * @param virtualFile file
     */
    public VirtualFileInstrumentationSource(final VirtualFile virtualFile) {
        this.virtualFile = virtualFile;
    }

    @Override
    public File getSourceFileLocation() {
        return VfsUtil.convertToFile(virtualFile);
    }

    @Override
    public Reader createReader() throws IOException {
        final String fileEncoding = CharsetUtil.getFileEncoding(virtualFile);
        if (fileEncoding == null) {
            return new InputStreamReader(virtualFile.getInputStream());
        } else {
            return new InputStreamReader(virtualFile.getInputStream(), fileEncoding);
        }
    }

}
