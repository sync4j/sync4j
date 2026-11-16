package com.fathzer.sync4j;

import java.io.IOException;
import java.time.Duration;

import jakarta.annotation.Nullable;

/**
 * An {@link IOException} that tells the caller that the failed operation may succeed if it is attempted again later.
 * <br>It is thrown by a {@link FileProvider} when it recognizes a transient condition: throttling (HTTP 429), timeout,
 * temporary server error (HTTP 5xx), network glitch, etc...
 * <br>The provider does not retry by itself: it is the caller (typically the synchronization) that decides if, when and how many times it tries again,
 * using {@link #getRetryAfter()} as the minimum delay when it is available.
 * <br>Errors that can't be solved by retrying (invalid request, authentication or authorization failure, quota exceeded, read-only provider, invalid name, etc...)
 * must <b>not</b> be reported with this exception.
 * <p>
 * See the contract in {@link FileProvider}.
 * </p>
 */
public class RetryableIOException extends IOException {
    private static final long serialVersionUID = 1L;

    private final transient Duration retryAfter;

    /**
     * Constructor.
     * @param message the message
     */
    public RetryableIOException(String message) {
        this(message, null, null);
    }

    /**
     * Constructor.
     * @param message the message
     * @param cause the cause (can be null)
     */
    public RetryableIOException(String message, @Nullable Throwable cause) {
        this(message, cause, null);
    }

    /**
     * Constructor.
     * @param message the message
     * @param cause the cause (can be null)
     * @param retryAfter the minimum delay to wait before trying again, typically the value of the Retry-After header sent by a server.
     * <br>Null if the provider has no suggestion.
     * @throws IllegalArgumentException if retryAfter is negative
     */
    public RetryableIOException(String message, @Nullable Throwable cause, @Nullable Duration retryAfter) {
        super(message, cause);
        if (retryAfter != null && retryAfter.isNegative()) {
            throw new IllegalArgumentException("retryAfter can't be negative");
        }
        this.retryAfter = retryAfter;
    }

    /**
     * Gets the minimum delay to wait before trying the operation again.
     * @return the delay suggested by the provider, or null if it has no suggestion (the caller uses its own backoff policy)
     */
    @Nullable
    public Duration getRetryAfter() {
        return retryAfter;
    }
}
