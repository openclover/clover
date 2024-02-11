package org.openclover.core.util;

import org.openclover.runtime.util.ByteSized;

import java.util.Map;
import java.util.LinkedHashMap;

public class SizedLRUCacheMap<K, V extends ByteSized> extends LinkedHashMap<K, V> {
    private long maxLength;
    private long currentLength;

    public SizedLRUCacheMap(long maxLength, int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor, true);
        this.maxLength = maxLength;
        this.currentLength = 0;
    }

    long getCurrentSizeInBytes() {
        return currentLength;
    }

    @Override
    public V remove(Object key) {
        V removed = super.remove(key);
        if (removed != null) {
            currentLength -= removed.sizeInBytes();
        }
        return removed;
    }

    @Override
    public void clear() {
        currentLength = 0;
        super.clear();
    }

    ///CLOVER:OFF
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException();
    }
    ///CLOVER:ON

    @Override
    public V put(K key, V value) {
        V replaced = super.put(key, value);
        if (replaced != null) {
            currentLength -= replaced.sizeInBytes();
        }
        currentLength += value.sizeInBytes();
        return replaced;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        final long eldestSize = eldest.getValue().sizeInBytes();
        if (currentLength + eldestSize > maxLength) {
            currentLength -= eldestSize;
            return true;
        } else {
            return false;
        }
    }
}