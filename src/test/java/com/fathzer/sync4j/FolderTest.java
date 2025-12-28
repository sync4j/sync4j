package com.fathzer.sync4j;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class FolderTest {
    
    private Folder folder;
    
    @BeforeEach
    void setUp() {
        folder = Mockito.mock(Folder.class, Mockito.CALLS_REAL_METHODS);
    }
    
    @Test
    void testDefaultPreload() {
        assertThrows(UnsupportedOperationException.class, () -> folder.preload(),
            "Default preload should throw UnsupportedOperationException");
    }
    
    @Test
    void testDefaultCheckFileName() {
        // Valid names should not throw
        assertDoesNotThrow(() -> folder.checkFileName("valid.txt"), "Valid file name should not throw");
        assertDoesNotThrow(() -> folder.checkFileName("file"), "Valid file name without extension should not throw");
        assertDoesNotThrow(() -> folder.checkFileName("file with spaces.txt"), "File name with spaces should not throw");
        
        // Invalid names should throw
        assertThrows(IllegalArgumentException.class, () -> folder.checkFileName(""), "Empty name should throw");
        assertThrows(IllegalArgumentException.class, () -> folder.checkFileName("   "), "Whitespace-only name should throw");
        assertThrows(IllegalArgumentException.class, () -> folder.checkFileName("file/name.txt"), "Name with path separator should throw");
        assertThrows(NullPointerException.class, () -> folder.checkFileName(null), "Null name should throw");
    }
    
    @Test
    void testFolderInheritance() {
        assertTrue(folder instanceof Entry, "Folder should extend Entry");
    }
}

