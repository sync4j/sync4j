package com.fathzer.sync4j.sync;

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
    protected final Context context;
    @Nonnull
    protected final A action;
    @Nonnull
    protected final Event event;
    @Nonnull
    protected final Counter counter;
    
    protected Task(@Nonnull Context context, @Nonnull A action, @Nonnull Counter counter) {
        this.context = Objects.requireNonNull(context, "Context cannot be null");
        this.action = Objects.requireNonNull(action, "Action cannot be null");
        this.event = Objects.requireNonNull(context.createEvent(action), "Event cannot be null");
        this.counter = Objects.requireNonNull(counter, "Counter cannot be null");
        counter.total().incrementAndGet();
    }

    protected abstract V execute() throws IOException;

    protected V defaultValue() throws IOException {
        return null;
    }
/*
    protected final boolean skipOnDryRun() {
        return Kind.MODIFIER == kind();
    }
*/

    protected Kind kind() {
        return Kind.MODIFIER;
    }

    protected boolean onlySynchronous() {
        return false;
    }
}
