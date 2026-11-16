package com.fathzer.sync4j;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.time.Duration;

import org.junit.jupiter.api.Test;

class RetryableIOExceptionTest {
    @Test
    void testMessageOnly() {
        RetryableIOException e = new RetryableIOException("busy");
        assertEquals("busy", e.getMessage());
        assertNull(e.getCause());
        assertNull(e.getRetryAfter());
    }

    @Test
    void testCause() {
        IOException cause = new IOException("cause");
        RetryableIOException e = new RetryableIOException("busy", cause);
        assertSame(cause, e.getCause());
        assertNull(e.getRetryAfter());
    }

    @Test
    void testRetryAfter() {
        RetryableIOException e = new RetryableIOException("throttled", null, Duration.ofSeconds(3));
        assertEquals(Duration.ofSeconds(3), e.getRetryAfter());
        assertEquals(Duration.ZERO, new RetryableIOException("now", null, Duration.ZERO).getRetryAfter());
    }

    @Test
    void testNegativeRetryAfter() {
        assertThrows(IllegalArgumentException.class, () -> new RetryableIOException("x", null, Duration.ofSeconds(-1)));
    }

    @Test
    void testIsAnIOException() {
        assertInstanceOf(IOException.class, new RetryableIOException("x"));
    }
}
