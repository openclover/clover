package org.openclover.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.TreeSet;

public abstract class Sets {
    public static <S> HashSet<S> newHashSet() {
        return new HashSet<>();
    }

    @SafeVarargs
    public static <S> HashSet<S> newHashSet(S... items) {
        final HashSet<S> set = new HashSet<>(items.length);
        Collections.addAll(set, items);
        return set;
    }

    public static <S> HashSet<S> newHashSet(Collection<? extends S> items) {
        return new HashSet<>(items);
    }

    public static <S> LinkedHashSet<S> newLinkedHashSet() {
        return new LinkedHashSet<>();
    }

    public static <S> LinkedHashSet<S> newLinkedHashSet(Collection<? extends S> items) {
        return new LinkedHashSet<>(items);
    }

    public static <S extends Comparable<S>> TreeSet<S> newTreeSet() {
        return new TreeSet<>();
    }

    public static <S> TreeSet<S> newTreeSet(Comparator<? super S> comparator) {
        return new TreeSet<>(comparator);
    }
}
