package com.fathzer.sync4j.sync;

import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * Limits the number of operations per second, whatever the number of threads that perform them.
 * <br>Operations are evenly spaced (there is no burst): with a limit of 10 operations per second, an operation is allowed every 100ms.
 */
class RateLimiter {
    private final long intervalNanos;
    private final LongSupplier nanoClock;
    private long nextSlot;

    /**
     * Constructor.
     * @param operationsPerSecond the maximum number of operations per second (strictly positive)
     */
    RateLimiter(double operationsPerSecond) {
        this(operationsPerSecond, System::nanoTime);
    }

    RateLimiter(double operationsPerSecond, LongSupplier nanoClock) {
        if (!(operationsPerSecond > 0)) {
            throw new IllegalArgumentException("operationsPerSecond must be > 0");
        }
        this.intervalNanos = Math.max(1L, (long) (TimeUnit.SECONDS.toNanos(1) / operationsPerSecond));
        this.nanoClock = nanoClock;
        this.nextSlot = nanoClock.getAsLong();
    }

    /**
     * Reserves the next slot.
     * @return the time to wait, in nanoseconds, before performing the operation (0 if it can be performed immediately)
     */
    synchronized long reserve() {
        final long now = nanoClock.getAsLong();
        final long slot = Math.max(now, nextSlot);
        nextSlot = slot + intervalNanos;
        return slot - now;
    }

    /**
     * Waits until the operation is allowed.
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    void acquire() throws InterruptedException {
        final long wait = reserve();
        if (wait > 0) {
            TimeUnit.NANOSECONDS.sleep(wait);
        }
    }
}
