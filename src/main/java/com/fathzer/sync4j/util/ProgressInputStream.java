package com.fathzer.sync4j.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.function.LongConsumer;

import jakarta.annotation.Nonnull;

/**
 * An input stream that reports read progress to a listener.
 */
public class ProgressInputStream extends InputStream {
    private final InputStream delegate;
    private final LongConsumer listener;
    private long bytesRead;

    /**
     * Creates a new ProgressInputStream.
     * @param delegate the underlying input stream
     * @param listener the progress listener to notify
     * @throws NullPointerException if delegate or listener is null
     */
    public ProgressInputStream(@Nonnull InputStream delegate, @Nonnull LongConsumer listener) {
        this.delegate = Objects.requireNonNull(delegate, "Delegate stream cannot be null");
        this.listener = Objects.requireNonNull(listener, "Listener cannot be null");
        this.bytesRead = 0;
        
        // Initial progress notification (0%)
        listener.accept(0);
    }

    @Override
    public int read() throws IOException {
        int result = delegate.read();
        if (result != -1) {
            bytesRead++;
            listener.accept(bytesRead);
        }
        return result;
    }

    @Override
    public int read(@Nonnull byte[] b, int off, int len) throws IOException {
        int count = delegate.read(b, off, len);
        if (count > 0) {
            bytesRead += count;
            listener.accept(bytesRead);
        }
        return count;
    }

    @Override
    public long skip(long n) throws IOException {
        long count = delegate.skip(n);
        if (count > 0) {
            bytesRead += count;
            listener.accept(bytesRead);
        }
        return count;
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    public int available() throws IOException {
        return delegate.available();
    }

    @Override
    public synchronized void mark(int readlimit) {
        delegate.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        delegate.reset();
        // Update bytesRead based on the new position if possible
        // Note: This is an approximation as we can't get the exact position from the stream
        bytesRead = Math.max(0, bytesRead - delegate.available());
        listener.accept(bytesRead);
    }

    @Override
    public boolean markSupported() {
        return delegate.markSupported();
    }
}
