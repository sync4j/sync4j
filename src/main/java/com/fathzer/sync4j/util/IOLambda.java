package com.fathzer.sync4j.util;

import java.io.IOException;

/**
 * Utility class for lambda expressions that throw IOException.
 */
public final class IOLambda {
    private IOLambda() {}
    
    @FunctionalInterface
    /**
     * Runnable that throws IOException.
     */
    public interface IORunnable {
        void run() throws IOException;
    }

    @FunctionalInterface
    /**
     * Supplier that throws IOException.
     */
    public interface IOSupplier<T> {
        T get() throws IOException;
    }
}
