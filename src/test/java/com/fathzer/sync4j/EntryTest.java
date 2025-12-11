package com.fathzer.sync4j;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.io.IOException;

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
    
    @Test
    void testGetPath() throws IOException {
        // Start building a file entry
        Entry root = Mockito.mock(Folder.class, Mockito.CALLS_REAL_METHODS);
        when(root.getName()).thenReturn("");
        when(root.getParent()).thenReturn(null);

        Entry aFolder = Mockito.mock(Folder.class, Mockito.CALLS_REAL_METHODS);
        when(aFolder.getName()).thenReturn("folder");
        when(aFolder.getParent()).thenReturn(root);
        
        Entry aFile = Mockito.mock(File.class, Mockito.CALLS_REAL_METHODS);
        when(aFile.getName()).thenReturn("file.txt");
        when(aFile.getParent()).thenReturn(aFolder);

        assertEquals(FileProvider.ROOT_PATH, root.getPath(), "Root entry should have path /");
        assertEquals("/folder", aFolder.getPath(), "Folder entry should have path /folder");
        assertEquals("/folder/file.txt", aFile.getPath(), "File entry should have path /folder/file.txt");
    }
}
