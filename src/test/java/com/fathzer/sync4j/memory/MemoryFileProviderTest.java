package com.fathzer.sync4j.memory;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.File;
import com.fathzer.sync4j.FileProvider;
import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.HashAlgorithm;

class MemoryFileProviderTest {

    private MemoryFileProvider provider;
    private MemoryFolder root;

    @BeforeEach
    void setUp() throws IOException {
        provider = new MemoryFileProvider();
        root = (MemoryFolder) provider.get(MemoryFileProvider.ROOT_PATH);
    }

    @Test
    void testRoot() throws IOException {
        assertTrue(root.exists(), "Root should exist");
        assertTrue(root.isFolder(), "Root should be a folder");
        assertFalse(root.isFile(), "Root should not be a file");
        assertEquals("", root.getName(), "Root should have no name");
        assertNull(root.getParentPath(), "Root should have no parent path");
        assertNull(root.getParent(), "Root should have no parent");
    }

    @Test
    void testGetProvider() throws IOException {
        assertSame(provider, root.getFileProvider());

        FileProvider fileProvider = root.getFileProvider();
        assertSame(fileProvider, fileProvider.get("/unknown.txt").getFileProvider());
    }

    @Test
    void testNonExistingEntry() throws IOException {
        Entry entry = provider.get("/nonexistent");
        assertFalse(entry.exists(), "Non-existing entry should not exist");
        assertFalse(entry.isFile(), "Non-existing entry should not be a file");
        assertFalse(entry.isFolder(), "Non-existing entry should not be a folder");

        // Create a file at /test.txt
        root.createFile("test.txt", "content".getBytes(StandardCharsets.UTF_8));

        // Try to get /test.txt/something (treating a file as if it were a folder)
        entry = provider.get("/test.txt/something");

        // Should return a non-existing entry
        assertFalse(entry.exists(), "Entry should not exist when intermediate path is a file");
    }

    @Test
    void testSupportedHash() {
        assertEquals(List.of(HashAlgorithm.values()), provider.getSupportedHash());
    }

    @Test
    void testCreateFile() throws IOException {
        byte[] content = "Hello, World!".getBytes(StandardCharsets.UTF_8);
        File file = root.createFile("test.txt", content);

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
        assertThrows(IOException.class, () -> root.createFile("test.txt", "content".getBytes(StandardCharsets.UTF_8)));

        // Trying to create a file at a folder location should throw IOException
        root.mkdir("folder");
        assertThrows(IOException.class,
                () -> root.createFile("folder", "content".getBytes(StandardCharsets.UTF_8)),
                "Should throw IOException when path is a folder");
    }

    @Test
    void testFolderList() throws IOException {
        MemoryFolder parent = (MemoryFolder) root.mkdir("parent");
        parent.createFile("file1.txt", "content1".getBytes(StandardCharsets.UTF_8));
        parent.createFile("file2.txt", "content2".getBytes(StandardCharsets.UTF_8));
        parent.mkdir("subfolder");

        Entry entry = provider.get("/parent");
        Folder folder = entry.asFolder();
        List<Entry> children = folder.list();

        assertEquals(3, children.size(), "Should have 3 children");
    }

    @Test
    void testFolderMkdir() throws IOException {
        Folder parent = root.mkdir("parent");

        Folder newFolder = parent.mkdir("child");
        assertEquals("child", newFolder.getName());
        assertEquals("/parent", newFolder.getParentPath());
        assertSame(parent, newFolder.getParent());
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
        MemoryFolder source = (MemoryFolder) root.mkdir("source");
        MemoryFolder dest = (MemoryFolder) root.mkdir("dest");

        byte[] content = "test content".getBytes(StandardCharsets.UTF_8);
        MemoryFile sourceFile = source.createFile("file.txt", content);
        sourceFile.setCreationTime(123456789);
        sourceFile.setLastModifiedTime(167654321);

        File copiedFile = dest.copy("copied.txt", sourceFile, null);
        assertEquals("copied.txt", copiedFile.getName());
        assertEquals(content.length, copiedFile.getSize());
        assertEquals(sourceFile.getCreationTime(), copiedFile.getCreationTime());
        assertEquals(sourceFile.getLastModifiedTime(), copiedFile.getLastModifiedTime());

        // Verify content
        try (InputStream is = copiedFile.getInputStream()) {
            byte[] readContent = is.readAllBytes();
            assertArrayEquals(content, readContent, "Copied content should match");
        }

        File missingFile = source.createFile("missing.txt", content);
        missingFile.delete();
        assertThrows(IOException.class, () -> dest.copy("copied.txt", missingFile, null), "Should throw IOException when file does not exist");
    }

    @Test
    void testDeleteFile() throws IOException {
        File entry = root.createFile("test.txt", "content".getBytes(StandardCharsets.UTF_8));

        assertTrue(entry.exists());

        // When
        entry.delete();

        // Then
        Entry afterDelete = provider.get("/test.txt");
        assertFalse(afterDelete.exists(), "File should not exist after deletion");

        // Check optional behavior, delete makes File.exists() return false and some other methods throw IOException
        assertFalse(entry.exists());
        assertThrows(IOException.class, entry::getSize, "Should throw IOException when file does not exist");
        assertThrows(IOException.class, entry::getCreationTime, "Should throw IOException when file does not exist");
        assertThrows(IOException.class, entry::getLastModifiedTime, "Should throw IOException when file does not exist");
        assertThrows(IOException.class, () -> entry.getHash(HashAlgorithm.SHA256), "Should throw IOException when file does not exist");
        assertThrows(IOException.class, entry::getInputStream, "Should throw IOException when file does not exist");
    }

    @Test
    void testDeleteFolder() throws IOException {
        MemoryFolder parent = (MemoryFolder) root.mkdir("parent");
        parent.createFile("file1.txt", "content1".getBytes(StandardCharsets.UTF_8));
        MemoryFolder subfolder = (MemoryFolder) parent.mkdir("subfolder");
        File fileInSubfolder = subfolder.createFile("file2.txt", "content2".getBytes(StandardCharsets.UTF_8));

        assertTrue(parent.exists());

        parent.delete();

        Entry afterDelete = provider.get("/parent");
        assertFalse(afterDelete.exists(), "Folder should not exist after deletion");

        Entry childAfterDelete = provider.get("/parent/file1.txt");
        assertFalse(childAfterDelete.exists(), "Child file should not exist after parent deletion");

        Entry subfolderAfterDelete = provider.get("/parent/subfolder");
        assertFalse(subfolderAfterDelete.exists(), "Subfolder should not exist after parent deletion");

        // Check optional behavior, delete makes Folder.exists() return false and some other methods throw IOException
        assertFalse(parent.exists());
        assertThrows(IOException.class, parent::list, "Should throw IOException when folder does not exist");
        assertThrows(IOException.class, () -> parent.mkdir("child"), "Should throw IOException when folder does not exist");
        assertThrows(IOException.class, () -> parent.copy("child", null, null), "Should throw IOException when folder does not exist");
        assertThrows(IOException.class, () -> parent.createFile("child", null), "Should throw IOException when folder does not exist");

        // Check optional behavior, previously listed entries no more exists
        assertFalse(subfolder.exists());
        assertFalse(fileInSubfolder.exists());

        // Check root folder can't be deleted
        assertThrows(IOException.class, () -> root.delete(), "Should throw IOException when root folder is deleted");
    }

    @Test
    void testGetParent() throws IOException {
        MemoryFolder parent = (MemoryFolder) root.mkdir("parent");
        parent.createFile("file.txt", "content".getBytes(StandardCharsets.UTF_8));

        Entry file = provider.get("/parent/file.txt");
        Entry entry = file.getParent();

        assertNotNull(entry);
        assertEquals("parent", entry.getName());
        assertTrue(entry.isFolder());
    }

    @Test
    void testGetParentPath() throws IOException {
        MemoryFolder parent = (MemoryFolder) root.mkdir("parent");
        parent.createFile("file.txt", "content".getBytes(StandardCharsets.UTF_8));

        Entry file = provider.get("/parent/file.txt");
        String parentPath = file.getParentPath();

        assertEquals("/parent", parentPath);
    }

    @Test
    void testFileHash() throws IOException {
        byte[] content = "Hello, World!".getBytes(StandardCharsets.UTF_8);
        File file = root.createFile("test.txt", content);

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

        MemoryFile file = root.createFile("test.txt", content);
        file.setCreationTime(creationTime);
        file.setLastModifiedTime(lastModified);

        assertEquals(creationTime, file.getCreationTime());
        assertEquals(lastModified, file.getLastModifiedTime());
    }

    @Test
    void testPreload() throws IOException {
        assertFalse(provider.isFastListSupported());
        Folder folder = root.mkdir("test");
        assertThrows(UnsupportedOperationException.class, folder::preload);
    }

    @Test
    void testInvalidFileName() throws IOException {
        root.mkdir("parent");
        Entry entry = provider.get("/parent");
        Folder folder = entry.asFolder();

        assertThrows(IllegalArgumentException.class, () -> folder.mkdir(""), "Should throw for empty name");
        assertThrows(IllegalArgumentException.class, () -> folder.mkdir("name/with/slash"), "Should throw for name with slash");

        assertThrows(IllegalArgumentException.class, () -> provider.get("notStartingWithSlash"), "Should throw for name not starting with slash");
    }

    @Test
    void testAsFileAndAsFolder() throws IOException {
        File file = root.createFile("file.txt", "content".getBytes(StandardCharsets.UTF_8));
        Folder folder = root.mkdir("folder");

        assertDoesNotThrow(file::asFile);
        assertThrows(IllegalStateException.class, file::asFolder);

        assertDoesNotThrow(folder::asFolder);
        assertThrows(IllegalStateException.class, folder::asFile);
    }

    @Test
    void testReadOnly() throws IOException {
        assertTrue(provider.isReadOnlySupported(), "MemoryFileProvider should support read-only mode");
        assertFalse(provider.isReadOnly(), "Provider should not be read-only by default");

        // Create a file
        MemoryFile file = root.createFile("test.txt", "content".getBytes(StandardCharsets.UTF_8));

        // When read-only is set
        provider.setReadOnly(true);

        // All modifications should fail
        assertThrows(IOException.class, () -> root.createFile("other.txt", "content".getBytes(StandardCharsets.UTF_8)));
        assertThrows(IOException.class, () -> root.mkdir("otherfolder"));
        assertThrows(IOException.class, file::delete);
        assertThrows(IOException.class, () -> root.mkdir("subfolder"));
        assertThrows(IOException.class, () -> root.copy("copy.txt", file, null));
        assertThrows(IOException.class, () -> file.setContent("new content".getBytes(StandardCharsets.UTF_8)));

        // But file can be read and directory listed
        try (InputStream is = file.getInputStream()) {
            byte[] readContent = is.readAllBytes();
            assertEquals("content", new String(readContent, StandardCharsets.UTF_8));
        }
        assertEquals(1, root.list().size());

        // When read-only is unset, all modifications should work
        provider.setReadOnly(false);
        assertFalse(provider.isReadOnly(), "Provider should not be read-only after unsetting");
        assertDoesNotThrow(() -> root.createFile("other.txt", "content".getBytes(StandardCharsets.UTF_8)));
        assertDoesNotThrow(() -> root.mkdir("otherfolder"));
        assertDoesNotThrow(() -> root.mkdir("subfolder"));
        assertDoesNotThrow(() -> root.copy("copy.txt", file, null));
        assertDoesNotThrow(file::delete);
        assertDoesNotThrow(() -> file.setContent("new content".getBytes(StandardCharsets.UTF_8)));
    }
}
