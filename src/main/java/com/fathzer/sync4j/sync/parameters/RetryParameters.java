package com.fathzer.sync4j.sync.parameters;

import java.time.Duration;
import java.util.Objects;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Parameters that control how the synchronization retries a failed operation.
 * <br>An operation is retried when its provider reports a transient error with a {@link com.fathzer.sync4j.RetryableIOException},
 * or when it fails in a way that can be a propagation delay on a provider that is not {@link com.fathzer.sync4j.FileProvider#isStronglyConsistent() strongly consistent}
 * (for instance, writing in a folder that was just created).
 * <br>The delay between two attempts grows exponentially: <code>initialDelay * multiplier^(n-1)</code> before the n-th retry, limited to {@link #maxDelay()}.
 * <br>If the provider tells how long to wait (see {@link com.fathzer.sync4j.RetryableIOException#getRetryAfter()}), this delay is used instead.
 * If it is longer than {@link #maxDelay()}, the operation is not retried and fails.
 * <p>
 * If some operations still fail after their retries, the whole synchronization can be run again (a new <i>pass</i>), see {@link #maxPasses(int)}.
 * A new pass compares source and destination again, so it only does what remains to be done.
 * </p>
 */
public class RetryParameters {
    private int maxAttempts;
    private int maxPasses;
    private Duration initialDelay;
    private double multiplier;
    private Duration maxDelay;

    /**
     * Creates a new instance of RetryParameters.
     * <br>Default values are 3 attempts per operation, 3 passes, an initial delay of one second, a multiplier of 2 and a maximum delay of 30 seconds.
     */
    public RetryParameters() {
        this.maxAttempts = 3;
        this.maxPasses = 3;
        this.initialDelay = Duration.ofSeconds(1);
        this.multiplier = 2.0;
        this.maxDelay = Duration.ofSeconds(30);
    }

    /**
     * Returns the maximum number of attempts (the first attempt is included).
     * @return the maximum number of attempts
     * @see #maxAttempts(int)
     */
    public int maxAttempts() {
        return maxAttempts;
    }

    /**
     * Sets the maximum number of attempts of an operation (the first attempt is included).
     * @param maxAttempts the maximum number of attempts. 1 means no retry. Default is 3.
     * @return this
     * @throws IllegalArgumentException if maxAttempts is less than 1
     */
    public RetryParameters maxAttempts(int maxAttempts) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be >= 1");
        }
        this.maxAttempts = maxAttempts;
        return this;
    }

    /**
     * Returns the maximum number of passes.
     * @return the maximum number of passes
     * @see #maxPasses(int)
     */
    public int maxPasses() {
        return maxPasses;
    }

    /**
     * Sets the maximum number of passes of the synchronization (the first pass is included).
     * <br>When a pass ends with errors that can be transient (an operation that still fails with a {@link com.fathzer.sync4j.RetryableIOException}
     * after its retries, or an error that can be a propagation delay on a destination that is not {@link com.fathzer.sync4j.FileProvider#isStronglyConsistent() strongly consistent}),
     * the synchronization waits (see {@link #delay(int, Duration)}, with the number of passes already done) and starts a new pass.
     * Definitive errors (authentication failure, read-only destination, etc...) never start a new pass.
     * <br>A new pass is never started if the synchronization was cancelled. Remember that, with the default error manager, any error cancels the synchronization:
     * you have to configure {@link SyncParameters#errorManager(java.util.function.BiPredicate)} to continue after an error to benefit from this feature.
     * <br>The statistics keep what was done by the previous passes, the tasks that failed are not counted again by the new pass.
     * The errors returned by {@link com.fathzer.sync4j.sync.Synchronization#getErrors()} are the errors of the last pass.
     * @param maxPasses the maximum number of passes. 1 means that the synchronization is never run again. Default is 3.
     * @return this
     * @throws IllegalArgumentException if maxPasses is less than 1
     */
    public RetryParameters maxPasses(int maxPasses) {
        if (maxPasses < 1) {
            throw new IllegalArgumentException("maxPasses must be >= 1");
        }
        this.maxPasses = maxPasses;
        return this;
    }

    /**
     * Returns the delay before the first retry.
     * @return the delay before the first retry
     * @see #initialDelay(Duration)
     */
    @Nonnull
    public Duration initialDelay() {
        return initialDelay;
    }

    /**
     * Sets the delay before the first retry.
     * @param initialDelay the delay before the first retry. Default is one second.
     * @return this
     * @throws IllegalArgumentException if initialDelay is negative
     */
    public RetryParameters initialDelay(@Nonnull Duration initialDelay) {
        if (Objects.requireNonNull(initialDelay).isNegative()) {
            throw new IllegalArgumentException("initialDelay must be >= 0");
        }
        this.initialDelay = initialDelay;
        return this;
    }

    /**
     * Returns the factor applied to the delay after each retry.
     * @return the factor
     * @see #multiplier(double)
     */
    public double multiplier() {
        return multiplier;
    }

    /**
     * Sets the factor applied to the delay after each retry.
     * @param multiplier the factor. Default is 2.
     * @return this
     * @throws IllegalArgumentException if multiplier is less than 1 (or not a number)
     */
    public RetryParameters multiplier(double multiplier) {
        if (Double.isNaN(multiplier) || multiplier < 1) {
            throw new IllegalArgumentException("multiplier must be >= 1");
        }
        this.multiplier = multiplier;
        return this;
    }

    /**
     * Returns the maximum delay between two attempts.
     * @return the maximum delay
     * @see #maxDelay(Duration)
     */
    @Nonnull
    public Duration maxDelay() {
        return maxDelay;
    }

    /**
     * Sets the maximum delay between two attempts.
     * <br>It is also the longest delay requested by a provider that the synchronization accepts to wait.
     * @param maxDelay the maximum delay. Default is 30 seconds.
     * @return this
     * @throws IllegalArgumentException if maxDelay is negative
     */
    public RetryParameters maxDelay(@Nonnull Duration maxDelay) {
        if (Objects.requireNonNull(maxDelay).isNegative()) {
            throw new IllegalArgumentException("maxDelay must be >= 0");
        }
        this.maxDelay = maxDelay;
        return this;
    }

    /**
     * Computes the delay to wait before the next attempt.
     * @param failedAttempts the number of attempts (or passes) that already failed (at least 1)
     * @param retryAfter the delay requested by the provider, null if it did not request any
     * @return the delay to wait, or null if the operation should not be retried (the provider requested a delay longer than {@link #maxDelay()}).
     */
    @Nullable
    public Duration delay(int failedAttempts, @Nullable Duration retryAfter) {
        if (retryAfter != null) {
            return retryAfter.compareTo(maxDelay) > 0 ? null : retryAfter;
        }
        double millis = initialDelay.toMillis() * Math.pow(multiplier, (double) failedAttempts - 1);
        return millis >= maxDelay.toMillis() ? maxDelay : Duration.ofMillis((long) millis);
    }
}
