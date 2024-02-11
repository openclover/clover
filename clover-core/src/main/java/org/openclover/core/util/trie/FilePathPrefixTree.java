package org.openclover.core.util.trie;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.StringTokenizer;

import static org.openclover.core.util.Lists.newArrayList;

/**
 * Prefix tree working on file paths
 */
public class FilePathPrefixTree<V> extends PrefixTree<String, V> {

    public static class FileKeySequence extends KeySequence<String> {

        public FileKeySequence(@NotNull final File file) {
            super(filePathToSequence(file));
        }

        @NotNull
        protected static List<String> filePathToSequence(@NotNull final File path) {
            final List<String> sequence = newArrayList();
            final StringTokenizer tokenizer = new StringTokenizer(path.getAbsolutePath(), File.separator);
            while (tokenizer.hasMoreTokens()) {
                sequence.add(tokenizer.nextToken());
            }
            return sequence;
        }
    }

    private static final String EMPTY_KEY = "";

    /**
     * Create empty prefix tree with no value in the root node.
     */
    public FilePathPrefixTree() {
        super(NodeFactoryImpl.HASH_MAP_BACKED, EMPTY_KEY, null);
    }

    /**
     * Create empty prefix tree with a specified <code>value</code> in the root node.
     */
    public FilePathPrefixTree(@Nullable final V value) {
        super(NodeFactoryImpl.HASH_MAP_BACKED, EMPTY_KEY, value);
    }

    public void add(@NotNull final File filePath, @Nullable final V value) {
        add(new FileKeySequence(filePath), value);
    }

    @Nullable
    public Node<String, V> find(@NotNull final File filePath) {
        return find(new FileKeySequence(filePath));
    }

    @NotNull
    public Node<String, V> findNearest(@NotNull final File filePath) {
        return findNearest(new FileKeySequence(filePath));
    }

    @Nullable
    public Node<String, V> findNearestWithValue(@NotNull final File filePath) {
        return findNearestWithValue(new FileKeySequence(filePath));
    }

}
