package com.atlassian.clover.context;

import com.atlassian.clover.api.registry.MethodInfo;
import com.atlassian.clover.api.registry.ParameterInfo;
import com.atlassian.clover.instr.java.FileStructureInfo;

import java.util.regex.Pattern;

/**
 * A regular expression based context searching for getXyz(), setXyz(), isXyz() methods.
 */
public class PropertyMethodRegexpContext extends MethodRegexpContext {
    public PropertyMethodRegexpContext(int index, String name) {
        super(index, name, Pattern.compile("(.* )?public .*(get|set|is)[A-Z0-9].*"), 1, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    @Override
    public boolean matches(FileStructureInfo.MethodMarker methodMarker) {
        if (!super.matches(methodMarker)) {
            return false;
        }
        final MethodInfo info = methodMarker.getMethod();
        final String name = info.getSignature().getName();
        final ParameterInfo[] params = info.getSignature().getParameters();
        if (name.startsWith("get") || name.startsWith("is")) {
            return !info.getSignature().hasParams();
        }
        return params != null && params.length == 1;
    }
}
