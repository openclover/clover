package org.openclover.core.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public abstract class Lists {
    public static <S> ArrayList<S> newArrayList() {
        return new ArrayList<>();
    }

    @SafeVarargs
    public static <S> ArrayList<S> newArrayList(S... items) {
        final ArrayList<S> list = new ArrayList<>(items.length);
        Collections.addAll(list, items);
        return list;
    }

    public static <S> ArrayList<S> newArrayList(Collection<? extends S> items) {
        return new ArrayList<>(items);
    }

    public static <S> List<S> join(Collection<? extends S> list1, Collection<? extends S> list2) {
        final List<S> joined = new ArrayList<>(list1);
        joined.addAll(list2);
        return joined;
    }

    public static <S> LinkedList<S> newLinkedList() {
        return new LinkedList<>();
    }

    public static <S> LinkedList<S> newLinkedList(Collection<? extends S> items) {
        return new LinkedList<>(items);
    }
}
