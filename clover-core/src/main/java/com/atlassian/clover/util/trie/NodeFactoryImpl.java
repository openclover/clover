package com.atlassian.clover.util.trie;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 */
public abstract class NodeFactoryImpl implements PrefixTree.NodeFactory {
    public static final PrefixTree.NodeFactory HASH_MAP_BACKED = new PrefixTree.NodeFactory() {
        @Override
        public <K, V> PrefixTree.Node<K, V> createNode(@NotNull K key, @Nullable V value) {
            return new NodeImpl<K, V>(key, value, new HashMap<K, PrefixTree.Node<K, V>>());
        }

        @Override
        public <K, V> Map<K, PrefixTree.Node<K, V>> cloneChildren(@NotNull PrefixTree.Node<K, V> node) {
            return new HashMap<K, PrefixTree.Node<K, V>>(node.children());
        }
    };

    public static final PrefixTree.NodeFactory TREE_MAP_BACKED = new PrefixTree.NodeFactory() {
        @Override
        public <K, V> PrefixTree.Node<K, V> createNode(@NotNull K key, @Nullable V value) {
            return new NodeImpl<K, V>(key, value, new TreeMap<K, PrefixTree.Node<K, V>>());
        }

        @Override
        public <K, V> Map<K, PrefixTree.Node<K, V>> cloneChildren(@NotNull PrefixTree.Node<K, V> node) {
            return new TreeMap<K, PrefixTree.Node<K, V>>(node.children());
        }
    };
}
