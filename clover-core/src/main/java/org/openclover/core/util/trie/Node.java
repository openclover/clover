package org.openclover.core.util.trie;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * A single tree node containing some token (or is empty).
 *
 * @param <K> sub-key type
 * @param <V> value type
 */
public interface Node<K, V> {
    @Nullable
    V getValue();

    @NotNull
    K getKey();

    @Nullable
    Node<K, V> getChild(K subKey);

    @NotNull
    Node<K, V> addChild(@NotNull Node<K, V> subKey);

    void setValue(@Nullable V value);

    @NotNull
    Map<K, Node<K, V>> children();
}
