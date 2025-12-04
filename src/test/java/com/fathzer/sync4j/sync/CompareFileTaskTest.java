package com.fathzer.sync4j.sync;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

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
        SyncParameters parameters = new SyncParameters();

        // Setup default behavior
        when(context.statistics()).thenReturn(statistics);
        when(context.params()).thenReturn(parameters);
    }
    
    @Test
    void testConstructor() {
        // When
        CompareFileTask task = new CompareFileTask(context, source, destination, null);
        
        // Then
        assertSame(context, task.context());
        assertSame(source, task.action().source());
        assertSame(destination, task.action().destination());
        assertFalse(task.onlySynchronous());
        assertEquals(Kind.CHECKER, task.kind());
        
        // Verify statistics were updated
        assertEquals(1, statistics.checkedFiles().total().get());

        // Verify default value
        assertTrue(task.defaultValue());

        // Test null context
        assertThrows(NullPointerException.class, () -> new CompareFileTask(null, source, destination, null));
        
        // Test null File
        assertThrows(NullPointerException.class, () -> new CompareFileTask(context, null, destination, null));
        assertThrows(NullPointerException.class, () -> new CompareFileTask(context, source, null, null));
    }

    @Test
    void testExecuteWithIOException() {
        // Given
        IOException expectedException = new IOException("compare failed");
        context.params().fileComparator( (f1,f2) -> {throw expectedException;});
        CompareFileTask task = new CompareFileTask(context, source, destination, null);
        
        // When/Then
        IOException thrown = assertThrows(IOException.class, task::execute);
        assertSame(expectedException, thrown);
    }
    
    @Test
    void testExecute() throws IOException {
        // Given
        AtomicBoolean extraActionCalled = new AtomicBoolean(false);
        CompareFileTask task = new CompareFileTask(context, source, destination, () -> extraActionCalled.set(true));


        // When file are same
        context.params().fileComparator( (f1,f2) -> true);
        boolean result = task.execute();
        // Then
        assertTrue(result);
        assertFalse(extraActionCalled.get());
        
        // When file are different
        context.params().fileComparator( (f1,f2) -> false);
        result = task.execute();
        // Then
        assertFalse(result);
        assertTrue(extraActionCalled.get());
    }
}
