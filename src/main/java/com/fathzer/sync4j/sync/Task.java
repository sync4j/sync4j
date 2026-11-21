package com.fathzer.sync4j.sync;

import static com.fathzer.sync4j.sync.Event.Status.*;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.time.Duration;
import java.util.Objects;

import com.fathzer.sync4j.RetryableIOException;
import com.fathzer.sync4j.sync.Event.Action;
import com.fathzer.sync4j.sync.Statistics.Counter;
import com.fathzer.sync4j.sync.parameters.RetryParameters;

import jakarta.annotation.Nonnull;

abstract class Task<V, A extends Action> {
    enum Kind {
        WALKER,
        CHECKER,
        MODIFIER
    }

    @Nonnull
    private final Context context;
    @Nonnull
    private final A action;
    @Nonnull
    private Event.Status status;
    @Nonnull
    private final Counter counter;
    
    @SuppressWarnings("java:S2637")
    protected Task(@Nonnull Context context, @Nonnull A action, @Nonnull Counter counter) {
        this.context = Objects.requireNonNull(context, "Context cannot be null");
        this.action = Objects.requireNonNull(action, "Action cannot be null");
        broadcast(PLANNED);
        this.counter = Objects.requireNonNull(counter, "Counter cannot be null");
        counter.total().incrementAndGet();
    }

    protected Context context() {
        return context;
    }

    protected A action() {
        return action;
    }

    private void broadcast(@Nonnull Event.Status status) {
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        context.params().eventListener().accept(new Event(action, status));
    }

    protected abstract V execute() throws IOException;

    protected V defaultValue() throws IOException {
        return null;
    }

    protected Kind kind() {
        return Kind.MODIFIER;
    }

    protected boolean onlySynchronous() {
        return false;
    }

    /**
     * Tests if an error can be a propagation delay on an eventually consistent provider.
     * <br>The provider can't tell if it is the case, only the task knows enough about its context.
     * It is called only if the exception is not a {@link RetryableIOException}.
     * @param e the exception thrown by {@link #execute()}
     * @return true if the task should be retried. The default implementation returns false.
     */
    protected boolean isPropagationDelay(@Nonnull IOException e) {
        return false;
    }

    /**
     * Restores the state that could have been altered by a failed attempt of {@link #execute()}.
     * <br>It is called before a new attempt, and when the task finally fails.
     * <br>The default implementation does nothing.
     */
    protected void rollback() {
        // Does nothing by default
    }

    private V executeWithRetry() throws IOException {
        final RetryParameters retry = context.params().retry();
        int attempt = 1;
        while (true) {
            try {
                throttle();
                return execute();
            } catch (IOException e) {
                if (attempt >= retry.maxAttempts() || context.isCancelled() || !isRetryable(e)) {
                    throw e;
                }
                final Duration retryAfter = e instanceof RetryableIOException r ? r.getRetryAfter() : null;
                final Duration delay = retry.delay(attempt, retryAfter);
                if (delay == null) {
                    throw e;
                }
                sleep(delay, e);
                rollback();
                attempt++;
            }
        }
    }

    private void throttle() throws IOException {
        try {
            context.throttle();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException("Interrupted while waiting for the rate limit");
        }
    }

    private boolean isRetryable(IOException e) {
        return e instanceof RetryableIOException || isPropagationDelay(e);
    }

    private static void sleep(Duration delay, IOException cause) throws IOException {
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            cause.addSuppressed(ie);
            throw cause;
        }
    }

    final V call() throws IOException {
        try {
            broadcast(STARTED);
            V result = executeWithRetry();
            counter.done().incrementAndGet();
            broadcast(COMPLETED);
            return result;
        } finally {
            if (status != COMPLETED) {
                rollback();
                broadcast(FAILED);
            }
        }
    }
}
