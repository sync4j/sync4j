package com.fathzer.sync4j.memory;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fathzer.sync4j.File;
import com.fathzer.sync4j.FileProvider;
import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.HashAlgorithm;
import com.fathzer.sync4j.test.AbstractFileProviderTest;

class MemoryFileProviderTest extends AbstractFileProviderTest {
    @Override
    protected FileProvider createFileProvider() throws IOException {
        return new MemoryFileProvider();
    }

    @Override
    protected UnderlyingFileSystem getUnderlyingFileSystem() {
        return null;
    }

    private MemoryFolder root() {
        return (MemoryFolder) root;
    }

    @Test
    void testSupportedHash() {
        assertEquals(List.of(HashAlgorithm.values()), provider.getSupportedHash());
    }

    @Test
    void testCreateFile() throws IOException {
        byte[] content = "Hello, World!".getBytes(StandardCharsets.UTF_8);
        File file = root().createFile("test.txt", content);

        assertTrue(file.exists(), "File should exist");
        assertTrue(file.isFile(), "Entry should be a file");
        assertFalse(file.isFolder(), "Entry should not be a folder");
        assertEquals("test.txt", file.getName());
        assertEquals(content.length, file.getSize());

        // Verify content
        try (InputStream is = file.getInputStream()) {
            byte[] readContent = is.readAllBytes();
            assertArrayEquals(content, readContent, "Content should match");
        }

        // Recreate a file at /test.txt should not throw any exception
        assertThrows(IOException.class,
                () -> root().createFile("test.txt", "content".getBytes(StandardCharsets.UTF_8)));

        // Trying to create a file at a folder location should throw IOException
        root().mkdir("folder");
        assertThrows(IOException.class,
                () -> root().createFile("folder", "content".getBytes(StandardCharsets.UTF_8)),
                "Should throw IOException when path is a folder");
    }
    
    @Test
    @Override
    protected void testDeleteFile() throws IOException {
        super.testDeleteFile();
        File entry = root.copy("test.txt", createMockFile("content"), null);

        // When
        entry.delete();

        // Check optional behavior, x.delete() makes x.exists() return false and some other methods throw IOException
        assertFalse(entry.exists());
        assertThrows(IOException.class, entry::getSize, "Should throw IOException when file does not exist");
        assertThrows(IOException.class, entry::getCreationTime, "Should throw IOException when file does not exist");
        assertThrows(IOException.class, entry::getLastModifiedTime, "Should throw IOException when file does not exist");
        assertThrows(IOException.class, () -> entry.getHash(HashAlgorithm.SHA256), "Should throw IOException when file does not exist");
        assertThrows(IOException.class, entry::getInputStream, "Should throw IOException when file does not exist");
    }
    
    @Test
    @Override
    protected void testDeleteFolder() throws IOException {
        super.testDeleteFolder();
        // Given
        Folder parent = root.mkdir("parent");
        Folder subfolder = parent.mkdir("subfolder");
        File fileInSubfolder = subfolder.copy("file.txt", createMockFile("content"), null);

        // When
        parent.delete();

        // Then
        // Check optional behavior, delete makes Folder.exists() return false and some other methods throw IOException
        assertFalse(parent.exists());
        assertThrows(IOException.class, parent::list, "Should throw IOException when folder does not exist");
        assertThrows(IOException.class, () -> parent.mkdir("child"), "Should throw IOException when folder does not exist");
        assertThrows(IOException.class, () -> parent.copy("child", null, null), "Should throw IOException when folder does not exist");

        // Check optional behavior, previously listed entries no more exists
        assertFalse(subfolder.exists(), "Subfolder should not exist after parent deletion");
        assertFalse(fileInSubfolder.exists(), "File in subfolder should not exist after parent deletion");
    }
    
    @Test
    void testFileHash() throws IOException {
        byte[] content = "Hello, World!".getBytes(StandardCharsets.UTF_8);
        File file = root().createFile("test.txt", content);

        String hash = file.getHash(HashAlgorithm.SHA256);
        assertNotNull(hash);
        assertFalse(hash.isEmpty());

        // Verify hash is consistent
        String hash2 = file.getHash(HashAlgorithm.SHA256);
        assertEquals(hash, hash2, "Hash should be consistent");
    }

    @Test
    void testFileTimestamps() throws IOException {
        long creationTime = 1000000L;
        long lastModified = 2000000L;
        byte[] content = "content".getBytes(StandardCharsets.UTF_8);

        MemoryFile file = root().createFile("test.txt", content);
        file.setCreationTime(creationTime);
        file.setLastModifiedTime(lastModified);

        assertEquals(creationTime, file.getCreationTime());
        assertEquals(lastModified, file.getLastModifiedTime());
    }

    @Test
    @Override
    protected void testPreload() throws IOException {
        assertFalse(provider.isFastListSupported());
        super.testPreload();
    }

    @Test
    void testConcurrentThings() throws IOException {
        String name = "test.txt";
        File file = root().createFile(name, "content".getBytes(StandardCharsets.UTF_8));
        file.delete();
        assertTrue(root.list().isEmpty(), "Root should be empty after deleting file");

        assertDoesNotThrow(() -> root().removeChild(name, file));

        File other = root().createFile(name, "other content".getBytes(StandardCharsets.UTF_8));
        assertNotSame(other, file, "deleted and recreate files should be different");

        root().removeChild(name, file);
        assertSame(other, provider.get("/" + name), "removeChild should have done nothing");
    }
}
