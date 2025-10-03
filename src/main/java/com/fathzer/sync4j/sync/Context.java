package com.fathzer.sync4j.sync;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.File;
import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.sync.parameters.SyncParameters;

class Context implements AutoCloseable {
    private static class DaemonThreadFactory implements ThreadFactory {
        private static final AtomicInteger THREAD_NUMBER = new AtomicInteger(1);
        private final String namePrefix;
        
        DaemonThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }
        
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, namePrefix + "-" + THREAD_NUMBER.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }

    private final SyncParameters syncParameters;
    final ExecutorService checkService;
    final ExecutorService copyService;
    private final AtomicBoolean cancelled = new AtomicBoolean();
    final Phaser phaser = new Phaser();
    private final Statistics statistics = new Statistics();

    Context(SyncParameters parameters) {
        this.syncParameters = parameters;
        this.checkService = Executors.newFixedThreadPool(parameters.performance().maxComparisonThreads(), new DaemonThreadFactory("check"));
        this.copyService = Executors.newFixedThreadPool(parameters.performance().maxCopyThreads(), new DaemonThreadFactory("copy"));
    }

    SyncParameters params() {
        return syncParameters;
    }

    Statistics statistics() {
        return statistics;
    }

    void skip(Entry entry) {
        if (entry.isFile()) {
            statistics.skippedFiles().incrementAndGet();
        } else {
            statistics.skippedFolders().incrementAndGet();
        }
    }

    void asyncCheckAndCopy(File src, Folder destinationFolder, File destinationFile) {
        CompletableFuture<Boolean> areSame = new CompareFileTask(this, src, destinationFile).executeAsync();
        areSame.thenAcceptAsync(same -> {
            if (Boolean.FALSE.equals(same)) {
                try {
                    new CopyFileTask(this, src, destinationFolder).buildAsyncSupplier().get();
                } catch (IOException e) {
                    throw new CompletionException(e);
                }
            }
        }, copyService);
    }

    Folder createFolder(Folder destination, String name) {
        return new CreateFolderTask(this, destination, name).executeSync();
    }

    void asyncCopy(File src, Folder destinationFolder) {
        new CopyFileTask(this, src, destinationFolder).executeAsync();
    }

    void asyncDelete(Entry entry) {
        new DeleteTask(this, entry).executeAsync();
    }

    void deleteThenAsyncCopy(Folder toBeDeleted, Folder toBeDeleteParent, File source) {
        new DeleteTask(this, toBeDeleted).executeSync();
        new CopyFileTask(this, source, toBeDeleteParent).executeAsync();
    }

    Folder deleteThenCreate(File toBeDeleted, Folder toBeDeleteParent, Folder source) {
        new DeleteTask(this, toBeDeleted).executeSync();
        return createFolder(toBeDeleteParent, source.getName());
    }

    void cancel() {
        cancelled.set(true);
    }

    boolean isCancelled() {
        return cancelled.get();
    }

    Event createEvent(Event.Action action) {
        final Event event = new Event(action);
        syncParameters.eventListener().accept(event);
        return event;
    }

    void update(Event event, Event.Status status) {
        event.setStatus(status);
        syncParameters.eventListener().accept(event);
    }

    void waitFor() {
        phaser.awaitAdvance(phaser.getPhase());
    }

    public void close() {
        this.checkService.shutdown();
        this.copyService.shutdown();
    }
}
