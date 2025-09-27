package com.fathzer.sync4j.sync;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadFactory;
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
            thread.setName(namePrefix);
            return thread;
        }
    }

    private final SyncParameters syncParameters;
    private final ForkJoinPool walkService;
    private final ExecutorService checkService;
    private final ExecutorService copyService;

    private final Consumer<Event> listener = System.out::println; //TODO


    Context(SyncParameters parameters) {
        this.syncParameters = parameters;
        this.walkService = new ForkJoinPool(4);
        this.checkService = Executors.newFixedThreadPool(parameters.performance().maxComparisonThreads(), new DaemonThreadFactory("tasks"));
        this.copyService = Executors.newFixedThreadPool(parameters.performance().maxCopyThreads(), new DaemonThreadFactory("copy"));
    }

    ForkJoinPool walkService() {
        return walkService;
    }

    SyncParameters params() {
        return syncParameters;
    }

    void doCheckTask(Task task) {
        checkService.execute(task);
    }

    void doCopyTask(Task task) {
        copyService.execute(task);
    }

    public void close() {
        this.walkService.shutdown();
        this.checkService.shutdown();
        this.copyService.shutdown();
    }
}
