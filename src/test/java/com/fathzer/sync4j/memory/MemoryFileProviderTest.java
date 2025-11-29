package com.fathzer.sync4j.memory;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.File;
import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.HashAlgorithm;

class MemoryFileProviderTest {

    private MemoryFileProvider provider;

    @BeforeEach
    void setUp() {
        provider = new MemoryFileProvider();
    }

    @Test
    void testExists() throws IOException {
        //TODO Not sure having "/" as name is correct
        Entry root = provider.get("/");
        assertTrue(root.exists(), "Root should exist");
        assertTrue(root.isFolder(), "Root should be a folder");
        assertFalse(root.isFile(), "Root should not be a file");
        assertNull(root.getParent(), "Root should have no parent");
        assertNull(root.getParentPath(), "Root should have no parent path");
        assertEquals("/", root.getName(), "Root should have no name");

        Entry entry = provider.get("/nonexistent");
        assertFalse(entry.exists(), "Non-existing entry should not exist");
        assertFalse(entry.isFile(), "Non-existing entry should not be a file");
        assertFalse(entry.isFolder(), "Non-existing entry should not be a folder");

        assertEquals(Set.of(HashAlgorithm.values()), Set.copyOf(provider.getSupportedHash()));
    }

    @Test
    void testCreateFile() throws IOException {
        byte[] content = "Hello, World!".getBytes(StandardCharsets.UTF_8);
        File file = provider.createFile("/test.txt", content);

        assertTrue(file.exists(), "File should exist");
        assertTrue(file.isFile(), "Entry should be a file");
        assertFalse(file.isFolder(), "Entry should not be a folder");
        assertEquals("/test.txt", file.getName());
        assertEquals(content.length, file.getSize());

        // Verify content
        try (InputStream is = file.getInputStream()) {
            byte[] readContent = is.readAllBytes();
            assertArrayEquals(content, readContent, "Content should match");
        }

        // Trying to create a file without parent should throw IOException
        assertThrows(IOException.class,
                () -> provider.createFile("/parent/file.txt", "content".getBytes(StandardCharsets.UTF_8)),
                "Should throw IOException when parent doesn't exist");

        // Create a file at /test.txt
        provider.createFile("/test.txt", "content".getBytes(StandardCharsets.UTF_8));

        // Trying to create a file with a file as parent should throw IOException
        assertThrows(IOException.class,
                () -> provider.createFile("/test.txt/child.txt", "content".getBytes(StandardCharsets.UTF_8)),
                "Should throw IOException when parent is a file");
    }

    @Test
    void testCreateFolder() throws IOException {
        Folder folder = provider.createFolder("/testfolder");

        assertTrue(folder.exists(), "Folder should exist");
        assertTrue(folder.isFolder(), "Entry should be a folder");
        assertFalse(folder.isFile(), "Entry should not be a file");
        assertEquals("/testfolder", folder.getName());

        // Trying to create a folder without parent should throw IOException
        assertThrows(IOException.class,
                () -> provider.createFolder("/parent/subfolder"),
                "Should throw IOException when parent doesn't exist");

        // Create a file at /test.txt
        provider.createFile("/test.txt", "content".getBytes(StandardCharsets.UTF_8));

        // Trying to create a folder with a file as parent should throw IOException
        assertThrows(IOException.class,
                () -> provider.createFolder("/test.txt/child"),
                "Should throw IOException when parent is a file");
    }

    @Test
    void testFolderList() throws IOException {
        provider.createFolder("/parent");
        provider.createFile("/parent/file1.txt", "content1".getBytes(StandardCharsets.UTF_8));
        provider.createFile("/parent/file2.txt", "content2".getBytes(StandardCharsets.UTF_8));
        provider.createFolder("/parent/subfolder");

        Entry entry = provider.get("/parent");
        Folder folder = entry.asFolder();
        List<Entry> children = folder.list();

        assertEquals(3, children.size(), "Should have 3 children");
    }

    @Test
    void testFolderMkdir() throws IOException {
        provider.createFolder("/parent");
        Entry parentEntry = provider.get("/parent");
        Folder parent = parentEntry.asFolder();

        Folder newFolder = parent.mkdir("child");
        assertEquals("/parent/child", newFolder.getName());
        assertTrue(newFolder.exists());

        // Verify it's in the file system
        Entry retrieved = provider.get("/parent/child");
        assertTrue(retrieved.exists());
        assertTrue(retrieved.isFolder());

        assertThrows(IOException.class, () -> parent.mkdir("child"),
                "Should throw IOException when folder already exists");
    }

    @Test
    void testFolderCopy() throws IOException {
        provider.createFolder("/source");
        provider.createFolder("/dest");

        byte[] content = "test content".getBytes(StandardCharsets.UTF_8);
        File sourceFile = provider.createFile("/source/file.txt", content);

        Entry destEntry = provider.get("/dest");
        Folder destFolder = destEntry.asFolder();

        File copiedFile = destFolder.copy("copied.txt", sourceFile, null);
        assertEquals("/dest/copied.txt", copiedFile.getName());
        assertEquals(content.length, copiedFile.getSize());

        // Verify content
        try (InputStream is = copiedFile.getInputStream()) {
            byte[] readContent = is.readAllBytes();
            assertArrayEquals(content, readContent, "Copied content should match");
        }

        File missingFile = provider.createFile("/source/missing.txt", content);
        missingFile.delete();
        assertThrows(IOException.class, () -> destFolder.copy("copied.txt", missingFile, null), "Should throw IOException when file does not exist");
    }

    @Test
    void testDeleteFile() throws IOException {
        provider.createFile("/test.txt", "content".getBytes(StandardCharsets.UTF_8));

        File entry = provider.get("/test.txt").asFile();
        assertTrue(entry.exists());

        // When
        entry.delete();

        // Then
        Entry afterDelete = provider.get("/test.txt");
        assertFalse(afterDelete.exists(), "File should not exist after deletion");

        // Check optional behavior, delete makes File.exists() return false and some other methods throw IOException
        assertFalse(entry.exists());
        assertThrows(IOException.class, () -> entry.getSize(), "Should throw IOException when file does not exist");
        assertThrows(IOException.class, () -> entry.getCreationTime(), "Should throw IOException when file does not exist");
        assertThrows(IOException.class, () -> entry.getLastModified(), "Should throw IOException when file does not exist");
        assertThrows(IOException.class, () -> entry.getHash(HashAlgorithm.SHA256), "Should throw IOException when file does not exist");
        assertThrows(IOException.class, () -> entry.getInputStream(), "Should throw IOException when file does not exist");
    }

    @Test
    void testDeleteFolderRecursive() throws IOException {
        provider.createFolder("/parent");
        provider.createFile("/parent/file1.txt", "content1".getBytes(StandardCharsets.UTF_8));
        provider.createFolder("/parent/subfolder");
        File fileInSubfolder = provider.createFile("/parent/subfolder/file2.txt", "content2".getBytes(StandardCharsets.UTF_8));

        Folder entry = provider.get("/parent").asFolder();
        assertTrue(entry.exists());

        entry.delete();

        Entry afterDelete = provider.get("/parent");
        assertFalse(afterDelete.exists(), "Folder should not exist after deletion");

        Entry childAfterDelete = provider.get("/parent/file1.txt");
        assertFalse(childAfterDelete.exists(), "Child file should not exist after parent deletion");

        Entry subfolderAfterDelete = provider.get("/parent/subfolder");
        assertFalse(subfolderAfterDelete.exists(), "Subfolder should not exist after parent deletion");

        // Check optional behavior, delete makes Folder.exists() return false and some other methods throw IOException
        assertFalse(entry.exists());
        assertThrows(IOException.class, () -> entry.list(), "Should throw IOException when folder does not exist");
        assertThrows(IOException.class, () -> entry.mkdir("child"), "Should throw IOException when folder does not exist");
        assertThrows(IOException.class, () -> entry.copy("child", null, null), "Should throw IOException when folder does not exist");

        // Check optional behavior, previously listed entries no more exists
        assertFalse(fileInSubfolder.exists());
    }

    @Test
    void testGetParent() throws IOException {
        provider.createFolder("/parent");
        provider.createFile("/parent/file.txt", "content".getBytes(StandardCharsets.UTF_8));

        Entry file = provider.get("/parent/file.txt");
        Entry parent = file.getParent();

        assertNotNull(parent);
        assertEquals("/parent", parent.getName());
        assertTrue(parent.isFolder());
    }

    @Test
    void testGetParentPath() throws IOException {
        provider.createFolder("/parent");
        provider.createFile("/parent/file.txt", "content".getBytes(StandardCharsets.UTF_8));

        Entry file = provider.get("/parent/file.txt");
        String parentPath = file.getParentPath();

        assertEquals("/parent", parentPath);
    }

    @Test
    void testRootParent() throws IOException {
        Entry root = provider.get("/");
        assertNull(root.getParent(), "Root should have no parent");
        assertNull(root.getParentPath(), "Root should have no parent path");
    }

    @Test
    void testFileHash() throws IOException {
        byte[] content = "Hello, World!".getBytes(StandardCharsets.UTF_8);
        File file = provider.createFile("/test.txt", content);

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

        File file = provider.createFile("/test.txt", content, creationTime, lastModified);

        assertEquals(creationTime, file.getCreationTime());
        assertEquals(lastModified, file.getLastModified());
    }

    @Test
    void testPreload() throws IOException {
        assertFalse(provider.isFastListSupported());
        Folder folder = provider.createFolder("/test");
        assertThrows(UnsupportedOperationException.class, () -> folder.preload());
    }

    @Test
    void testInvalidFileName() throws IOException {
        provider.createFolder("/parent");
        Entry entry = provider.get("/parent");
        Folder folder = entry.asFolder();

        assertThrows(IllegalArgumentException.class, () -> folder.mkdir(""),
                "Should throw for empty name");
        assertThrows(IllegalArgumentException.class, () -> folder.mkdir("name/with/slash"),
                "Should throw for name with slash");
    }

    @Test
    void testAsFileAndAsFolder() throws IOException {
        File file = provider.createFile("/file.txt", "content".getBytes(StandardCharsets.UTF_8));
        Folder folder = provider.createFolder("/folder");

        assertDoesNotThrow(() -> file.asFile());
        assertThrows(IllegalStateException.class, () -> file.asFolder());

        assertDoesNotThrow(() -> folder.asFolder());
        assertThrows(IllegalStateException.class, () -> folder.asFile());
    }

    @Test
    void testReadOnly() throws IOException {
        assertTrue(provider.isReadOnlySupported(), "MemoryFileProvider should support read-only mode");
        assertFalse(provider.isReadOnly(), "Provider should not be read-only by default");

        // Create a file
        File file = provider.createFile("/test.txt", "content".getBytes(StandardCharsets.UTF_8));
        Folder root = provider.get("/").asFolder();

        // When read-only is set
        provider.setReadOnly(true);

        // All modifications should fail
        assertThrows(IOException.class, () -> provider.createFile("/other.txt", "content".getBytes(StandardCharsets.UTF_8)));
        assertThrows(IOException.class, () -> provider.createFolder("/otherfolder"));
        assertThrows(IOException.class, () -> file.delete());
        assertThrows(IOException.class, () -> root.mkdir("subfolder"));
        assertThrows(IOException.class, () -> root.copy("copy.txt", file, null));

        // But file can be read and directory listed
        try (InputStream is = file.getInputStream()) {
            byte[] readContent = is.readAllBytes();
            assertEquals("content", new String(readContent, StandardCharsets.UTF_8));
        }
        assertEquals(1, root.list().size());

        // When read-only is unset, all modifications should work
        provider.setReadOnly(false);
        assertFalse(provider.isReadOnly(), "Provider should not be read-only after unsetting");
        assertDoesNotThrow(() -> provider.createFile("/other.txt", "content".getBytes(StandardCharsets.UTF_8)));
        assertDoesNotThrow(() -> provider.createFolder("/otherfolder"));
        assertDoesNotThrow(() -> root.mkdir("subfolder"));
        assertDoesNotThrow(() -> root.copy("copy.txt", file, null));
        assertDoesNotThrow(() -> file.delete());
    }
}
