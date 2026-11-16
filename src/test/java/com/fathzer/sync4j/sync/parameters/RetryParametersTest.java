package com.fathzer.sync4j.sync.parameters;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class RetryParametersTest {
    @Test
    void testDefaultValues() {
        RetryParameters params = new RetryParameters();
        assertEquals(3, params.maxAttempts());
        assertEquals(3, params.maxPasses());
        assertEquals(Duration.ofSeconds(1), params.initialDelay());
        assertEquals(2.0, params.multiplier());
        assertEquals(Duration.ofSeconds(30), params.maxDelay());
    }

    @Test
    void testSetters() {
        RetryParameters params = new RetryParameters();
        assertSame(params, params.maxAttempts(5).maxPasses(2).initialDelay(Duration.ofMillis(10)).multiplier(3).maxDelay(Duration.ofSeconds(2)));
        assertEquals(5, params.maxAttempts());
        assertEquals(2, params.maxPasses());
        assertEquals(Duration.ofMillis(10), params.initialDelay());
        assertEquals(3.0, params.multiplier());
        assertEquals(Duration.ofSeconds(2), params.maxDelay());
    }

    @Test
    void testInvalidValues() {
        RetryParameters params = new RetryParameters();
        assertThrows(IllegalArgumentException.class, () -> params.maxAttempts(0));
        assertThrows(IllegalArgumentException.class, () -> params.maxPasses(0));
        assertThrows(IllegalArgumentException.class, () -> params.initialDelay(Duration.ofMillis(-1)));
        assertThrows(IllegalArgumentException.class, () -> params.multiplier(0.5));
        assertThrows(IllegalArgumentException.class, () -> params.multiplier(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> params.maxDelay(Duration.ofMillis(-1)));
        assertThrows(NullPointerException.class, () -> params.initialDelay(null));
        assertThrows(NullPointerException.class, () -> params.maxDelay(null));
    }

    @Test
    void testExponentialDelay() {
        RetryParameters params = new RetryParameters().initialDelay(Duration.ofSeconds(1)).multiplier(2).maxDelay(Duration.ofSeconds(5));
        assertEquals(Duration.ofSeconds(1), params.delay(1, null));
        assertEquals(Duration.ofSeconds(2), params.delay(2, null));
        assertEquals(Duration.ofSeconds(4), params.delay(3, null));
        // Limited to the max delay
        assertEquals(Duration.ofSeconds(5), params.delay(4, null));
        assertEquals(Duration.ofSeconds(5), params.delay(1000, null));
    }

    @Test
    void testRetryAfter() {
        RetryParameters params = new RetryParameters().initialDelay(Duration.ofSeconds(1)).maxDelay(Duration.ofSeconds(5));
        // The delay requested by the provider has priority
        assertEquals(Duration.ofMillis(10), params.delay(3, Duration.ofMillis(10)));
        assertEquals(Duration.ofSeconds(5), params.delay(1, Duration.ofSeconds(5)));
        // ... unless it is too long: do not retry
        assertNull(params.delay(1, Duration.ofSeconds(6)));
    }

    @Test
    void testSyncParametersHasRetry() {
        SyncParameters params = new SyncParameters();
        assertNotNull(params.retry());
        assertSame(params.retry(), params.retry());
    }
}
