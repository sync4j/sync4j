package com.fathzer.sync4j.sync;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

class RateLimiterTest {
    private static final long MS = TimeUnit.MILLISECONDS.toNanos(1);

    @Test
    void testInvalidLimit() {
        assertThrows(IllegalArgumentException.class, () -> new RateLimiter(0));
        assertThrows(IllegalArgumentException.class, () -> new RateLimiter(-1));
        assertThrows(IllegalArgumentException.class, () -> new RateLimiter(Double.NaN));
    }

    @Test
    void testOperationsAreEvenlySpaced() {
        final AtomicLong clock = new AtomicLong(1_000);
        final RateLimiter limiter = new RateLimiter(10, clock::get); // One operation every 100 ms
        assertEquals(0, limiter.reserve());
        assertEquals(100 * MS, limiter.reserve());
        assertEquals(200 * MS, limiter.reserve());

        // 250ms later, two slots are over, the third one is in 50ms
        clock.addAndGet(250 * MS);
        assertEquals(50 * MS, limiter.reserve());
    }

    @Test
    void testNoBurstAfterIdlePeriod() {
        final AtomicLong clock = new AtomicLong(0);
        final RateLimiter limiter = new RateLimiter(10, clock::get);
        assertEquals(0, limiter.reserve());
        // A long idle period does not allow to catch up
        clock.addAndGet(10_000 * MS);
        assertEquals(0, limiter.reserve());
        assertEquals(100 * MS, limiter.reserve());
    }

    @Test
    void testAcquireWaits() throws InterruptedException {
        final RateLimiter limiter = new RateLimiter(50); // One operation every 20 ms
        final long start = System.nanoTime();
        for (int i = 0; i < 6; i++) {
            limiter.acquire();
        }
        assertTrue(System.nanoTime() - start >= 90 * MS, "5 intervals of 20ms were expected");
    }

    @Test
    void testConcurrentThreadsShareTheLimit() throws InterruptedException {
        final RateLimiter limiter = new RateLimiter(100); // One operation every 10 ms
        final long start = System.nanoTime();
        final Thread[] threads = new Thread[4];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < 5; j++) {
                        limiter.acquire();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            threads[i].start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        // 20 operations in total: at least 19 intervals of 10ms (with some tolerance)
        assertTrue(System.nanoTime() - start >= 170 * MS);
    }
}
