package org.openclover.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

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

    public static <S> LinkedList<S> newLinkedList() {
        return new LinkedList<>();
    }

    public static <S> LinkedList<S> newLinkedList(Collection<? extends S> items) {
        return new LinkedList<>(items);
    }
}
