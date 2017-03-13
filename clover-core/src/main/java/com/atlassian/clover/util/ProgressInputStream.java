package com.atlassian.clover.util;

import com.atlassian.clover.ProgressListener;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ProgressInputStream extends FilterInputStream {
    private final ProgressListener progressListener;
    private final String message;
    private long counter;
    private final float total;
    private long markedCount;

    private void updateProgressListener() {
        progressListener.handleProgress(message, counter / total);
    }

    public ProgressInputStream(InputStream is, long length, ProgressListener progressListener, String message) {
        super(is);
        this.progressListener = progressListener;
        this.message = message;

        total = (float) (length > 0 ? length : 1);
    }

    @Override
    public int read() throws IOException {
        ++counter;
        return super.read();
    }

    /**
     * This one would be called also by {@link #read(byte[])}
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        final int read = super.read(b, off, len);
        if (read != -1) {
            counter += read;
        }
        updateProgressListener();
        return read;
    }

    @Override
    public long skip(long n) throws IOException {
        final long skipped = super.skip(n);
        counter += skipped;
        updateProgressListener();
        return skipped;
    }

    @Override
    public void mark(int readlimit) {
        super.mark(readlimit);
        markedCount = counter;
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        counter = markedCount; // unless IOException is thrown
        updateProgressListener();
    }
}
