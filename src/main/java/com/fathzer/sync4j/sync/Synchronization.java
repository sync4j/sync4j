package com.fathzer.sync4j.sync;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

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
        this(new Context(parameters), source, destination);
    }

    Synchronization(@Nonnull Context context, @Nonnull Folder source, @Nonnull Folder destination) {
        this.source = Objects.requireNonNull(source);
        this.destination = Objects.requireNonNull(destination);
        this.context = Objects.requireNonNull(context);
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

    void doPreload() throws InterruptedException {
        List<CompletableFuture<Void>> futures = List.of(
            doPreloadAsync(source, folder -> this.source = folder), 
            doPreloadAsync(destination, folder -> this.destination = folder)
        );
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
        } catch (ExecutionException e) {
            throw new IllegalStateException("PANIC, should not happen",e.getCause());
        }
    }

    private CompletableFuture<Void> doPreloadAsync(Folder folder, Consumer<Folder> consumer) {
        if (folder.getFileProvider().isFastListSupported()) {
            PreLoadTask task = new PreLoadTask(context, folder);
            return context.executeAsync(task).thenAccept(consumer);
        }
        return CompletableFuture.completedFuture(null);
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
     * Waits for the synchronization to finish.
     * @param timeout the maximum time to wait
     * @param unit the time unit
     * @return true if the synchronization finished within the timeout, false otherwise
     * @throws InterruptedException if the current thread is interrupted
     */
    public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
        return context.taskCounter().await(timeout, unit);
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

