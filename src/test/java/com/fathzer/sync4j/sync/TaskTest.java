package com.fathzer.sync4j.sync;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import static com.fathzer.sync4j.sync.Event.Status.*;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
}
