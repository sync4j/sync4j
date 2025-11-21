package com.fathzer.sync4j.sync.parameters;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PerformanceParametersTest {

    @Test
    void testDefaultValues() {
        // When
        PerformanceParameters params = new PerformanceParameters();
        
        // Then
        assertFalse(params.fastList(), "Default fastList should be false");
        assertEquals(1, params.maxWalkThreads(), "Default maxWalkThreads should be 1");
        assertEquals(1, params.maxCopyThreads(), "Default maxCopyThreads should be 1");
        assertEquals(1, params.maxComparisonThreads(), "Default maxComparisonThreads should be 1");
    }
    
    @Test
    void testFastList() {
        // Given
        PerformanceParameters params = new PerformanceParameters();
        
        // When
        PerformanceParameters result = params.fastList(true);
        
        // Then
        assertSame(params, result, "Should return this for method chaining");
        assertTrue(params.fastList(), "fastList should be set to true");
        
        // When
        params.fastList(false);
        
        // Then
        assertFalse(params.fastList(), "fastList should be set to false");
    }
    
    @Test
    void testMaxWalkThreads() {
        // Given
        PerformanceParameters params = new PerformanceParameters();
        
        // When
        PerformanceParameters result = params.maxWalkThreads(5);
        
        // Then
        assertSame(params, result, "Should return this for method chaining");
        assertEquals(5, params.maxWalkThreads(), "maxWalkThreads should be set to 5");
        
        // Test validation
        assertThrows(IllegalArgumentException.class, () -> params.maxWalkThreads(0),
            "Should throw when maxWalkThreads is 0");
        assertThrows(IllegalArgumentException.class, () -> params.maxWalkThreads(-1),
            "Should throw when maxWalkThreads is negative");
    }
    
    @Test
    void testMaxCopyThreads() {
        // Given
        PerformanceParameters params = new PerformanceParameters();
        
        // When
        PerformanceParameters result = params.maxCopyThreads(3);
        
        // Then
        assertSame(params, result, "Should return this for method chaining");
        assertEquals(3, params.maxCopyThreads(), "maxCopyThreads should be set to 3");
        
        // Test validation
        assertThrows(IllegalArgumentException.class, () -> params.maxCopyThreads(-1),
            "Should throw when maxCopyThreads is negative");
    }
    
    @Test
    void testMaxComparisonThreads() {
        // Given
        PerformanceParameters params = new PerformanceParameters();
        
        // When
        PerformanceParameters result = params.maxComparisonThreads(4);
        
        // Then
        assertSame(params, result, "Should return this for method chaining");
        assertEquals(4, params.maxComparisonThreads(), "maxComparisonThreads should be set to 4");
        
        // Test validation

        assertThrows(IllegalArgumentException.class, () -> params.maxComparisonThreads(-1),
            "Should throw when maxComparisonThreads is negative");
    }
    
    @Test
    void testFluentInterface() {
        // When
        PerformanceParameters params = new PerformanceParameters()
            .fastList(true)
            .maxWalkThreads(2)
            .maxCopyThreads(3)
            .maxComparisonThreads(4);
        
        // Then
        assertTrue(params.fastList(), "fastList should be true");
        assertEquals(2, params.maxWalkThreads(), "maxWalkThreads should be 2");
        assertEquals(3, params.maxCopyThreads(), "maxCopyThreads should be 3");
        assertEquals(4, params.maxComparisonThreads(), "maxComparisonThreads should be 4");
    }
}
