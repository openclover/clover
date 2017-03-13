package com.atlassian.clover.util.trie;

import com.atlassian.clover.reporters.html.PackageInfoExt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.StringTokenizer;

import static clover.com.google.common.collect.Lists.newArrayList;

/**
 *
 */
public class PackagePrefixTree extends PrefixTree<String, PackageInfoExt> {
    public static class PackageKeySequence extends KeySequence<String> {

        public PackageKeySequence(@NotNull final String packageName) {
            super(packageNameToSequence(packageName));
        }

        @NotNull
        protected static List<String> packageNameToSequence(@NotNull final String packageName) {
            final List<String> sequence = newArrayList();
            final StringTokenizer tokenizer = new StringTokenizer(packageName, ".");
            while (tokenizer.hasMoreTokens()) {
                sequence.add(tokenizer.nextToken());
            }
            return sequence;
        }
    }

    private static final String EMPTY_KEY = "";

    /**
     * Create empty prefix tree with no value in the root node. Using TreeMap as backend to have nodes
     * sorted alphabetically.
     */
    public PackagePrefixTree() {
        super(NodeFactoryImpl.TREE_MAP_BACKED, EMPTY_KEY, null);
    }

    public void add(@NotNull final String packageName, @Nullable final PackageInfoExt value) {
        add(new PackageKeySequence(packageName), value);
    }

    /**
     * Compress tree using default predicate and compressor:
     *  - parent and child nodes are contatenated if only one child is present AND there is no value in parent node set
     *  - concatenation means that a value of new node is taken from a child, while new node's key is concatenation of
     *     parent.key + "." + child.key
     */
    public void compressTree() {
        final NodeVisitor<String, PackageInfoExt> nodeCompressor = new NodeVisitor<String, PackageInfoExt>() {
            @Override
            public Node<String, PackageInfoExt> visit(@NotNull Node<String, PackageInfoExt> currentNode, int depth) {
                // can we merge nodes?
                if (currentNode.children().size() == 1 && currentNode.getValue() == null) {
                    // yes, merge nodes, keep child's value and children of a child node
                    final Node<String, PackageInfoExt> childNode = currentNode.children().values().iterator().next();
                    // don't put a dot if package name is empty (e.g. in a root node)
                    final String mergedKey = (currentNode.getKey().isEmpty() ? "" : currentNode.getKey() + ".") + childNode.getKey();
                    final Node<String, PackageInfoExt> mergedNode = nodeFactory.createNode(mergedKey, childNode.getValue());
                    mergedNode.children().putAll(childNode.children());
                    return mergedNode;
                } else {
                    // nothing to do
                    return currentNode;
                }
            }
        };

        rootNode = compressTree(rootNode, nodeCompressor);
    }

    public Node<String, PackageInfoExt> compressTree(Node<String, PackageInfoExt> rootNode, NodeVisitor<String, PackageInfoExt> nodeCompressor) {
        return rewriteTree(rootNode, nodeCompressor);
    }


}
