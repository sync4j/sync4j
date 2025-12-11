package com.fathzer.sync4j.util;

import java.io.IOException;

/**
 * Utility class for lambda expressions that throw IOException.
 */
public final class IOLambda {
    private IOLambda() {}
    
    /**
     * Runnable that throws IOException.
     */
    @FunctionalInterface
    public interface IORunnable {
        /**
         * Runs the runnable.
         * @throws IOException if an I/O error occurs
         */
        void run() throws IOException;
    }

    /**
     * Supplier that throws IOException.
     * 
     * @param <T> the type of the result
     */
    @FunctionalInterface
    public interface IOSupplier<T> {
        /**
         * Gets the result.
         * @return the result
         * @throws IOException if an I/O error occurs
         */
        T get() throws IOException;
    }
}
