package com.fathzer.sync4j.sync;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fathzer.sync4j.File;
import com.fathzer.sync4j.sync.parameters.SyncParameters;
import com.fathzer.sync4j.sync.Task.Kind;

class CompareFileTaskTest {
    private Context context;
    private Statistics statistics;
    private File source;
    private File destination;
    
    @BeforeEach
    void setUp() {
        // Create mocks
        context = mock(Context.class);
        statistics = new Statistics();
        source = mock(File.class);
        destination = mock(File.class);
        
        // Setup default behavior
        when(context.statistics()).thenReturn(statistics);
        
        // Setup context to create events
        when(context.createEvent(any())).thenAnswer(invocation -> {
            Event.Action a = invocation.getArgument(0);
            return new Event(a);
        });

        SyncParameters params = new SyncParameters();
        when(context.params()).thenReturn(params);
    }
    
    @Test
    void testConstructor() {
        // When
        CompareFileTask task = new CompareFileTask(context, source, destination);
        
        // Then
        assertSame(context, task.context);
        assertSame(source, task.action.source());
        assertSame(destination, task.action.destination());
        assertFalse(task.onlySynchronous());
        assertEquals(Kind.CHECKER, task.kind());
        
        // Verify statistics were updated
        assertEquals(1, statistics.checkedFiles().total().get());

        // Verify default value
        assertTrue(task.defaultValue());

        // Test null context
        assertThrows(NullPointerException.class, () -> new CompareFileTask(null, source, destination));
        
        // Test null File
        assertThrows(NullPointerException.class, () -> new CompareFileTask(context, null, destination));
        assertThrows(NullPointerException.class, () -> new CompareFileTask(context, source, null));
    }

    @Test
    void testExecuteWithIOException() {
        // Given
        IOException expectedException = new IOException("compare failed");
        context.params().fileComparator( (f1,f2) -> {throw expectedException;});
        CompareFileTask task = new CompareFileTask(context, source, destination);
        
        // When/Then
        IOException thrown = assertThrows(IOException.class, task::execute);
        assertSame(expectedException, thrown);
    }
    
    @Test
    void testExecute() throws IOException {
        // Given
        context.params().fileComparator( (f1,f2) -> false);
        CompareFileTask task = new CompareFileTask(context, source, destination);

        // When
        Boolean result = task.execute();
        
        // Then
        assertFalse(result);
    }
}
