package org.openclover.util;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public abstract class Maps {

    public static <S, T> HashMap<S, T> newHashMap() {
        return new HashMap<>();
    }

    public static <S, T> HashMap<S, T> newHashMap(Map<? extends S, ? extends T> inputMap) {
        return new HashMap<>(inputMap);
    }

    public static <S, T> LinkedHashMap<S, T> newLinkedHashMap() {
        return new LinkedHashMap<>();
    }

    public static <S extends Comparable<S>, T> TreeMap<S, T> newTreeMap() {
        return new TreeMap<>();
    }
}
