package com.fathzer.sync4j.sync;

import java.io.IOException;

import com.fathzer.sync4j.sync.Event.Action;
import com.fathzer.sync4j.sync.Statistics.Counter;

abstract class Task<V, A extends Action> {
    enum Kind {
        WALKER,
        CHECKER,
        MODIFIER
    }

    protected final Context context;
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

    protected abstract V execute() throws IOException;

    protected V getDefaultValue() throws IOException {
        return null;
    }

    protected boolean skipOnDryRun() {
        return true;
    }

    protected Kind kind() {
        return Kind.MODIFIER;
    }

    protected boolean isOnlySynchronous() {
        return false;
    }
}
