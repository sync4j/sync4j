package com.fathzer.sync4j.sync;

import static com.fathzer.sync4j.sync.Event.Status.*;

import java.io.IOException;
import java.util.Objects;

import com.fathzer.sync4j.sync.Event.Action;
import com.fathzer.sync4j.sync.Statistics.Counter;

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
    private final Event event;
    @Nonnull
    private final Counter counter;
    
    protected Task(@Nonnull Context context, @Nonnull A action, @Nonnull Counter counter) {
        this.context = Objects.requireNonNull(context, "Context cannot be null");
        this.action = Objects.requireNonNull(action, "Action cannot be null");
        this.event = new Event(action);
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
        event.setStatus(Objects.requireNonNull(status, "Status cannot be null"));
        context.params().eventListener().accept(event);
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

    final V call() throws IOException {
        try {
            broadcast(STARTED);
            V result = execute();
            counter.done().incrementAndGet();
            broadcast(COMPLETED);
            return result;
        } finally {
            if (event.getStatus() != COMPLETED) {
                broadcast(FAILED);
            }
        }
    }
}
