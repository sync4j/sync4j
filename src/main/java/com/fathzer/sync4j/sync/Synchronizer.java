package com.fathzer.sync4j.sync;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.sync.Event.PreloadAction;
import com.fathzer.sync4j.sync.parameters.SyncParameters;

import jakarta.annotation.Nonnull;

public class Synchronizer implements AutoCloseable {
    private final Folders folders;
    private Context context;
    private Future<?> walkTask;

    public Synchronizer(@Nonnull Folder source, @Nonnull Folder destination, @Nonnull SyncParameters parameters) throws IOException {
        this.folders = new Folders(Objects.requireNonNull(source), Objects.requireNonNull(destination));
        this.context = new Context(parameters);
    }

    public void start() {
        try (ForkJoinPool walkService = new ForkJoinPool(4)) {
            if (context.params().performance().fastList()) {
                doPreload(folders, walkService);
            }
            if (context.isCancelled()) return;
            walkTask = walkService.submit(new WalkTask(context, folders.source, folders.destination, null));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public Statistics getStatistics() {
    	return context.statistics();
    }
    
    public void cancel() {
        context.cancel();
    }

    public boolean isCancelled() {
        return context.isCancelled();
    }

    public void waitFor() throws ExecutionException, InterruptedException {
        if (walkTask != null) {
            walkTask.get();
            context.waitFor();
        }
    }

    private void doPreload(Folders folders, ExecutorService preloadService) throws InterruptedException {
        Map<Boolean, CompletableFuture<Folder>> futures = new HashMap<>();
        if (folders.source.getFileProvider().isFastListSupported()) {
            futures.put(true, new PreLoadTask(context, new PreloadAction(folders.source), preloadService).executeAsync());
        }
        if (folders.destination.getFileProvider().isFastListSupported()) {
            futures.put(false, new PreLoadTask(context, new PreloadAction(folders.destination), preloadService).executeAsync());
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

    @Override
    public void close() {
        context.close();
    }
}

