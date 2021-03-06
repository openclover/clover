package com.atlassian.clover.api.registry;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;

public interface MethodSignatureInfo {
    ModifiersInfo getModifiers();

    @NotNull
    Map<String, Collection<Annotation>> getAnnotations();

    String getName();

    String getReturnType();

    String getTypeParams();

    ParameterInfo[] getParameters();

    boolean hasParams();

    int getParamCount();

    String[] getThrowsTypes();

    String getNormalizedSignature();
}
