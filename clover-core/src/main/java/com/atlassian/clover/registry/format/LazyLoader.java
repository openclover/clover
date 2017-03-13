package com.atlassian.clover.registry.format;

import com.atlassian.clover.registry.RegistryFormatException;

import java.io.IOException;
import java.nio.channels.FileChannel;

/** A future T read from a {@link FileChannel} */
public abstract class LazyLoader<T> implements LazyProxy<T> {
    private final FileChannel channel;
    private final long position;
    private T result;

    protected LazyLoader(FileChannel channel, long position) {
        this.channel = channel;
        this.position = position;
    }

    @Override
    public T get() throws RegistryLoadException {
        try {
            if (result == null) {
                channel.position(position);
                result = getImpl(channel);
            }
            return result;
        } catch (Exception e) {
            throw new RegistryLoadException(e);
        }
    }

    /** Factory method for getting the future object now */
    protected abstract T getImpl(FileChannel channel) throws IOException, RegistryFormatException;
}
