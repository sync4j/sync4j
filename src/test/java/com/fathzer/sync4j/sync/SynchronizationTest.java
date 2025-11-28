package com.fathzer.sync4j.sync;

import static com.fathzer.sync4j.util.PrivateFields.getFieldValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fathzer.sync4j.FileProvider;
import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.sync.parameters.SyncParameters;

@ExtendWith(MockitoExtension.class)
class SynchronizationTest {
    @Mock private Folder sourceFolder;
    @Mock private Folder destinationFolder;
    private SyncParameters parameters;
    
    @BeforeEach
    void setUp() {
        parameters = new SyncParameters();
    }

    @Test
    void testConstructorWithNullParameters() {
        assertThrows(NullPointerException.class, 
            () -> new Synchronization(null, destinationFolder, parameters));
        assertThrows(NullPointerException.class, 
            () -> new Synchronization(sourceFolder, destinationFolder, null));
        assertThrows(NullPointerException.class, 
            () -> new Synchronization(sourceFolder, null, parameters));
    }

    @Test
    void test() throws IOException {
        // Given
        try (Synchronization synchronization= new Synchronization(sourceFolder, destinationFolder, parameters)) {
            // When
            Statistics stats = synchronization.getStatistics();
            
            // Then
            assertNotNull(stats, "Statistics should not be null");
            assertFalse(synchronization.isCancelled(), "Initial state should not be cancelled");

            // When
            synchronization.cancel();
            // Then
            assertTrue(synchronization.isCancelled(), "Synchronization should be cancelled after cancel()");

            // When/Then - should not throw an exception
            assertDoesNotThrow(synchronization::close, "Close should not throw an exception");
            // Then second close (on auto close) should not throw an exception
        }
    }

    @Test
    void testDoPreload() throws Exception {
        // Given
        
        // Create mock folders with providers
        FileProvider sourceProvider = mock(FileProvider.class);
        when(sourceProvider.isFastListSupported()).thenReturn(true);
        Folder source = mock(Folder.class);
        when(source.getFileProvider()).thenReturn(sourceProvider);

        FileProvider destProvider = mock(FileProvider.class);
        when(destProvider.isFastListSupported()).thenReturn(false); // Only source supports fast list
        Folder dest = mock(Folder.class);
        when(dest.getFileProvider()).thenReturn(destProvider);

        parameters.performance().fastList(true);
        Statistics stats = new Statistics();
        Context.TaskCounter counter = new Context.TaskCounter();
        Context context = mock(Context.class);
        when(context.statistics()).thenReturn(stats);
        when(context.params()).thenReturn(parameters);
        when(context.executeAsync(any(PreLoadTask.class))).thenReturn(CompletableFuture.completedFuture(mock(Folder.class)));
        when(context.taskCounter()).thenReturn(counter);

        try (Synchronization synchronization= new Synchronization(context, source, dest)) {
            // When
            synchronization.doPreload();
            
            // Then
            // Verify source was preloaded (only source supports fast list)
            verify(sourceProvider).isFastListSupported();
            verify(destProvider).isFastListSupported();
            
            // Verify task counter is back to 0
            assertEquals(0, context.taskCounter().getPendingTasks());
            
            // Verify the source folder was updated (preloaded)
            assertNotSame(source, getFieldValue(synchronization, "source", Folder.class));
            assertSame(dest, getFieldValue(synchronization, "destination", Folder.class)); // Should not be updated as it doesn't support fast list
        }
    }
}
