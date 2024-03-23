package org.openclover.core.instr.java;

import org.openclover.core.api.registry.ContextSet;
import org.openclover.core.instr.tests.naming.JUnitParameterizedTestExtractor;
import org.openclover.core.instr.tests.naming.SpockFeatureNameExtractor;
import org.openclover.core.registry.FixedSourceRegion;
import org.openclover.core.registry.entities.FullClassInfo;
import org.openclover.core.registry.entities.Modifiers;
import org.openclover.runtime.CloverNames;

import java.util.List;
import java.util.Map;

public class ClassEntryNode extends Emitter {
    private final String className;
    private final String pkgName;
    private final String superclass;
    private final boolean isTopLevel;
    private final boolean isInterface;
    private final boolean isEnum;
    private final boolean isAnnotation;
    private final Map<String, List<String>> tags;
    private final Modifiers mods;
    private boolean outerDetectTests;
    private CloverToken recorderInsertPoint;
    private RecorderInstrEmitter recorderInstrEmitter;

    public ClassEntryNode(Map<String, List<String>> tags, Modifiers mods,
                          String className, String pkgName, String superclass,
                          ContextSet context, int line, int col, boolean isTopLevel,
                          boolean isInterface, boolean isEnum, boolean isAnnotation) {
        super(context, line, col);
        this.tags = tags;
        this.mods = mods;
        this.className = className;
        this.pkgName = pkgName;
        this.superclass = superclass;
        this.isTopLevel = isTopLevel;
        this.isInterface = isInterface;
        this.isEnum = isEnum;
        this.isAnnotation = isAnnotation;
    }

    @Override
    public void init(InstrumentationState state) {
        outerDetectTests = state.isDetectTests();

        boolean testClass = state.getTestDetector() != null &&
                state.getTestDetector().isTypeMatch(state, new JavaTypeContext(tags, mods, pkgName, className, superclass));
        state.setDetectTests(testClass);
        // using 'testClass &&' as user could have custom test detector
        state.setSpockTestClass(testClass && SpockFeatureNameExtractor.isClassWithSpecAnnotations(mods));
        state.setParameterizedJUnitTestClass(testClass && JUnitParameterizedTestExtractor.isParameterizedClass(mods));

        FullClassInfo clazz = (FullClassInfo) state.getSession().enterClass(className,
                new FixedSourceRegion(getLine(), getColumn()), mods,
                isInterface, isEnum, isAnnotation);

        if (isTopLevel) {
            String recorderPrefix =
                CloverNames.recorderPrefixFor(
                    state.getFileInfo().getDataIndex(),
                    clazz.getDataIndex());

            if (state.getCfg().isClassInstrStrategy() || isEnum) {
                recorderPrefix += "." + CloverNames.RECORDER_FIELD_NAME;
            }
            state.setRecorderPrefix(recorderPrefix);
        }
    }

    public CloverToken getRecorderInsertPoint() {
        return recorderInsertPoint;
    }

    public void setRecorderInsertPoint(CloverToken recorderInsertPoint) {
        this.recorderInsertPoint = recorderInsertPoint;
    }

    public boolean isOuterDetectTests() {
        return outerDetectTests;
    }

    public void setRecorderInstrEmitter(RecorderInstrEmitter emitter) {
        this.recorderInstrEmitter = emitter;
    }

    public RecorderInstrEmitter getRecorderInstrEmitter() {
        return recorderInstrEmitter;
    }
}
