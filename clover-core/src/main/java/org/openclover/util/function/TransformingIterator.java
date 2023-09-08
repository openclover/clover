package org.openclover.util.function;

import java.util.Iterator;

public class TransformingIterator<S, T, F extends Function<S, T>> implements Iterator<T> {

    private final Iterator<S> sourceIterator;
    private final F mapper;

    public TransformingIterator(Iterator<S> sourceIterator, F mapper) {
        this.sourceIterator = sourceIterator;
        this.mapper = mapper;
    }

    @Override
    public boolean hasNext() {
        return sourceIterator.hasNext();
    }

    @Override
    public T next() {
        return mapper.apply(sourceIterator.next());
    }

    @Override
    public void remove() {
        sourceIterator.remove();
    }
}
