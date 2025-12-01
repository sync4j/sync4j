package com.fathzer.sync4j;

import static org.junit.jupiter.api.Assertions.*;

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
    }
    
    @Test
    void testGetWithNullPath() {
        assertThrows(NullPointerException.class, () -> provider.get(null),
            "Should throw NullPointerException for null path");
    }
    
    // Mock implementation for testing
    private static class TestFileProvider implements FileProvider {
        @Override
        public Entry get(String path) throws IOException {
            if (path == null) {
                throw new NullPointerException("Path cannot be null");
            }
            return new TestEntry(path, this);
        }
    }
    
    private static class TestEntry implements Entry {
        private final String name;
        private final FileProvider provider;
        
        TestEntry(String name, FileProvider provider) {
            this.name = name;
            this.provider = provider;
        }
        
        @Override
        public boolean isFile() {
            return false;
        }
        
        @Override
        public boolean isFolder() {
            return false;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public String getParentPath() throws IOException {
            return null;
        }
        
        @Override
        public Folder getParent() throws IOException {
            return null;
        }
        
        @Override
        public void delete() throws IOException {
            // Do nothing
        }
        
        @Override
        public FileProvider getFileProvider() {
            return provider;
        }
    }
}
