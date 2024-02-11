package org.openclover.ant.types;

import org.openclover.core.api.optimization.Optimizable;

import java.io.File;

class TestFile implements Optimizable {
    private File file;

    TestFile(File file) {
        this.file = file;
    }

    @Override
    public String getName() {
        return BaseCloverOptimizedType.normalizePath(file.getAbsolutePath());
    }

    File getFile() {
        return file;
    }
}
