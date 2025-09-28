package com.fathzer.sync4j.sync;

import java.io.IOException;
import java.io.UncheckedIOException;

import com.fathzer.sync4j.sync.Statistics.Counter;

abstract class Task<V> implements Runnable {
    private final Context context;
    protected final Counter counter;
    
    protected Task(Context context, Counter counter) {
        this.context = context;
        this.counter = counter;
        counter.total().incrementAndGet();
    }
    
    Context context() {
        return context;
    }
    
    public final void run() {
        try {
            execute();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected abstract V execute() throws IOException;
}
