package com.fathzer.sync4j.sync;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.sync.parameters.SyncParameters;

import jakarta.annotation.Nonnull;

/**
 * A synchronization between a source and a destination folder.
 * <br>Please note that synchronization may produce unpredictable results if the source or destination folders are modified
 * while the synchronizer is running.
 */
public class Synchronization implements AutoCloseable {
    private Folder source;
    private Folder destination;
    private final Context context;

    /**
     * Creates a new synchronizer.
     * @param source the source folder
     * @param destination the destination folder
     * @param parameters the parameters
     * @throws IOException if an I/O error occurs
     */
    public Synchronization(@Nonnull Folder source, @Nonnull Folder destination, @Nonnull SyncParameters parameters) throws IOException {
        this.source = Objects.requireNonNull(source);
        this.destination = Objects.requireNonNull(destination);
        this.context = new Context(parameters);
    }

    /**
     * Starts the synchronization.
     */
    public void start() {
        context.taskCounter().increment();
        try {
            if (context.params().performance().fastList()) {
                doPreload();
            }
            if (context.isCancelled()) return;
            context.submit(new WalkTask(context, source, destination, null));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            context.taskCounter().decrement();
        }
    }

    private void doPreload() throws InterruptedException {
        List<CompletableFuture<Void>> futures = new LinkedList<>();
        if (source.getFileProvider().isFastListSupported()) {
            futures.add(
                context.executeAsync(new PreLoadTask(context, source)).thenAccept(folder -> source = folder)
            );
        }
        if (destination.getFileProvider().isFastListSupported()) {
            futures.add(
                context.executeAsync(new PreLoadTask(context, destination)).thenAccept(folder -> destination = folder)
            );
        }
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
        } catch (ExecutionException e) {
            throw new IllegalStateException("PANIC, should not happen",e.getCause());
        }
    }

    /**
     * Gets the statistics of the synchronization.
     * <br>This method returns the statistics of the synchronization at the time it was called.
     * It can be polled to get the progress of the synchronization.
     * @return the statistics of the synchronization
     */
    public Statistics getStatistics() {
    	return context.statistics();
    }
    
    /**
     * Cancels the synchronization.
     */
    public void cancel() {
        context.cancel();
    }

    /**
     * Returns true if the synchronization is cancelled.
     * @return true if the synchronization is cancelled
     */
    public boolean isCancelled() {
        return context.isCancelled();
    }

    /**
     * Waits for the synchronization to finish.
     * @throws InterruptedException if the current thread is interrupted
     */
    public void waitFor() throws InterruptedException {
        context.taskCounter().await();
    }

    /**
     * Gets the list of errors that occurred during the synchronization.
     * @return the list of errors (empty if no error occurred)
     */
    public List<Throwable> getErrors() {
        return context.errors();
    }

    @Override
    public void close() {
        context.close();
    }
}

