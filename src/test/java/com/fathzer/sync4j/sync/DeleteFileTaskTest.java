package com.fathzer.sync4j.sync;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.File;
import com.fathzer.sync4j.sync.Task.Kind;
import com.fathzer.sync4j.sync.parameters.SyncParameters;

class DeleteFileTaskTest {
    private Context context;
    private Statistics statistics;
    private Entry target;
    
    @BeforeEach
    void setUp() {
        // Create mocks
        context = mock(Context.class);
        statistics = new Statistics();
        target = mock(File.class);
        
        // Setup default behavior
        when(context.statistics()).thenReturn(statistics);
        SyncParameters parameters = new SyncParameters();
        when(context.params()).thenReturn(parameters);
    }
    
    @Test
    void testConstructor() throws IOException {
        // When file
        when(target.isFile()).thenReturn(true);
        when(target.isFolder()).thenReturn(false);
        
        // Then
        DeleteTask task = new DeleteTask(context, target);

        // Verify attributes
        assertSame(context, task.context());
        assertSame(target, task.action().entry());
        assertFalse(task.onlySynchronous());
        assertEquals(Kind.MODIFIER, task.kind());

        // Verify default value
        assertNull(task.defaultValue());

        // Verify statistics were updated
        assertEquals(1, statistics.deletedFiles().total().get());
        assertEquals(0, statistics.deletedFolders().total().get());

        // When folder
        when(target.isFile()).thenReturn(false);
        when(target.isFolder()).thenReturn(true);

        // Then
        new DeleteTask(context, target);

        // Verify statistics were updated
        assertEquals(1, statistics.deletedFiles().total().get());
        assertEquals(1, statistics.deletedFolders().total().get());

        // Test null context
        assertThrows(NullPointerException.class, () -> new DeleteTask(null, target));
        // Test null File
        assertThrows(NullPointerException.class, () -> new DeleteTask(context, null));
    }

    @Test
    void testExecuteWithIOException() throws IOException {
        // Given
        IOException expectedException = new IOException("Delete failed");
        Mockito.doThrow(expectedException).when(target).delete();
        DeleteTask task = new DeleteTask(context, target);
        
        // When/Then
        IOException thrown = assertThrows(IOException.class, task::execute);
        assertSame(expectedException, thrown);
    }
    
    @Test
    void testExecute() throws IOException {
        // Given
        doNothing().when(target).delete();
        DeleteTask task = new DeleteTask(context, target);

        // When
        task.execute();
        
        // Then
        verify(target).delete();
    }
}
