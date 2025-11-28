package com.fathzer.sync4j.sync;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.sync.parameters.SyncParameters;

class CreateFolderTaskTest {
    private Context context;
    private Statistics statistics;
    private Folder destinationFolder;
    
    @BeforeEach
    void setUp() throws IOException {
        // Create mocks
        context = mock(Context.class);
        statistics = new Statistics();
        destinationFolder = mock(Folder.class);
        SyncParameters parameters = new SyncParameters();
        
        // Setup default behavior
        when(context.statistics()).thenReturn(statistics);
        when(context.params()).thenReturn(parameters);

        // Setup destination folder behavior
        when(destinationFolder.getParentPath()).thenReturn("/test");
        when(destinationFolder.getName()).thenReturn("test");
    }
    
    @Test
    void testConstructor() throws IOException {
        // When
        CreateFolderTask task = new CreateFolderTask(context, destinationFolder, "test");
        
        // Then
        assertSame(context, task.context());
        assertSame(destinationFolder, task.action().folder());
        assertEquals("test", task.action().name());
        assertTrue(task.onlySynchronous());
        
        // Verify statistics were updated
        assertEquals(1, statistics.createdFolders().total().get());

        // Verify default value
        Folder defaultValue = task.defaultValue();
        assertEquals(DryRunFolder.class, defaultValue.getClass());
        assertEquals(destinationFolder.getParentPath()+"/"+destinationFolder.getName(), defaultValue.getParentPath());
        assertEquals("test", defaultValue.getName());

        // Test null context
        assertThrows(NullPointerException.class, () -> new CreateFolderTask(null, destinationFolder, "test"));
        
        // Test null action
        assertThrows(NullPointerException.class, () -> new CreateFolderTask(context, null, "test"));
    }

    @Test
    void testExecuteWithIOException() throws IOException {
        // Given
        IOException expectedException = new IOException("Mkdir failed");
        when(destinationFolder.mkdir(anyString())).thenThrow(expectedException);
        CreateFolderTask task = new CreateFolderTask(context, destinationFolder, "test");
        
        // When/Then
        IOException thrown = assertThrows(IOException.class, task::execute);
        assertSame(expectedException, thrown);
    }
    
    @Test
    void testExecute() throws IOException {
        // Given
        Folder createdFolder = mock(Folder.class);
        when (destinationFolder.mkdir(anyString())).thenAnswer(invocation -> {
            return createdFolder;
        });
        CreateFolderTask task = new CreateFolderTask(context, destinationFolder, "test");

        // When
        Folder result = task.execute();
        
        // Then
        assertSame(createdFolder, result);
    }
}
