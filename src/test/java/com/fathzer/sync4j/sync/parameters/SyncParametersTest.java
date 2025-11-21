package com.fathzer.sync4j.sync.parameters;

import static org.junit.jupiter.api.Assertions.*;

import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.sync.Event;
import com.fathzer.sync4j.sync.Event.Action;

@ExtendWith(MockitoExtension.class)
class SyncParametersTest {
    @Mock
    private Predicate<Entry> filter;
    
    @Mock
    private FileComparator fileComparator;
    
    @Mock
    private Consumer<Event> eventListener;
    
    @Mock
    private BiPredicate<Throwable, Action> errorManager;
    
    @Mock
    private Entry testEntry;

    @Test
    void testDefaultValues() {
        // When
        SyncParameters params = new SyncParameters();
        
        // Then
        assertFalse(params.dryRun(), "Default dryRun should be false");
        assertNotNull(params.filter(), "Default filter should not be null");
        assertTrue(params.filter().test(testEntry), "Default filter should accept all entries");
        assertNotNull(params.fileComparator(), "Default fileComparator should not be null");
        assertNotNull(params.performance(), "Default performance params should not be null");
        assertNotNull(params.eventListener(), "Default eventListener should not be null");
        assertNotNull(params.errorManager(), "Default errorManager should not be null");
    }
    
    @Test
    void testFilter() {
        // Given
        SyncParameters params = new SyncParameters();
        
        // When
        SyncParameters result = params.filter(filter);
        
        // Then
        assertSame(params, result, "Should return this for method chaining");
        assertSame(filter, params.filter(), "Filter should be set");
    }
    
    @Test
    void testFilterWithNull() {
        // Given
        SyncParameters params = new SyncParameters();
        
        // When/Then
        assertThrows(NullPointerException.class, () -> params.filter(null),
            "Should throw when filter is null");
    }
    
    @Test
    void testFileComparator() {
        // Given
        SyncParameters params = new SyncParameters();
        
        // When
        SyncParameters result = params.fileComparator(fileComparator);
        
        // Then
        assertSame(params, result, "Should return this for method chaining");
        assertSame(fileComparator, params.fileComparator(), "FileComparator should be set");
    }
    
    @Test
    void testFileComparatorWithNull() {
        // Given
        SyncParameters params = new SyncParameters();
        
        // When/Then
        assertThrows(NullPointerException.class, () -> params.fileComparator(null),
            "Should throw when fileComparator is null");
    }
    
    @Test
    void testDryRun() {
        // Given
        SyncParameters params = new SyncParameters();
        
        // When
        SyncParameters result = params.dryRun(true);
        
        // Then
        assertSame(params, result, "Should return this for method chaining");
        assertTrue(params.dryRun(), "dryRun should be true");
        
        // When
        params.dryRun(false);
        
        // Then
        assertFalse(params.dryRun(), "dryRun should be false");
    }
    
    @Test
    void testEventListener() {
        // Given
        SyncParameters params = new SyncParameters();
        
        // When
        SyncParameters result = params.eventListener(eventListener);
        
        // Then
        assertSame(params, result, "Should return this for method chaining");
        assertSame(eventListener, params.eventListener(), "EventListener should be set");
    }
    
    @Test
    void testEventListenerWithNull() {
        // Given
        SyncParameters params = new SyncParameters();
        
        // When/Then
        assertThrows(NullPointerException.class, () -> params.eventListener(null),
            "Should throw when eventListener is null");
    }
    
    @Test
    void testErrorManager() {
        // Given
        SyncParameters params = new SyncParameters();
        
        // When
        SyncParameters result = params.errorManager(errorManager);
        
        // Then
        assertSame(params, result, "Should return this for method chaining");
        assertSame(errorManager, params.errorManager(), "ErrorManager should be set");
    }
    
    @Test
    void testErrorManagerWithNull() {
        // Given
        SyncParameters params = new SyncParameters();
        
        // When/Then
        assertThrows(NullPointerException.class, () -> params.errorManager(null),
            "Should throw when errorManager is null");
    }
    
    @Test
    void testFluentInterface() {
        // When
        SyncParameters params = new SyncParameters()
            .dryRun(true)
            .filter(filter)
            .fileComparator(fileComparator)
            .eventListener(eventListener)
            .errorManager(errorManager);
        
        // Then
        assertTrue(params.dryRun(), "dryRun should be true");
        assertSame(filter, params.filter(), "Filter should be set");
        assertSame(fileComparator, params.fileComparator(), "FileComparator should be set");
        assertSame(eventListener, params.eventListener(), "EventListener should be set");
        assertSame(errorManager, params.errorManager(), "ErrorManager should be set");
    }
}