package com.fathzer.sync4j.sync;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.sync.Task.Kind;
import com.fathzer.sync4j.sync.parameters.SyncParameters;

class PreloadTaskTest {
    private Context context;
    private Statistics statistics;
    private Folder folder;
    
    @BeforeEach
    void setUp() {
        // Create mocks
        context = mock(Context.class);
        statistics = new Statistics();
        folder = mock(Folder.class);
        
        // Setup default behavior
        when(context.statistics()).thenReturn(statistics);
        SyncParameters parameters = new SyncParameters();
        when(context.params()).thenReturn(parameters);
    }
    
    @Test
    void testConstructor() throws IOException {
        // When
        PreLoadTask task = new PreLoadTask(context, folder);
        
        // Then
        assertSame(context, task.context());
        assertSame(folder, task.action().folder());
        assertFalse(task.onlySynchronous());
        assertEquals(Kind.WALKER, task.kind());
        
        // Verify statistics were updated
        assertEquals(1, statistics.preloadedFolders().total().get());

        // Verify default value
        assertNull(task.defaultValue());

        // Test null context
        assertThrows(NullPointerException.class, () -> new PreLoadTask(null, folder));
        
        // Test null action
        assertThrows(NullPointerException.class, () -> new CreateFolderTask(context, null, "test"));
    }

    @Test
    void testExecuteWithIOException() throws IOException {
        // Given
        IOException expectedException = new IOException("Mkdir failed");
        when(folder.preload()).thenThrow(expectedException);
        PreLoadTask task = new PreLoadTask(context, folder);
        
        // When/Then
        IOException thrown = assertThrows(IOException.class, task::execute);
        assertSame(expectedException, thrown);
    }
    
    @Test
    void testExecute() throws IOException {
        // Given
        Folder createdFolder = mock(Folder.class);
        when (folder.preload()).thenAnswer(invocation -> {
            return createdFolder;
        });
        PreLoadTask task = new PreLoadTask(context, folder);

        // When
        Folder result = task.execute();
        
        // Then
        assertSame(createdFolder, result);
    }
}
