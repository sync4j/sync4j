package com.fathzer.sync4j.sync;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.sync.Event.PreloadAction;
import com.fathzer.sync4j.sync.parameters.SyncParameters;

import jakarta.annotation.Nonnull;

/**
 * A synchronization between a source and a destination folder.
 * <br>Please note that synchronization may produce unpredictable results if the source or destination folders are modified
 * while the synchronizer is running.
 */
public class Synchronization implements AutoCloseable {
    private final Folders folders;
    private Context context;

    /**
     * Creates a new synchronizer.
     * @param source the source folder
     * @param destination the destination folder
     * @param parameters the parameters
     * @throws IOException if an I/O error occurs
     */
    public Synchronization(@Nonnull Folder source, @Nonnull Folder destination, @Nonnull SyncParameters parameters) throws IOException {
        this.folders = new Folders(Objects.requireNonNull(source), Objects.requireNonNull(destination));
        this.context = new Context(parameters);
    }

    /**
     * Starts the synchronization.
     */
    public void start() {
        context.taskCounter().increment();
        try {
            if (context.params().performance().fastList()) {
                doPreload(folders);
            }
            if (context.isCancelled()) return;
            context.submit(new WalkTask(context, folders.source, folders.destination, null));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            context.taskCounter().decrement();
        }
    }

    private void doPreload(Folders folders) throws InterruptedException {
        Map<Boolean, CompletableFuture<Folder>> futures = new HashMap<>();
        if (folders.source.getFileProvider().isFastListSupported()) {
            futures.put(true, context.executeAsync(new PreLoadTask(context, new PreloadAction(folders.source))));
        }
        if (folders.destination.getFileProvider().isFastListSupported()) {
            futures.put(false, context.executeAsync(new PreLoadTask(context, new PreloadAction(folders.destination))));
        }
        for (Map.Entry<Boolean, CompletableFuture<Folder>> entry : futures.entrySet()) {
            final boolean isSource = entry.getKey();
            try {
                Folder folder = entry.getValue().get();
                if (folder != null) {
                    if (isSource) {
                        folders.source = folder;
                    } else {
                        folders.destination = folder;
                    }
                }
            } catch (ExecutionException e) {
                throw new IllegalStateException("PANIC, should not happen",e.getCause()); //TODO
            }
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

    public List<Throwable> getErrors() {
        return context.errors();
    }

    @Override
    public void close() {
        context.close();
    }
}

