package com.fathzer.sync4j.sync;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import com.fathzer.sync4j.sync.Event.Action;
import com.fathzer.sync4j.sync.Statistics.Counter;

abstract class Task<V, A extends Action> {
    private final Context context;
    protected final A action;
    protected final Event event;
    protected final Counter counter;
    
    protected Task(Context context, A action, Counter counter) {
        this.context = context;
        this.action = action;
        this.event = context.createEvent(action);
        this.counter = counter;
        counter.total().incrementAndGet();
    }
    
    Context context() {
        return context;
    }

    protected abstract V execute() throws IOException;

    protected V getDefaultValue() throws IOException {
        return null;
    }

    protected boolean skipOnDryRun() {
        return true;
    }

    //TODO Remove this method
    private static final Map<Class<?>, String> THREAD_PREFIX = Map.of(
        CopyFileTask.class, "copy",
        CompareFileTask.class, "check"
    );
    private void checkThread() {
        final String name = Thread.currentThread().getName();
        final String expectedPrefix = THREAD_PREFIX.get(this.getClass());
        if (expectedPrefix != null) {
            if (!name.startsWith(expectedPrefix)) {
                System.err.println("!!! Task " + this + " should be executed on " + expectedPrefix + " thread instead of " + name);
            }
//        } else {
//System.out.println("Starting " + action + " on " + Thread.currentThread().getName());
        }
    }

    final V executeSync() throws IOException {
        checkThread();
        if (context.isCancelled() || (context().params().dryRun() && skipOnDryRun())) return getDefaultValue();
        context.update(event, Event.Status.STARTED);
        try {
            V result = execute();
            context.update(event, Event.Status.COMPLETED);
            counter.done().incrementAndGet();
            return result;
        } finally {
            if (event.getStatus() != Event.Status.COMPLETED) {
                context.update(event, Event.Status.FAILED);
            }
        }
    }


    protected CompletableFuture<V> executeAsync() {
        return CompletableFuture.supplyAsync(buildAsyncSupplier(), executorService())
            .exceptionally(e -> {
                e.printStackTrace(); //TODO
                return null;
            }
        );
    }

    /**
     * Build a supplier that can be used to execute the task asynchronously.
     * <p>
     * WARNING: The supplier will register the task in the phaser and will deregister it when the task is done.
     * So you absolutely must call supplier.get() to execute the task one time (and only one time).
     * </p>
     * @return the supplier
     */
    protected Supplier<V> buildAsyncSupplier() {
        final Supplier<V> supplier = () -> {
            try {
                return executeSync();
            } catch (Exception e) {
                throw new CompletionException(e);
            } finally {
                context.phaser.arriveAndDeregister();
            }
        };
        context.phaser.register();
        return supplier;
    }

    protected ExecutorService executorService() {
        return context.copyService;
    }
}
