package com.atlassian.clover.util.trie;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
*
*/
public class NodeImpl<K, V> implements Node<K, V> {

    @NotNull
    protected final Map<K, Node<K, V>> children;

    @NotNull
    private final K key;

    @Nullable
    private V value;

    public NodeImpl(@NotNull K key, @Nullable V value, @NotNull Map<K, Node<K, V>> children) {
        this.key = key;
        this.value = value;
        this.children = children;
    }

    @NotNull
    @Override
    public K getKey() {
        return key;
    }

    @Nullable
    @Override
    public V getValue() {
        return value;
    }

    @Override
    public void setValue(@Nullable V value) {
        this.value = value;
    }

    @Nullable
    @Override
    public Node<K, V> getChild(@NotNull K subKey) {
        return children.get(subKey);
    }

    @NotNull
    @Override
    public Node<K, V> addChild(@NotNull Node<K, V> subNode) {
        children.put(subNode.getKey(), subNode);
        return children.get(subNode.getKey());
    }

    @NotNull
    @Override
    public Map<K, Node<K, V>> children() {
        return children;
    }
}
