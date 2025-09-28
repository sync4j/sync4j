package com.fathzer.sync4j.sync;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

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
    private final ExecutorService checkService;
    private final ExecutorService copyService;
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final Phaser phaser = new Phaser();
    private final Statistics statistics = new Statistics();

    private final Consumer<Event> listener = System.out::println; //TODO

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

    void doCheck(File src, Folder destinationFolder, File destinationFile) {
        submit(checkService, new CompareFileTask(this, src, destinationFolder, destinationFile));
    }

    Folder createFolder(Folder destination, String name) throws IOException {
        return new CreateFolderTask(this, destination, name).execute();
    }

    void doCopy(File src, Folder destinationFolder) throws IOException {
        submit(copyService, new CopyFileTask(this, src, destinationFolder));
    }

    void doDelete(Entry entry, boolean background) throws IOException {
        if (background) {
            submit(copyService, new DeleteTask(this, entry));
        } else {
            new DeleteTask(this, entry).execute();
        }
    }

    void doDeleteThenCopy(Folder toBeDeleted, Folder toBeDeleteParent, File source) throws IOException {
        submit(copyService, new DeleteThenCopyTask(this, toBeDeleted, source, toBeDeleteParent));
    }

    void cancel() {
        cancelled.set(true);
    }

    boolean isCancelled() {
        return cancelled.get();
    }

    private void submit(ExecutorService executor, Task<?> task) {
        phaser.register();
        executor.execute(() -> {
            try {
                if (!cancelled.get()) {
                    task.run();
                }
            } finally {
                try {
                    phaser.arriveAndDeregister();
                } catch (IllegalStateException e) {
                    System.out.println("Exception while deregistering from phaser task: " + task);
                    e.printStackTrace();
                }
                System.out.println("Thread " + Thread.currentThread().getName() + ": It remains " + phaser.getUnarrivedParties() + " tasks");
            }
        });
    }

    void waitFor() {
        System.out.println("Waiting for " + phaser.getUnarrivedParties() + " tasks to finish");
        phaser.awaitAdvance(phaser.getPhase());
        System.out.println("All tasks finished");
    }

    public void close() {
        this.checkService.shutdown();
        this.copyService.shutdown();
    }
}
