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

    /**
     * Function that throws IOException.
     * 
     * @param <T> the type of the input to the function
     * @param <R> the type of the result of the function
     */
    @FunctionalInterface
    public interface IOFunction<T, R> {
        /**
         * Applies the function to the input.
         * @param t the input
         * @return the result
         * @throws IOException if an I/O error occurs
         */
        R apply(T t) throws IOException;
    }

    /**
     * Consumer that throws IOException.
     * 
     * @param <T> the type of the input to the consumer
     */
    @FunctionalInterface
    public interface IOConsumer<T> {
        /**
         * Accepts the input.
         * @param t the input
         * @throws IOException if an I/O error occurs
         */
        void accept(T t) throws IOException;
    }

    /**
     * Predicate that throws IOException.
     * 
     * @param <T> the type of the input to the predicate
     */
    @FunctionalInterface
    public interface IOPredicate<T> {
        /**
         * Tests the input.
         * @param t the input
         * @return true if the input matches the predicate
         * @throws IOException if an I/O error occurs
         */
        boolean test(T t) throws IOException;
    }
}
