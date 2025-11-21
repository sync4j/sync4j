package com.fathzer.sync4j.sync;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fathzer.sync4j.File;
import com.fathzer.sync4j.Folder;

class DryRunFolderTest {

    @Test
    void test() throws IOException {

        Path testPath = Paths.get("testFolder");
        Folder folder = new DryRunFolder(testPath);
        assertFalse(folder.isFile());
        assertTrue(folder.isFolder());
        assertEquals("testFolder", folder.getName());
        assertEquals(List.of(), folder.list());
        assertThrows(UnsupportedOperationException.class, folder::getFileProvider);
        assertThrows(UnsupportedOperationException.class, () -> folder.mkdir("newFolder"));
        File mockFile = mock(File.class);
        assertThrows(UnsupportedOperationException.class, () -> folder.copy("test.txt", mockFile, l -> {}));

        // Test with parent
        DryRunFolder subFolder = new DryRunFolder(testPath.resolve("subfolder"));
        assertEquals(testPath.toString(), subFolder.getParentPath());
        
        // Test with null parent (root folder)
        DryRunFolder rootFolder = new DryRunFolder(Paths.get(""));
        assertNull(rootFolder.getParentPath());
        assertThrows(UnsupportedOperationException.class, folder::getParent);
        assertThrows(UnsupportedOperationException.class, folder::delete);
    }
    
    @Test
    void testNullArgs() {
        Folder folder = new DryRunFolder(Paths.get("testFolder"));
        assertThrows(NullPointerException.class, () -> new DryRunFolder(null));

        File mockFile = mock(File.class);
        assertThrows(NullPointerException.class, () -> folder.copy(null, mockFile, l -> {}));
        assertThrows(NullPointerException.class, () -> folder.copy("test.txt", null, l -> {}));
        assertThrows(NullPointerException.class, () -> folder.copy("test.txt", mockFile, null));

        assertThrows(NullPointerException.class, () -> folder.mkdir(null));
    }
}
