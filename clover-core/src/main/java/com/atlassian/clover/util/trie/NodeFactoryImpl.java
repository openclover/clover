package com.atlassian.clover.util.trie;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 */
public abstract class NodeFactoryImpl implements NodeFactory {
    public static final NodeFactory HASH_MAP_BACKED = new NodeFactory() {
        @Override
        public <K, V> Node<K, V> createNode(@NotNull K key, @Nullable V value) {
            return new NodeImpl<>(key, value, new HashMap<K, Node<K, V>>());
        }

        @Override
        public <K, V> Map<K, Node<K, V>> cloneChildren(@NotNull Node<K, V> node) {
            return new HashMap<>(node.children());
        }
    };

    public static final NodeFactory TREE_MAP_BACKED = new NodeFactory() {
        @Override
        public <K, V> Node<K, V> createNode(@NotNull K key, @Nullable V value) {
            return new NodeImpl<>(key, value, new TreeMap<K, Node<K, V>>());
        }

        @Override
        public <K, V> Map<K, Node<K, V>> cloneChildren(@NotNull Node<K, V> node) {
            return new TreeMap<>(node.children());
        }
    };
}
