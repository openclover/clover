package com.atlassian.clover.util.trie;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the prefix tree (trie) data structure http://en.wikipedia.org/wiki/Trie
 *
 * @param <K> type of the sub-key
 * @param <V> value to be stored in a leaf
 */
public class PrefixTree<K, V> {

    /**
     * A single tree node containing some token (or is empty).
     *
     * @param <K> sub-key type
     * @param <V> value type
     */
    public static interface Node<K, V> {
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

    /**
     * A values which can be added to the tree, represented in as a sequence of tokens.
     *
     * @param <K> token type
     */
    public static class KeySequence<K> implements Iterable<K> {
        private final List<K> subKeys;

        public KeySequence() {
            subKeys = Collections.emptyList();
        }

        public KeySequence(List<K> subKeys) {
            this.subKeys = subKeys;
        }

        @Override
        public Iterator<K> iterator() {
            return subKeys.iterator();
        }
    }

    public static interface NodeFactory {
        <K, V> Node<K, V> createNode(@NotNull K key, @Nullable V value);

        /** Make a shallow copy of Node#children() */
        <K, V> Map<K, Node<K, V>> cloneChildren(@NotNull Node<K, V> node);
    }

    public static interface NodeVisitor<K, V> {
        /**
         * Visit given node.
         * @param node current node
         * @param depth current recursion depth
         * @return Node&lt;K, V&gt; state of a node after visiting (may be a new instance if modifed)
         */
        Node<K, V> visit(@NotNull final Node<K, V> node, int depth);
    }

    @NotNull
    protected Node<K, V> rootNode;

    protected final NodeFactory nodeFactory;

    /**
     * @param nodeFactory for creating new nodes
     * @param value value for a root node
     */
    public PrefixTree(@NotNull final NodeFactory nodeFactory, @NotNull K key, @Nullable V value) {
        this.nodeFactory = nodeFactory;
        this.rootNode = nodeFactory.createNode(key, value);
    }

    /**
     * @param nodeFactory for creating new nodes
     * @param rootNode a root node
     */
    public PrefixTree(@NotNull final NodeFactory nodeFactory, @NotNull Node<K, V> rootNode) {
        this.nodeFactory = nodeFactory;
        this.rootNode = rootNode;
    }

    /**
     * Adds new value to the trie. Might replace existing one if key sequence is the same.
     *
     * @param keySequence key
     * @param value       value
     */
    public void add(@NotNull final KeySequence<K> keySequence, @Nullable final V value) {
        Node<K, V> currentNode = rootNode;
        for (K token : keySequence) {
            Node<K, V> nextNode = currentNode.getChild(token);
            if (nextNode == null) {
                nextNode = currentNode.addChild(nodeFactory.createNode(token, (V)null));
            }
            currentNode = nextNode;
        }
        currentNode.setValue(value);
    }

    /**
     * Search for a key
     *
     * @param keySequence key
     * @return Node&lt;K,V&gt; or <code>null</code> if not found
     */
    @Nullable
    public Node<K, V> find(@NotNull final KeySequence<K> keySequence) {
        Node<K, V> current = rootNode;
        for (K token : keySequence) {
            current = current.getChild(token);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    /**
     * Search as deep as possible and return the deepest node matching a key sequence. Warning: this method might return
     * a non-leaf (i.e. the one which was not added via add()), can be useful for setting values in intermediate nodes.
     * Consider using findNearestWithValue() for searching.
     *
     * @param keySequence key
     * @return Node&lt;K,V&gt;, it can be a root node if not found
     */
    @NotNull
    public Node<K, V> findNearest(@NotNull final KeySequence<K> keySequence) {
        Node<K, V> current = rootNode;
        for (K token : keySequence) {
            Node<K, V> next = current.getChild(token);
            if (next == null) {
                // no more matching sub-keys, return what we've got so far
                return current;
            }
            current = next;
        }
        return current;
    }

    /**
     * Search as deep as possible and return the deepest node containing a non-null value matching the keySequence.
     *
     * @param keySequence key
     * @return Node&lt;K,V&gt; with a value found; it can be the root node if key was not found at all and the root node
     *         has a value associated with it; or <code>null</code> if no value was not found, even in the root node
     */
    @Nullable
    public Node<K, V> findNearestWithValue(@NotNull final KeySequence<K> keySequence) {
        Node<K, V> current = rootNode;
        Node<K, V> lastWithValue = rootNode.getValue() != null ? rootNode : null;
        for (K token : keySequence) {
            Node<K, V> next = current.getChild(token);
            if (next != null) {
                current = next;
                if (current.getValue() != null) {
                    // remember the node with value
                    lastWithValue = current;
                }
            } else {
                break;
            }
        }
        return lastWithValue;
    }

    @NotNull
    public Node<K, V> getRootNode() {
        return rootNode;
    }

    /**
     * Print entire tree to System.out
     */
    public void printTree() {
        printTree(System.out, rootNode);
    }

    /**
     * Print tree starting from a <code>node</code> to output stream <code>out</code>. Recursive method.
     *
     * @param out    output stream
     * @param rootNode   node for which tree shall be printed
     */
    void printTree(@NotNull final PrintStream out, @NotNull final Node<K, V> rootNode) {
        final NodeVisitor<K,V> nodePrinter = new NodeVisitor<K, V>() {
            @Override
            public Node<K, V> visit(@NotNull Node<K, V> node, int depth) {
                final StringBuilder line = new StringBuilder();
                for (int i = 0; i < depth; i++) {
                    line.append("  ");
                }
                line.append('+');

                line.append(node.getKey());
                if (node.getValue() != null) {
                    line.append(" (").append(node.getValue().toString()).append(')');
                }

                out.println(line);
                return node;
            }
        };

        walkTree(rootNode, nodePrinter);
    }

    /**
     * Walk through all the nodes of the prefix tree (including empty intermediate nodes) calling the callback.
     * Walking is performed IN-ORDER (current node first, next children). It's not specified in which order child
     * nodes will be visited.
     *
     * Walking assumes that the NodeVisitor will not replace instances of the nodes (i.e. that tree structure
     * does not change while walking).
     *
     * @param rootNode tree root
     * @param call  callback method
     */
    public void walkTree(@NotNull final Node<K, V> rootNode, @NotNull final NodeVisitor<K, V> call) {
        walkTree(rootNode, 0, call);
    }

    /**
     * Walk through all the nodes of the prefix tree (including empty intermediate nodes) calling the callback.
     * Walking is performed IN-ORDER (current node first, next children). It's not specified in which order child
     * nodes will be visited.
     *
     * Walking assumes that the NodeVisitor will not replace instances of the nodes (i.e. that tree structure
     * does not change while walking).
     *
     * @param node current node
     * @param depth depth in tree of the current node
     * @param call callback method
     */
    protected void walkTree(@NotNull final Node<K, V> node, int depth,
                            @NotNull final NodeVisitor<K, V> call) {
        call.visit(node, depth);
        for (Map.Entry<K, Node<K, V>> entry : node.children().entrySet()) {
            walkTree(entry.getValue(), depth + 1, call);
        }
    }

    /**
     * Walk through all the nodes of the prefix tree (including empty intermediate nodes) calling the callback.
     * Walking is performed IN-DEPTH (children first, current node last). It's not specified in which order child
     * nodes will be visited.
     *
     * Walking assumes that the NodeVisitor may replace instances of the nodes (i.e. that tree structure
     * may change while walking).
     *
     * @param rootNode tree root
     * @param call  callback method
     * @return Node&lt;K, V&gt; new root node
     */
    public Node<K, V> rewriteTree(@NotNull final Node<K, V> rootNode, @NotNull final NodeVisitor<K, V> call) {
        return rewriteTree(rootNode, 0, call);
    }

    /**
     * Walk through all the nodes of the prefix tree (including empty intermediate nodes) calling the callback.
     * Walking is performed IN-DEPTH (children first, current node last). It's not specified in which order child
     * nodes will be visited.
     *
     * Walking assumes that the NodeVisitor may replace instances of the nodes (i.e. that tree structure
     * may change while walking).
     *
     * @param node current node
     * @param depth depth in tree of the current node
     * @param call callback method
     * @return Node&lt;K, V&gt; new current node
     */
    protected Node<K, V> rewriteTree(@NotNull final Node<K, V> node, int depth,
                                     @NotNull final NodeVisitor<K, V> call) {

        // remember existing children, we'll replace them one after another (recursively btw)
        final Map<K, Node<K, V>> oldChildren = nodeFactory.cloneChildren(node);
        node.children().clear();

        for (final Map.Entry<K, Node<K, V>> entry : oldChildren.entrySet()) {
            // visit child, add modified child to the current node
            node.addChild(rewriteTree(entry.getValue(), depth + 1, call));
        }

        // visit current node, we may get new instance as a result
        return call.visit(node, depth);
    }
}
