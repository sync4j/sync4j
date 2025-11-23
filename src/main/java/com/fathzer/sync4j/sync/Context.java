package com.fathzer.sync4j.sync;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.File;
import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.sync.Event.Action;
import com.fathzer.sync4j.sync.Event.CopyFileAction;
import com.fathzer.sync4j.sync.parameters.SyncParameters;

import jakarta.annotation.Nonnull;

class Context implements AutoCloseable {
    static class DaemonThreadFactory implements ThreadFactory {
        private static final AtomicInteger THREAD_NUMBER = new AtomicInteger(1);
        private final String namePrefix;
        
        DaemonThreadFactory(@Nonnull String namePrefix) {
            this.namePrefix = Objects.requireNonNull(namePrefix);
        }
        
        @Override
        public Thread newThread(@Nonnull Runnable r) {
            Thread thread = new Thread(Objects.requireNonNull(r), namePrefix + "-" + THREAD_NUMBER.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }

    /**
     * A counter of tasks.
     * <br>It is used to count the number of tasks that need to be completed.
     * <br>It is used to wait for all tasks to be completed.
     */
    static class TaskCounter {
        private final AtomicLong pendingTasks = new AtomicLong(0);
        private final CountDownLatch completionLatch = new CountDownLatch(1);

        void increment() {
            pendingTasks.incrementAndGet();
        }

        void decrement() {
            if (pendingTasks.decrementAndGet() == 0) {
                completionLatch.countDown();
            }
        }

        long getPendingTasks() {
            return pendingTasks.get();
        }

        void await() throws InterruptedException {
            completionLatch.await();
        }
    }

    private final SyncParameters syncParameters;
    private final ForkJoinPool walkService;
    private final ExecutorService checkService;
    private final ExecutorService copyService;
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final Statistics statistics = new Statistics();
    private final TaskCounter taskCounter = new TaskCounter();
    private final List<Throwable> exceptions = new LinkedList<>();

    Context(SyncParameters parameters) {
        this.syncParameters = parameters;
        this.walkService = new ForkJoinPool(parameters.performance().maxWalkThreads());
        this.checkService = buildExecutorService(parameters.performance().maxComparisonThreads(), "check");
        this.copyService = buildExecutorService(parameters.performance().maxCopyThreads(), "copy");
    }

    private ExecutorService buildExecutorService(int threadCount, String prefix) {
        return threadCount > 0 ? Executors.newFixedThreadPool(threadCount, new DaemonThreadFactory(prefix)) : null;
    }

    SyncParameters params() {
        return syncParameters;
    }

    Statistics statistics() {
        return statistics;
    }

    List<Throwable> errors() {
        return exceptions;
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
            processError(e, actionSupplier.get());
        }
        return new Result<>(null, true);
    }

    void processError(Throwable e, Action action) {
        if (e instanceof CompletionException) {
            e = e.getCause();
        }
        exceptions.add(e);
        if (syncParameters.errorManager().test(e, action)) {
            cancel();
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
        tryExecute(() -> executeAsync(new CopyFileTask(this, action)), () -> action);
    }

    void asyncCheckAndCopy(File src, Folder destinationFolder, File destinationFile) {
        CompletableFuture<Boolean> areSame = executeAsync(new CompareFileTask(this, src, destinationFile));
        areSame.thenAcceptAsync(same -> {
            if (Boolean.FALSE.equals(same)) {
                asyncCopy(src, destinationFolder);
            }
        }, copyService==null?walkService:copyService);
    }

    Folder createFolder(Folder destination, String name) {
        final CreateFolderTask task = new CreateFolderTask(this, destination, name);
        final Result<Folder> result = tryExecute(() -> executeSync(task), () -> task.action);
        return result.value();
    }

    void asyncDelete(Entry entry) {
        executeAsync(new DeleteTask(this, entry));
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
        return !tryExecute(() -> executeSync(deleteTask), () -> deleteTask.action).failed();
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

    private void update(Event event, Event.Status status) {
        event.setStatus(status);
        syncParameters.eventListener().accept(event);
    }

    <V> V executeSync(Task<V, ?> task) throws IOException {
        if (isCancelled() || (params().dryRun() && (Task.Kind.MODIFIER == task.kind()))) return task.defaultValue();
        update(task.event, Event.Status.STARTED);
        try {
            V result = task.execute();
            update(task.event, Event.Status.COMPLETED);
            task.counter.done().incrementAndGet();
            return result;
        } finally {
            if (task.event.getStatus() != Event.Status.COMPLETED) {
                update(task.event, Event.Status.FAILED);
            }
        }
    }

    protected <V> CompletableFuture<V> executeAsync(Task<V, ?> task) {
        if (task.onlySynchronous()) {
            throw new UnsupportedOperationException("Task " + this + " is only synchronous");
        }
        return CompletableFuture.supplyAsync(buildAsyncSupplier(task), executorService(task))
            .exceptionally(e -> {
                processError(e, task.action);
                return null;
            }
        );
    }

    ExecutorService executorService(Task<?,?> task) {
        final ExecutorService executorService = switch (task.kind()) {
            case WALKER -> walkService;
            case CHECKER -> checkService;
            case MODIFIER -> copyService;
        };
        return executorService==null ? walkService : executorService;
    }

    /**
     * Build a supplier that can be used to execute the task asynchronously.
     * <p>
     * WARNING: The supplier will register the task in the phaser and will deregister it when the task is done.
     * So you absolutely must call supplier.get() to execute the task one time (and only one time).
     * </p>
     * @return the supplier
     */
    private <V> Supplier<V> buildAsyncSupplier(Task<V, ?> task) {
        final Supplier<V> supplier = () -> {
            try {
                return executeSync(task);
            } catch (Exception e) {
                throw new CompletionException(e);
            } finally {
                taskCounter.decrement();
            }
        };
        taskCounter.increment();
        return supplier;
    }

    Future<Void> submit(RecursiveAction action) {
        return walkService.submit(action);
    }

    TaskCounter taskCounter() {
        return taskCounter;
    }

    public void close() {
        walkService.shutdown();
        if (this.checkService != null) {
            this.checkService.shutdown();
        }
        if (this.copyService != null) {
            this.copyService.shutdown();
        }
    }
}
