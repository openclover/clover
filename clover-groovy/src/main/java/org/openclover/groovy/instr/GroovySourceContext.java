package org.openclover.groovy.instr;

import org.openclover.core.instr.tests.TestDetector;
import org.openclover.core.spi.lang.Language;

import java.io.File;

class GroovySourceContext implements TestDetector.SourceContext {
    private final File srcFile;

    GroovySourceContext(File srcFile) {
        this.srcFile = srcFile;
    }

    @Override
    public Language getLanguage() {
        return Language.Builtin.GROOVY;
    }

    @Override
    public boolean areAnnotationsSupported() {
        return true;
    }

    @Override
    public File getSourceFile() {
        return srcFile;
    }
}
