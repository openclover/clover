package com.atlassian.clover.util.trie;

import org.jetbrains.annotations.NotNull;

public interface NodeVisitor<K, V> {
    /**
     * Visit given node.
     *
     * @param node  current node
     * @param depth current recursion depth
     * @return Node&lt;K, V&gt; state of a node after visiting (may be a new instance if modifed)
     */
    Node<K, V> visit(@NotNull final Node<K, V> node, int depth);
}
