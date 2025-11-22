package com.fathzer.sync4j.sync;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TaskTest {
    
    private Context context;
    private Event.Action action;
    private Statistics.Counter counter;
    
    // Concrete implementation of Task for testing
    private static class TestTask extends Task<String, Event.Action> {
        TestTask(Context context, Event.Action action, Statistics.Counter counter) {
            super(context, action, counter);
        }

        @Override
        protected String execute() throws IOException {
            throw new IOException("Should not be called");
        }
    }
    
    @BeforeEach
    void setUp() {
        // Create mocks
        context = mock(Context.class);
        action = mock(Event.Action.class);
        counter = new Statistics.Counter(new AtomicLong(0), new AtomicLong(0));
        
        // Setup default behavior for context
        when(context.createEvent(any())).thenAnswer(invocation -> {
            Event.Action a = invocation.getArgument(0);
            return new Event(a);
        });
    }
    
    @Test
    void testConstructor() throws IOException {
        // Test that constructor sets up fields correctly
        TestTask task = new TestTask(context, action, counter);
        
        assertSame(context, task.context);
        assertSame(action, task.action);
        assertNotNull(task.event);
        assertSame(counter, task.counter);
        
        // Verify event was created with the action
        assertSame(action, task.event.getAction());
        
        // Verify counter.total() was incremented
        assertEquals(1, counter.total().get());

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
        
        // Test context.createEvent returns null
        when(context.createEvent(any())).thenReturn(null);
        assertThrows(NullPointerException.class, () -> new TestTask(context, action, counter));
    }
}
