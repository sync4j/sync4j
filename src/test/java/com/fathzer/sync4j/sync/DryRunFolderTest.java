package com.fathzer.sync4j.sync;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fathzer.sync4j.File;
import com.fathzer.sync4j.FileProvider;
import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.memory.MemoryFileProvider;
import com.fathzer.sync4j.memory.MemoryFolder;

class DryRunFolderTest {
    private FileProvider fileProvider;
    private MemoryFolder root;

    @BeforeEach
    void setUp() throws IOException {
        this.fileProvider = new MemoryFileProvider();
        root = (MemoryFolder) fileProvider.get(MemoryFileProvider.ROOT_PATH).asFolder();
    }

    @AfterEach
    void tearDown() {
        fileProvider.close();
    }

    @Test
    void test() throws IOException {
        MemoryFolder nonRoot = root.mkdir("parent");

        final Folder folder = new DryRunFolder(nonRoot, "testFolder");
        assertFalse(folder.isFile());
        assertTrue(folder.isFolder());
        assertEquals("testFolder", folder.getName());
        assertEquals(nonRoot, folder.getParent());
        assertEquals(List.of(), folder.list());
        assertThrows(UnsupportedOperationException.class, folder::getFileProvider);
        assertThrows(UnsupportedOperationException.class, () -> folder.mkdir("newFolder"));
        File mockFile = mock(File.class);
        assertThrows(UnsupportedOperationException.class, () -> folder.copy("test.txt", mockFile, l -> {}));
        
        // Test with null parent (root folder)
        final Folder other = new DryRunFolder(root, "testFolder");
        assertEquals(root, other.getParent());
        assertThrows(UnsupportedOperationException.class, other::delete);
    }
    
    @Test
    void testNullArgs() {
        assertThrows(NullPointerException.class, () -> new DryRunFolder(null, "testFolder"));
        assertThrows(NullPointerException.class, () -> new DryRunFolder(root, null));

        final Folder folder = new DryRunFolder(root, "testFolder");
        File mockFile = mock(File.class);
        assertThrows(NullPointerException.class, () -> folder.copy(null, mockFile, l -> {}));
        assertThrows(NullPointerException.class, () -> folder.copy("test.txt", null, l -> {}));
        assertThrows(NullPointerException.class, () -> folder.copy("test.txt", mockFile, null));

        assertThrows(NullPointerException.class, () -> folder.mkdir(null));
    }
}
