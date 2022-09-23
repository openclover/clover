package com.atlassian.clover.util.trie;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface NodeFactory {
    <K, V> Node<K, V> createNode(@NotNull K key, @Nullable V value);

    /**
     * Make a shallow copy of Node#children()
     */
    <K, V> Map<K, Node<K, V>> cloneChildren(@NotNull Node<K, V> node);
}
