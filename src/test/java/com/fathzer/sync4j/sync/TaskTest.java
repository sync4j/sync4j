package com.fathzer.sync4j.sync;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import static com.fathzer.sync4j.sync.Event.Status.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fathzer.sync4j.RetryableIOException;
import com.fathzer.sync4j.sync.parameters.SyncParameters;

class TaskTest {
    
    private Context context;
    private Event.Action action;
    private Statistics.Counter counter;
    private List<Event.Status> statuses = new LinkedList<>();
    
    // Concrete implementation of Task for testing
    private static class TestTask extends Task<String, Event.Action> {
        private boolean shouldFail;

        TestTask(Context context, Event.Action action, Statistics.Counter counter) {
            super(context, action, counter);
        }

        @Override
        protected String execute() throws IOException {
            if (shouldFail) {
                throw new IOException("Should not be called");
            }
            return "test";
        }
    }
    
    @BeforeEach
    void setUp() {
        // Create mocks
        context = mock(Context.class);
        action = mock(Event.Action.class);
        counter = new Statistics.Counter(new AtomicLong(0), new AtomicLong(0));

        // Setup event listener
        SyncParameters parameters = new SyncParameters();
        parameters.eventListener(event -> {
            assertEquals(action, event.action());
            statuses.add(event.status());
        });
        when(context.params()).thenReturn(parameters);

    }
    
    @Test
    void testConstructor() throws IOException {
        // Test that constructor sets up fields correctly
        TestTask task = new TestTask(context, action, counter);
        
        assertSame(context, task.context());
        assertSame(action, task.action());
        
        // Verify counter.total() was incremented
        assertEquals(1, counter.total().get());
        assertEquals(List.of(PLANNED), statuses);

        assertNull(task.defaultValue());
        assertEquals(Task.Kind.MODIFIER, task.kind());
        assertFalse(task.onlySynchronous());
    }
    
    @Test
    void testConstructorNullChecks() {
        // Test null context
        assertThrows(NullPointerException.class, () -> new TestTask(null, action, counter));
        
        // Test null action
        assertThrows(NullPointerException.class, () -> new TestTask(context, null, counter));
        
        // Test null counter
        assertThrows(NullPointerException.class, () -> new TestTask(context, action, null));
    }

    @Test
    void testCall() throws IOException {
        // Test that call() returns the expected value
        TestTask task = new TestTask(context, action, counter);
        assertEquals("test", task.call());
        assertEquals(List.of(PLANNED, STARTED, COMPLETED), statuses);
        assertEquals(1, counter.done().get());

        // Test that call() throws IOException when execute() fails
        statuses.clear();
        Statistics.Counter counter2 = new Statistics.Counter(new AtomicLong(0), new AtomicLong(0));
        TestTask task2 = new TestTask(context, action, counter2);
        task2.shouldFail = true;
        assertThrows(IOException.class, task2::call);
        assertEquals(List.of(PLANNED, STARTED, FAILED), statuses);
        assertEquals(1, counter2.total().get());
        assertEquals(0, counter2.done().get());
    }

    /** A task that fails with the provided exceptions, then succeeds. */
    private static class FlakyTask extends Task<String, Event.Action> {
        private final LinkedList<IOException> failures;
        private final boolean propagationDelay;
        private int attempts;
        private int rollbacks;

        FlakyTask(Context context, Event.Action action, Statistics.Counter counter, boolean propagationDelay, IOException... failures) {
            super(context, action, counter);
            this.failures = new LinkedList<>(List.of(failures));
            this.propagationDelay = propagationDelay;
        }

        @Override
        protected String execute() throws IOException {
            attempts++;
            if (!failures.isEmpty()) {
                throw failures.removeFirst();
            }
            return "ok";
        }

        @Override
        protected boolean isPropagationDelay(IOException e) {
            return propagationDelay;
        }

        @Override
        protected void rollback() {
            rollbacks++;
        }
    }

    private void fastRetries(int maxAttempts) {
        context.params().retry().maxAttempts(maxAttempts).initialDelay(Duration.ofMillis(1)).maxDelay(Duration.ofMillis(5));
    }

    @Test
    void testRetryOnRetryableException() throws IOException {
        fastRetries(3);
        FlakyTask task = new FlakyTask(context, action, counter, false, new RetryableIOException("busy"), new RetryableIOException("busy"));
        assertEquals("ok", task.call());
        assertEquals(3, task.attempts);
        assertEquals(2, task.rollbacks);
        // Retries are not visible in the events
        assertEquals(List.of(PLANNED, STARTED, COMPLETED), statuses);
        assertEquals(1, counter.done().get());
    }

    @Test
    void testThrottleIsCalledForEachAttempt() throws Exception {
        fastRetries(3);
        FlakyTask task = new FlakyTask(context, action, counter, false, new RetryableIOException("busy"));
        task.call();
        verify(context, times(2)).throttle();
    }

    @Test
    void testInterruptedWhileThrottling() throws Exception {
        doThrow(new InterruptedException()).when(context).throttle();
        FlakyTask task = new FlakyTask(context, action, counter, false);
        try {
            assertThrows(java.io.InterruptedIOException.class, task::call);
            assertEquals(0, task.attempts);
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted(); // Clear the flag
        }
    }

    @Test
    void testRetryGivesUp() {
        fastRetries(2);
        FlakyTask task = new FlakyTask(context, action, counter, false, new RetryableIOException("1"), new RetryableIOException("2"), new RetryableIOException("3"));
        IOException e = assertThrows(RetryableIOException.class, task::call);
        assertEquals("2", e.getMessage());
        assertEquals(2, task.attempts);
        assertEquals(2, task.rollbacks, "Rollback should be called before the retry and after the failure");
        assertEquals(List.of(PLANNED, STARTED, FAILED), statuses);
    }

    @Test
    void testNoRetryOnOtherException() {
        fastRetries(3);
        FlakyTask task = new FlakyTask(context, action, counter, false, new FileNotFoundException("missing"));
        assertThrows(FileNotFoundException.class, task::call);
        assertEquals(1, task.attempts);
        assertEquals(1, task.rollbacks, "Only the rollback that follows the final failure is expected");
    }

    @Test
    void testRetryOnPropagationDelay() throws IOException {
        fastRetries(3);
        FlakyTask task = new FlakyTask(context, action, counter, true, new FileNotFoundException("missing"));
        assertEquals("ok", task.call());
        assertEquals(2, task.attempts);
    }

    @Test
    void testNoRetryIfCancelled() {
        fastRetries(3);
        when(context.isCancelled()).thenReturn(true);
        FlakyTask task = new FlakyTask(context, action, counter, false, new RetryableIOException("busy"));
        assertThrows(RetryableIOException.class, task::call);
        assertEquals(1, task.attempts);
    }

    @Test
    void testNoRetryIfRetryAfterIsTooLong() {
        fastRetries(3);
        FlakyTask task = new FlakyTask(context, action, counter, false, new RetryableIOException("busy", null, Duration.ofHours(1)));
        assertThrows(RetryableIOException.class, task::call);
        assertEquals(1, task.attempts);
    }

    @Test
    void testRetryAfterIsUsed() throws IOException {
        fastRetries(2);
        context.params().retry().maxDelay(Duration.ofSeconds(1));
        FlakyTask task = new FlakyTask(context, action, counter, false, new RetryableIOException("busy", null, Duration.ofMillis(100)));
        long start = System.nanoTime();
        assertEquals("ok", task.call());
        assertTrue(System.nanoTime() - start >= Duration.ofMillis(100).toNanos());
    }
}
