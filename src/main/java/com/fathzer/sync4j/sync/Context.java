package com.fathzer.sync4j.sync;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.File;
import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.sync.Event.Action;
import com.fathzer.sync4j.sync.Event.CopyFileAction;
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

    private <V, C extends Callable<V>> Result<V> tryExecute(C callable, Supplier<Action> actionSupplier) {
        try {
            return new Result<>(callable.call(), false);
        } catch (Exception e) {
            if (syncParameters.errorManager().test(e, actionSupplier.get())) {
                cancel();
            }
            return new Result<>(null, true);
        }
    }

    private record Result<V>(V value, boolean failed) {}

    /**
     * Asynchronously copies a file to a destination folder.
     * @param src the source file
     * @param destinationFolder the destination folder
     */
    void asyncCopy(File src, Folder destinationFolder) {
        CopyFileAction action = new CopyFileAction(src, destinationFolder);
        tryExecute(() -> 
            new CopyFileTask(this, action).executeAsync()
        , () -> action);
    }

    void asyncCheckAndCopy(File src, Folder destinationFolder, File destinationFile) {
        CompletableFuture<Boolean> areSame = new CompareFileTask(this, src, destinationFile).executeAsync();
        areSame.thenAcceptAsync(same -> {
            if (Boolean.FALSE.equals(same)) {
                asyncCopy(src, destinationFolder);
            }
        }, copyService);
    }

    Folder createFolder(Folder destination, String name) {
        final CreateFolderTask task = new CreateFolderTask(this, destination, name);
        final Result<Folder> result = tryExecute(task::executeSync, () -> task.action);
        return result.value();
    }

    void asyncDelete(Entry entry) {
        new DeleteTask(this, entry).executeAsync();
    }

    void deleteThenAsyncCopy(Folder toBeDeleted, Folder toBeDeleteParent, File source) {
        if (delete(toBeDeleted)) {
            asyncCopy(source, toBeDeleteParent);
        }
    }

    Folder deleteThenCreate(File toBeDeleted, Folder toBeDeleteParent, Folder source) {
        return delete(toBeDeleted) ? createFolder(toBeDeleteParent, source.getName()) : null;
    }

    private boolean delete(Entry toBeDeleted) {
        final DeleteTask deleteTask = new DeleteTask(this, toBeDeleted);
        return !tryExecute(deleteTask::executeSync, () -> deleteTask.action).failed();
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
