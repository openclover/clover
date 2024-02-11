package org.openclover.core.util.trie;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A values which can be added to the tree, represented in as a sequence of tokens.
 *
 * @param <K> token type
 */
public class KeySequence<K> implements Iterable<K> {
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
