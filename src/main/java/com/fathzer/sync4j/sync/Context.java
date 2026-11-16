package com.fathzer.sync4j.sync;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
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
        private BiConsumer<TaskCounter, Boolean> eventConsumer = (tc, add) -> {};
        private volatile BooleanSupplier idleHandler = () -> false;

        /**
         * Sets the handler called when there is no more pending tasks.
         * <br>It can start a new pass: it must then increment this counter before returning true.
         * The completion latch is released only if it returns false.
         * @param idleHandler the handler, that returns true if a new pass was started.
         */
        void setIdleHandler(BooleanSupplier idleHandler) {
            this.idleHandler = idleHandler;
        }

        void increment() {
            pendingTasks.incrementAndGet();
            eventConsumer.accept(this, true);
        }

        void decrement() {
            if (pendingTasks.decrementAndGet() == 0) {
                boolean newPassStarted = false;
                try {
                    newPassStarted = idleHandler.getAsBoolean();
                } finally {
                    if (!newPassStarted) {
                        completionLatch.countDown();
                    }
                }
            }
            eventConsumer.accept(this, false);
        }

        void setEventConsumer(BiConsumer<TaskCounter, Boolean> eventConsumer) {
            this.eventConsumer = eventConsumer;
        }

        long getPendingTasks() {
            return pendingTasks.get();
        }

        void await() throws InterruptedException {
            completionLatch.await();
        }

        boolean await(long timeout, TimeUnit unit) throws InterruptedException {
            return completionLatch.await(timeout, unit);
        }
    }

    private final SyncParameters syncParameters;
    private final ForkJoinPool walkService;
    private final ExecutorService checkService;
    private final ExecutorService copyService;
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final Statistics statistics = new Statistics();
    private final TaskCounter taskCounter = new TaskCounter();
    private final RateLimiter rateLimiter;
    private final List<Throwable> exceptions = new LinkedList<>();
    /** The destination folders created by this synchronization. */
    private final Set<Folder> createdFolders = ConcurrentHashMap.newKeySet();

    Context(SyncParameters parameters) {
        this.syncParameters = parameters;
        this.walkService = new ForkJoinPool(parameters.performance().maxWalkThreads());
        this.checkService = buildExecutorService(parameters.performance().maxComparisonThreads(), "check");
        this.copyService = buildExecutorService(parameters.performance().maxCopyThreads(), "copy");
        final double maxOperationsPerSecond = parameters.performance().maxOperationsPerSecond();
        this.rateLimiter = maxOperationsPerSecond > 0 ? new RateLimiter(maxOperationsPerSecond) : null;
    }

    /**
     * Waits, if needed, until the next operation on a provider is allowed by the rate limit.
     * <br>It does nothing if there is no limit.
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    void throttle() throws InterruptedException {
        if (rateLimiter != null) {
            rateLimiter.acquire();
        }
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

    /**
     * Resets what only makes sense inside a pass (errors, folders created by this pass).
     * <br>The statistics keep what was done, but forget the tasks that failed (see {@link Statistics#startNewPass()}).
     */
    void startNewPass() {
        exceptions.clear();
        createdFolders.clear();
        statistics.startNewPass();
    }

    void skip(Entry entry) {
        if (entry.isFile()) {
            statistics.skippedFiles().incrementAndGet();
        } else {
            statistics.skippedFolders().incrementAndGet();
        }
    }

    <V, C extends Callable<V>> Result<V> tryExecute(C callable, Supplier<Action> actionSupplier) {
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

    record Result<V>(V value, boolean failed) {}

    /**
     * Asynchronously copies a file to a destination folder.
     * @param src the source file
     * @param destinationFolder the destination folder
     */
    void asyncCopy(File src, Folder destinationFolder) {
        asyncCopy(src, destinationFolder, false);
    }

    private void asyncCopy(File src, Folder destinationFolder, boolean afterDelete) {
        CopyFileAction action = new CopyFileAction(src, destinationFolder);
        tryExecute(() -> executeAsync(new CopyFileTask(this, action, afterDelete)), () -> action);
    }

    /**
     * Tests if an error can be a propagation delay on an eventually consistent destination.
     * <br>It is the case if the provider is not strongly consistent and
     * <ul>
     * <li>the error says an entry does not exist, and the destination folder was created by this synchronization</li>
     * <li>or the error says an entry already exists, and the synchronization has just deleted something with this name</li>
     * </ul>
     * @param e the error
     * @param destinationFolder the folder where the failed operation was performed
     * @param afterDelete true if the failed operation follows the deletion of an entry
     * @return true if the operation is worth to be retried
     */
    boolean isPropagationDelay(IOException e, Folder destinationFolder, boolean afterDelete) {
        if (destinationFolder.getFileProvider().isStronglyConsistent()) {
            return false;
        }
        if (e instanceof FileNotFoundException || e instanceof NoSuchFileException) {
            return createdFolders.contains(destinationFolder);
        }
        return afterDelete && e instanceof FileAlreadyExistsException;
    }

    void asyncCheckAndCopy(File src, File destinationFile) {
        executeAsync(new CompareFileTask(this, src, destinationFile, () -> asyncCopy(src, destinationFile.getParent().asFolder())));
    }

    Folder createFolder(Folder destination, String name) {
        return createFolder(destination, name, false);
    }

    private Folder createFolder(Folder destination, String name, boolean afterDelete) {
        final CreateFolderTask task = new CreateFolderTask(this, destination, name, afterDelete);
        final Result<Folder> result = tryExecute(() -> executeSync(task), task::action);
        final Folder created = result.value();
        if (created != null && !params().dryRun()) {
            createdFolders.add(created);
        }
        return created;
    }

    void asyncDelete(Entry entry) {
        executeAsync(new DeleteTask(this, entry));
    }

    void deleteThenAsyncCopy(Folder toBeDeleted, Folder toBeDeleteParent, File source) {
        if (delete(toBeDeleted)) {
            asyncCopy(source, toBeDeleteParent, true);
        }
    }

    Folder deleteThenCreate(File toBeDeleted, Folder toBeDeleteParent, Folder source) {
        return delete(toBeDeleted) ? createFolder(toBeDeleteParent, source.getName(), true) : null;
    }

    private boolean delete(Entry toBeDeleted) {
        final DeleteTask deleteTask = new DeleteTask(this, toBeDeleted);
        return !tryExecute(() -> executeSync(deleteTask), deleteTask::action).failed();
    }

    void cancel() {
        cancelled.set(true);
    }

    boolean isCancelled() {
        return cancelled.get();
    }

    <V> V executeSync(Task<V, ?> task) throws IOException {
        if (isCancelled() || (params().dryRun() && (Task.Kind.MODIFIER == task.kind()))) return task.defaultValue();
        return task.call();
    }

    protected <V> CompletableFuture<V> executeAsync(Task<V, ?> task) {
        if (task.onlySynchronous()) {
            throw new UnsupportedOperationException("Task " + this + " is only synchronous");
        }
        // The task counter is decremented after the error processing: the synchronization can't be considered as finished
        // (or ask for a new pass) while the error of a task is not yet recorded.
        taskCounter.increment();
        try {
            return CompletableFuture.supplyAsync(() -> execute(task), executorService(task))
                .exceptionally(e -> {
                    processError(e, task.action());
                    return null;
                })
                .whenComplete((result, e) -> taskCounter.decrement());
        } catch (RuntimeException e) {
            // The task was not submitted (the executor service rejected it)
            taskCounter.decrement();
            throw e;
        }
    }

    private <V> V execute(Task<V, ?> task) {
        try {
            return executeSync(task);
        } catch (Exception e) {
            throw new CompletionException(e);
        }
    }

    ExecutorService executorService(Task<?,?> task) {
        final ExecutorService executorService = switch (task.kind()) {
            case WALKER -> walkService;
            case CHECKER -> checkService;
            case MODIFIER -> copyService;
        };
        return executorService == null ? walkService : executorService;
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
