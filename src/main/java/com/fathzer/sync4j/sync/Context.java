package com.fathzer.sync4j.sync;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.fathzer.sync4j.Event;
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

    private final Consumer<Event> listener = System.out::println; //TODO

    Context(SyncParameters parameters) {
        this.syncParameters = parameters;
        this.checkService = Executors.newFixedThreadPool(parameters.performance().maxComparisonThreads(), new DaemonThreadFactory("check"));
        this.copyService = Executors.newFixedThreadPool(parameters.performance().maxCopyThreads(), new DaemonThreadFactory("copy"));
    }

    SyncParameters params() {
        return syncParameters;
    }

    void cancel() {
        cancelled.set(true);
    }

    boolean isCancelled() {
        return cancelled.get();
    }

    void doCheckTask(Task task) {
        submit(checkService, task);
    }

    void doCopyTask(Task task) {
        submit(copyService, task);
    }

    private void submit(ExecutorService executor, Task task) {
        phaser.register();
        executor.execute(() -> {
            try {
                if (!cancelled.get()) {
                    task.run();
                }
            } finally {
                phaser.arriveAndDeregister();
                System.out.println("Thread " + Thread.currentThread().getName() + ": It remains " + phaser.getUnarrivedParties() + " tasks");
            }
        });
    }

    void waitFor() {
        System.out.println("Waiting for " + phaser.getUnarrivedParties() + " tasks to finish");
        phaser.arriveAndAwaitAdvance();
        System.out.println("All tasks finished");
    }

    public void close() {
        this.checkService.shutdown();
        this.copyService.shutdown();
    }
}
