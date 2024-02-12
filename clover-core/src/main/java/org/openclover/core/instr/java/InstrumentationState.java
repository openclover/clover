package org.openclover.core.instr.java;

import org.openclover.core.api.instrumentation.InstrumentationSession;
import org.openclover.core.api.registry.ContextSet;
import org.openclover.core.cfg.instr.java.JavaInstrumentationConfig;
import org.openclover.core.context.ContextSetImpl;
import org.openclover.core.instr.tests.TestDetector;
import org.openclover.core.registry.entities.FullFileInfo;
import org.openclover.core.spi.lang.Language;

import java.io.File;

/**
 * contains various state variables used during the instrumentation phase
 */
public class InstrumentationState implements TestDetector.SourceContext {
    private final InstrumentationSession session;
    private final FullFileInfo fileInfo;
    private final FileStructureInfo structure;
    private final JavaInstrumentationConfig cfg;
    private int boolIndex;
    private String recorderPrefix;
    private boolean needsFlush = false;
    private boolean dirty = false;
    private boolean instrEnabled = true;
    private boolean detectTests = false;
    private boolean spockTestClass = false;
    private boolean parameterizedJUnitTestClass = false;
    private boolean parameterizedJUnit5TestClass = false;
    private ContextSet instrContext = new ContextSetImpl();
    private boolean hasInstrumented = false;
    private int autoCloseableClassCount = 0;
    private int autoCloseableInstanceCount = 0;

    public InstrumentationState(InstrumentationSession session, FullFileInfo fileInfo, FileStructureInfo structure,
                                JavaInstrumentationConfig cfg) {
        this.session = session;
        this.fileInfo = fileInfo;
        this.structure = structure;
        this.cfg = cfg;
    }

    public FullFileInfo getFileInfo() {
        return fileInfo;
    }

    public boolean isInstrEnabled() {
        return instrEnabled;
    }

    public void setInstrEnabled(boolean instrEnabled) {
        this.instrEnabled = instrEnabled;
    }

    public ContextSet getInstrContext() {
        return instrContext;
    }

    public FileStructureInfo getStructure() {
        return structure;
    }

    public int getBoolIndex() {
        return boolIndex;
    }

    public void setBoolIndex(int boolIndex) {
        this.boolIndex = boolIndex;
    }

    public int getIncBoolIndex() {
        return this.boolIndex++;
    }

    public InstrumentationSession getSession() {
        return session;
    }

    public String getRecorderPrefix() {
        return recorderPrefix;
    }

    public void setRecorderPrefix(String recorderPrefix) {
        this.recorderPrefix = recorderPrefix;
    }

    public boolean needsFlush() {
        return needsFlush;
    }

    public void setNeedsFlush(boolean needsFlush) {
        this.needsFlush = needsFlush;
    }

    public void setInstrContext(ContextSet instrContext) {
        this.instrContext = instrContext;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty() {
        this.dirty = true;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public boolean isDetectTests() {
        return detectTests;
    }

    public void setDetectTests(boolean detectTests) {
        this.detectTests = detectTests;
    }

    public TestDetector getTestDetector() {
        return cfg.getTestDetector();
    }

    public JavaInstrumentationConfig getCfg() {
        return cfg;
    }

    public boolean hasInstrumented() {
        return hasInstrumented;
    }

    public void setHasInstrumented(boolean hasInstrumented) {
        this.hasInstrumented = hasInstrumented;
    }

    @Override
    public boolean areAnnotationsSupported() {
        return true;
    }

    @Override
    public File getSourceFile() {
        return fileInfo.getPhysicalFile();
    }

    @Override
    public Language getLanguage() {
        return Language.Builtin.JAVA;
    }

    public int getAutoCloseableClassCount() {
        return autoCloseableClassCount;
    }

    public int incAutoCloseableClassCount() {
        this.autoCloseableClassCount += 1;
        return this.autoCloseableClassCount;
    }

    public int getAutoCloseableInstanceCount() {
        return autoCloseableInstanceCount;
    }

    public int incAutoCloseableInstanceCount() {
        this.autoCloseableInstanceCount += 1;
        return this.autoCloseableInstanceCount;
    }

    /**
     * Returns true if during instrumentation it was found that the class contains Spock framework's annotations
     * attached to the class.
     * @return boolean true if it's a Spock test class, false otherwise
     */
    public boolean isSpockTestClass() {
        return spockTestClass;
    }

    public void setSpockTestClass(boolean spockTestClass) {
        this.spockTestClass = spockTestClass;
    }

    /**
     * Returns true if during instrumentation it was detected that it's a JUnit4 class
     * with @Parameterized annotation.
     * @return boolean - true for parameterized JUnit, false otherwise
     */
    public boolean isParameterizedJUnitTestClass() {
        return parameterizedJUnitTestClass;
    }

    public void setParameterizedJUnitTestClass(boolean parameterizedJUnitTestClass) {
        this.parameterizedJUnitTestClass = parameterizedJUnitTestClass;
    }
	
	/**
     * Returns true if during instrumentation if atleast one test method is annotated with ParameterizedTest annotation.
     * @return boolean - true for a Test class with a @ParameterizedTest method, false otherwise
     */
    public boolean isParameterizedJUnit5TestClass() {
        return parameterizedJUnit5TestClass;
    }

    public void setParameterizedJUnit5TestClass(boolean parameterizedJUnit5TestClass) {
        this.parameterizedJUnit5TestClass = parameterizedJUnit5TestClass;
    }
}
