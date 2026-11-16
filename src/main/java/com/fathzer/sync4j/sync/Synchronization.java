package com.fathzer.sync4j.sync;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.RetryableIOException;
import com.fathzer.sync4j.sync.parameters.RetryParameters;
import com.fathzer.sync4j.sync.parameters.SyncParameters;

import jakarta.annotation.Nonnull;

/**
 * A synchronization between a source and a destination folder.
 * <br>Please note that synchronization may produce unpredictable results if the source or destination folders are modified
 * while the synchronizer is running.
 */
public class Synchronization implements AutoCloseable {
    private final Folder rootSource;
    private final Folder rootDestination;
    // The source and destination of the current pass (they can be replaced by their preloaded version)
    private Folder source;
    private Folder destination;
    private final Context context;
    private final AtomicInteger pass = new AtomicInteger(1);

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
        this.rootSource = Objects.requireNonNull(source);
        this.rootDestination = Objects.requireNonNull(destination);
        this.source = source;
        this.destination = destination;
        this.context = Objects.requireNonNull(context);
    }

    /**
     * Starts the synchronization.
     */
    public void start() {
        pass.set(1);
        context.taskCounter().setIdleHandler(this::onPassFinished);
        runPass();
    }

    private void runPass() {
        context.taskCounter().increment();
        try {
            // Restart from the original folders (a preloaded folder is a snapshot that can be outdated)
            source = rootSource;
            destination = rootDestination;
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

    /**
     * Called when a pass is over.
     * @return true if a new pass was scheduled (the task counter is then incremented until the new pass starts)
     */
    private boolean onPassFinished() {
        final RetryParameters retry = context.params().retry();
        final int current = pass.get();
        if (current >= retry.maxPasses() || context.isCancelled()) {
            return false;
        }
        final List<Throwable> errors = context.errors();
        if (errors.stream().noneMatch(this::isWorthANewPass)) {
            return false;
        }
        final Duration retryAfter = errors.stream()
                .filter(RetryableIOException.class::isInstance)
                .map(e -> ((RetryableIOException) e).getRetryAfter())
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder()).orElse(null);
        final Duration delay = retry.delay(current, retryAfter);
        if (delay == null) {
            return false;
        }
        pass.incrementAndGet();
        context.startNewPass();
        // The synchronization is not over while it waits for the next pass
        context.taskCounter().increment();
        CompletableFuture.delayedExecutor(delay.toMillis(), TimeUnit.MILLISECONDS).execute(() -> {
            try {
                runPass();
            } finally {
                context.taskCounter().decrement();
            }
        });
        return true;
    }

    /**
     * Tests if an error that happened during a pass can be solved by a new pass.
     * @param error the error
     * @return true if it can
     */
    private boolean isWorthANewPass(Throwable error) {
        if (error instanceof RetryableIOException) {
            return true;
        }
        // These errors can be a propagation delay, but only if the destination is not strongly consistent
        final boolean maybePropagationDelay = error instanceof FileNotFoundException || error instanceof NoSuchFileException || error instanceof FileAlreadyExistsException;
        return maybePropagationDelay && !rootDestination.getFileProvider().isStronglyConsistent();
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

