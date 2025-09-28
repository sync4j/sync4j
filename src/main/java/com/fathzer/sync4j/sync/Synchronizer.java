package com.fathzer.sync4j.sync;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

import com.fathzer.sync4j.Folder;

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

    public void start() throws IOException {
        try (ForkJoinPool walkService = new ForkJoinPool(4)) {
            System.out.println("Syncing " + folders.source + " -> " + folders.destination);
            if (context.params().performance().fastList()) {
                doPreload(folders, walkService);
            }
            walkTask = walkService.submit(new WalkTask(context, folders.source, folders.destination, null));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            throw new IOException(e.getCause());
        }
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

    private void doPreload(Folders folders, ForkJoinPool walkService) throws ExecutionException, InterruptedException {
        List<Future<?>> futures = new LinkedList<>();
        if (folders.source.getFileProvider().isFastListSupported()) {
            futures.add(walkService.submit(new PreloadTask(context, folders, true)));
        }
        if (folders.destination.getFileProvider().isFastListSupported()) {
            futures.add(walkService.submit(new PreloadTask(context, folders, false)));
        }
        for (Future<?> future : futures) {
            future.get();
        }
    }

    @Override
    public void close() {
        context.close();
    }
}

