package com.fathzer.sync4j;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Answers.RETURNS_DEFAULTS;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FileProviderTest {
    
    private TestFileProvider provider;
    
    @BeforeEach
    void setUp() {
        provider = new TestFileProvider();
    }

    @Test
    void testDefaultCheckPath() {
        assertDoesNotThrow(() -> provider.checkPath("/test/path"), "Default implementation should not throw exception");
        assertThrows(IllegalArgumentException.class, () -> provider.checkPath("/test//path"), "Should throw IllegalArgumentException for path with empty segment");
        assertThrows(IllegalArgumentException.class, () -> provider.checkPath("test"), "Should throw IllegalArgumentException for path without root /");
    }

    @Test
    void testDefaultReadOnly() {
        assertTrue(provider.isWriteSupported(), "Default implementation should support write operations");
        assertFalse(provider.isReadOnly(), "Default implementation should not be read-only");
        assertDoesNotThrow(() -> provider.setReadOnly(true), "Default implementation should not throw UnsupportedOperationException");
        
        FileProvider anotherProvider = mock(FileProvider.class, CALLS_REAL_METHODS);
        when(anotherProvider.isWriteSupported()).thenReturn(false);
        assertFalse(anotherProvider.isWriteSupported(), "Default implementation should not support write operations");
        assertTrue(anotherProvider.isReadOnly(), "Default implementation should be read-only");
        assertThrows(UnsupportedOperationException.class, () -> anotherProvider.setReadOnly(false), "Default implementation should throw UnsupportedOperationException");
    }
    
    @Test
    void testDefaultGetSupportedHash() {
        List<HashAlgorithm> supported = provider.getSupportedHash();
        assertNotNull(supported, "Supported hash list should not be null");
        assertTrue(supported.isEmpty(), "Default implementation should return empty list");
    }
    
    @Test
    void testDefaultIsFastListSupported() {
        assertFalse(provider.isFastListSupported(), "Default implementation should return false");
    }
    
    @Test
    void testDefaultClose() {
        assertDoesNotThrow(() -> provider.close(), "Default close should not throw exception");
    }
    
    @Test
    void testGet() throws IOException {
        Entry entry = provider.get("/test/path");
        assertNotNull(entry, "get should return non-null entry");
        assertEquals("/test/path", entry.getName(), "Entry should have correct name");
        assertThrows(NullPointerException.class, () -> provider.get(null), "Should throw NullPointerException for null path");
    }
    
    // Mock implementation for testing
    private static class TestFileProvider implements FileProvider {
        @Override
        public Entry get(String path) throws IOException {
            this.checkPath(path);
            Entry entry = mock(Entry.class, RETURNS_DEFAULTS);
            when(entry.getName()).thenReturn(path);
            when(entry.getFileProvider()).thenReturn(this);
            return entry;
        }
    }
}
