package com.fathzer.sync4j.sync;

import java.io.IOException;
import java.io.UncheckedIOException;

abstract class Task implements Runnable {
    private final Context context;
    
    protected Task(Context context) {
        this.context = context;
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

    public abstract void execute() throws IOException;
}
