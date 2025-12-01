package com.fathzer.sync4j;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class FileTest {
    private File file;

    @BeforeEach
    void setUp() throws IOException {
        long lastModified = System.currentTimeMillis();
        file = Mockito.mock(File.class, Mockito.CALLS_REAL_METHODS);
        Mockito.when(file.getLastModifiedTime()).thenReturn(lastModified);
    }
    
    @Test
    void testDefaultGetCreationTime() throws IOException {
        assertEquals(file.getLastModifiedTime(), file.getCreationTime(), "Default creation time should equal last modified");
    }
    
    @Test
    void testDefaultGetHash() {
        assertThrows(UnsupportedOperationException.class, () -> file.getHash(HashAlgorithm.MD5),
            "Default getHash should throw UnsupportedOperationException");
    }
    
    @Test
    void testFileInheritance() {
        assertTrue(file instanceof Entry, "File should extend Entry");
    }
}
