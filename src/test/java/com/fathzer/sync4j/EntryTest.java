package com.fathzer.sync4j;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class EntryTest {
    
    private Entry fileEntry;
    private Entry folderEntry;
    private Entry nonExistingEntry;
    
    @BeforeEach
    void setUp() {
        fileEntry = Mockito.mock(File.class, Mockito.CALLS_REAL_METHODS);
        Mockito.when(fileEntry.isFile()).thenReturn(true);
        Mockito.when(fileEntry.isFolder()).thenReturn(false);
        folderEntry = Mockito.mock(Folder.class, Mockito.CALLS_REAL_METHODS);
        Mockito.when(folderEntry.isFile()).thenReturn(false);
        Mockito.when(folderEntry.isFolder()).thenReturn(true);
        nonExistingEntry = Mockito.mock(Entry.class, Mockito.CALLS_REAL_METHODS);
        Mockito.when(nonExistingEntry.isFile()).thenReturn(false);
        Mockito.when(nonExistingEntry.isFolder()).thenReturn(false);
    }
    
    @Test
    void testExists() {
        assertTrue(fileEntry.exists(), "File entry should exist");
        assertTrue(folderEntry.exists(), "Folder entry should exist");
        assertFalse(nonExistingEntry.exists(), "Non-existing entry should not exist");
    }
    
    @Test
    void testAsFile() {
        assertSame(fileEntry, fileEntry.asFile(), "asFile should return this entry for files");
        assertThrows(IllegalStateException.class, () -> folderEntry.asFile(),
            "asFile should throw IllegalStateException for folders");
        assertThrows(IllegalStateException.class, () -> nonExistingEntry.asFile(),
            "asFile should throw IllegalStateException for non-existing entries");
    }
    
    @Test
    void testAsFolder() {
        assertSame(folderEntry, folderEntry.asFolder(), "asFolder should return this entry for folders");
        assertThrows(IllegalStateException.class, () -> fileEntry.asFolder(),
            "asFolder should throw IllegalStateException for files");
        assertThrows(IllegalStateException.class, () -> nonExistingEntry.asFolder(),
            "asFolder should throw IllegalStateException for non-existing entries");
    }
}
