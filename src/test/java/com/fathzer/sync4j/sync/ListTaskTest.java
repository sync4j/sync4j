package com.fathzer.sync4j.sync;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.sync.Task.Kind;

class ListTaskTest {
    private Context context;
    private Statistics statistics;
    private Folder target;
    
    @BeforeEach
    void setUp() {
        // Create mocks
        context = mock(Context.class);
        statistics = new Statistics();
        target = mock(Folder.class);
        
        // Setup default behavior
        when(context.statistics()).thenReturn(statistics);
        
        // Setup context to create events
        when(context.createEvent(any())).thenAnswer(invocation -> {
            Event.Action a = invocation.getArgument(0);
            return new Event(a);
        });
    }
    
    @Test
    void testConstructor() throws IOException {
        // Then
        ListTask task = new ListTask(context, target);

        // Verify attributes
        assertSame(context, task.context);
        assertSame(target, task.action.folder());
        assertFalse(task.onlySynchronous());
        assertEquals(Kind.WALKER, task.kind());

        // Verify default value
        assertNull(task.defaultValue());

        // Verify statistics were updated
        assertEquals(1, statistics.listedFolders().total().get());

        // Test null context
        assertThrows(NullPointerException.class, () -> new ListTask(null, target));
        // Test null File
        assertThrows(NullPointerException.class, () -> new ListTask(context, null));
    }

    @Test
    void testExecuteWithIOException() throws IOException {
        // Given
        IOException expectedException = new IOException("Delete failed");
        when(target.list()).thenThrow(expectedException);
        ListTask task = new ListTask(context, target);
        
        // When/Then
        IOException thrown = assertThrows(IOException.class, task::execute);
        assertSame(expectedException, thrown);
    }
    
    @Test
    void testExecute() throws IOException {
        // Given
        List<Entry> entries = new ArrayList<>();
        when(target.list()).thenReturn(entries);
        ListTask task = new ListTask(context, target);

        // When
        List<Entry> result = task.execute();
        
        // Then
        assertSame(entries, result);
    }
}
