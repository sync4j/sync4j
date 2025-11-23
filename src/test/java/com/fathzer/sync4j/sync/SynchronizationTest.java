package com.fathzer.sync4j.sync;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.sync.parameters.SyncParameters;

@ExtendWith(MockitoExtension.class)
class SynchronizationTest {
    @Mock private Folder sourceFolder;
    @Mock private Folder destinationFolder;
    private SyncParameters parameters;
    
    private Synchronization synchronization;

    @BeforeEach
    void setUp() throws IOException {
        parameters = new SyncParameters();
        synchronization = new Synchronization(sourceFolder, destinationFolder, parameters);
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
    void testGetStatistics() {
        // When
        Statistics stats = synchronization.getStatistics();
        
        // Then
        assertNotNull(stats, "Statistics should not be null");
    }

    @Test
    void testCancel() {
        // Initial state should be not cancelled
        assertFalse(synchronization.isCancelled(), "Initial state should not be cancelled");

        // When
        synchronization.cancel();
        
        // Then
        assertTrue(synchronization.isCancelled(), "Synchronization should be cancelled after cancel()");
    }

    @Test
    void testClose() {
        // When/Then - should not throw an exception
        assertDoesNotThrow(() -> synchronization.close(), "Close should not throw an exception");
        
        // Verify that close can be called multiple times without issues
        assertDoesNotThrow(() -> synchronization.close(), "Multiple close calls should be safe");
    }
}
