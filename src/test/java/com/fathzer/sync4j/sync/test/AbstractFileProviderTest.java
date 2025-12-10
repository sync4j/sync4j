package com.fathzer.sync4j.sync.test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.File;
import com.fathzer.sync4j.FileProvider;
import com.fathzer.sync4j.Folder;

public abstract class AbstractFileProviderTest {
    /** An annotation to declare there's no write support expected. */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface NoWriteSupport {
    }

    /**
     * The underlying file system.
     * <br>This interface is used to assert that the file provider and the underlying file system are in sync.
     */
    public interface UnderlyingFileSystem {
        /**
         * Create a file.
         * @param path the path of the file to create (relative to the root folder - e.g. "/folder/file.txt")
         * @throws IOException if an I/O error occurs
         */
        void createFile(String path) throws IOException;
        
        /**
         * Delete a file or a folder.
         * <br>If called on a folder, the implementor can assume that the folder is empty.
         * @param path the path of the file or folder to delete (relative to the root folder - e.g. "/folder/file.txt")
         * @throws IOException if an I/O error occurs
         */
        void delete(String path) throws IOException;
        
        /**
         * Create a folder.
         * @param path the path of the folder to create (relative to the root folder - e.g. "/folder")
         * @throws IOException if an I/O error occurs
         */
        void createFolder(String path) throws IOException;
        
        /**
         * Assert that the file exists and its content is equal to the given file.
         * @param path the path of the file to assert (relative to the root folder - e.g. "/folder/file.txt")
         * @param file the file to compare to
         * @throws IOException if an I/O error occurs
         */
        void assertUnderlyingFileEquals(String path, File file) throws IOException;
        
        /**
         * Assert that the folder exists.
         * @param path the path of the folder to assert (relative to the root folder - e.g. "/folder")
         * @throws IOException if an I/O error occurs
         */
        void assertUnderlyingFolderExist(String path) throws IOException;
    }

    /** The root folder.
     * <br>This folder is created in the setup method.
     */
    protected Folder root;
    /** The file provider.
     * <br>This provider is created in the setup method.
     */
    protected FileProvider provider;
    /** The underlying file system.
     * <br>This file system is created in the setup method by calling {@link #getUnderlyingFileSystem()}. 
     */
    protected UnderlyingFileSystem ufs;

    protected abstract FileProvider createFileProvider();

    /**
     * Returns the underlying file system on which the file provider is based.
     * @return the underlying file system, or null if the provider doesn't reflect an underlying file system (for instance for in-memory providers)
     */
    protected abstract UnderlyingFileSystem getUnderlyingFileSystem();

    @BeforeEach
    protected void setup() throws IOException {
        provider = createFileProvider();
        root = provider.get(FileProvider.ROOT_PATH).asFolder();
        ufs = getUnderlyingFileSystem();
    }

    @AfterEach
    void teardown() {
        root.getFileProvider().close();
    }

    protected File createMockFile(String content) throws IOException {
        File result = Mockito.mock(File.class);
        Mockito.lenient().when(result.getInputStream()).thenReturn(new ByteArrayInputStream(content.getBytes()));
        return result;
    }

    @Test
    protected void testRoot() throws IOException {
        assertTrue(root.exists(), "Root should exist");
        assertTrue(root.isFolder(), "Root should be a folder");
        assertFalse(root.isFile(), "Root should not be a file");
        assertEquals("", root.getName(), "Root should have no name");
        assertNull(root.getParent(), "Root should have no parent");
        assertThrows(IOException.class, () -> root.delete(), "Root should not be deleted");
    }

    @Test
    protected void testGet() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> provider.get("/folder//file.txt"), "Invalid path should not be retrieved");
        assertThrows(IllegalArgumentException.class, () -> provider.get("folder/file.txt"), "Invalid path should not be retrieved");
        
        // Check inconsistent path does not throw any exception and returns a non existing entry
        root.copy("file.txt", createMockFile("content"), null);
        Entry file = provider.get("/file.txt/toto.txt");
        assertFalse(file.exists());
    }

    @Test
    protected void testGetParent() throws IOException {
    	// Check get parent on missing file does not throw IOException
        Entry file = provider.get("/folder/file.txt");
        assertFalse(file.exists());
        assertFalse(provider.get("/folder").exists());
        assertDoesNotThrow(file::getParent, "Parent of a missing file should be retrieved");
        
        // Check get parent on existing file
        root.mkdir("folder").copy("file.txt", createMockFile("content"), null);
        file = provider.get("/folder/file.txt");
        assertTrue(file.exists());
        Entry parent = file.getParent();
        assertEquals("folder", parent.getName());
        parent = parent.getParent();
        assertEquals("", parent.getName());
        // Now parent is root => no parent
        assertNull(parent.getParent());

        // Check parent on missing entry with a file as parent
        file = provider.get("/folder/file.txt/missing");
        parent = file.getParent();
        assertTrue(parent.isFile());
    }

    @Test
    protected void testGetProvider() throws IOException {
        assertSame(provider, root.getFileProvider());
        Folder folder = root.mkdir("folder");
        assertSame(provider, folder.getFileProvider());
        File file = folder.copy("file.txt", createMockFile("content"), null);
        assertSame(provider, file.getFileProvider());
    }

    /**
     * Tests that the file provider reflects the underlying file system.
     * <br>This test is skipped if the provider has no underlying file system.
     * <br>Please note that this test doesn't test that existing entries reflect the underlying file system changes, because this is not required by the sync4j API.
     * @throws IOException if an I/O error occurs
     */
    @Test
    protected void testReflectsUnderlyingFileSystem() throws IOException {
        assumeTrue(ufs != null, "Test skipped because provider has no underlying filesystem");

        // Test folder creation is reflected in entries returned by the provider
        ufs.createFolder("/folder");
        Folder folder = provider.get("/folder").asFolder();
        assertTrue(folder.exists());
        assertTrue(folder.list().isEmpty());
        assertEquals(List.of("folder"), provider.get(FileProvider.ROOT_PATH).asFolder().list().stream().map(Entry::getName).toList());

        // Test file creation is reflected in entries returned by the provider
        ufs.createFile("/file.txt");
        File file = provider.get("/file.txt").asFile();
        assertTrue(file.exists());
        ufs.assertUnderlyingFileEquals("file.txt", file);
        assertEquals(2, provider.get(FileProvider.ROOT_PATH).asFolder().list().size());

        ufs.createFile("/folder/file.txt");
        file = provider.get("/folder/file.txt").asFile();
        ufs.assertUnderlyingFileEquals("folder/file.txt", file);
        assertEquals(1, provider.get("/folder").asFolder().list().size());

        // Test non existing entry
        assertFalse(provider.get("/nonExisting").exists());

        // Test file deletion is reflected in entries returned by the provider
        ufs.delete("/file.txt");
        assertFalse(provider.get("/file.txt").exists());
        assertEquals(1, provider.get(FileProvider.ROOT_PATH).asFolder().list().size());

        // Test folder deletion is reflected in entries returned by the provider
        ufs.delete("/folder/file.txt");
        ufs.delete("/folder");
        assertFalse(provider.get("/folder").exists());
        assertEquals(0, provider.get(FileProvider.ROOT_PATH).asFolder().list().size());
    }

    @Test
    void testChangesUnderlyingFileSystem() throws IOException {
        assumeTrue(ufs != null, "Test skipped because provider has no underlying filesystem");


//fail("Not implemented");
        // TODO
    }
    
    @Test
    void testIsFileAndSimilar() throws IOException {
        File file = root.copy("file.txt", createMockFile("content"), null);
        assertTrue(file.exists());
        assertTrue(file.isFile());
        assertFalse(file.isFolder());
        assertSame(file, file.asFile());
        assertThrows(IllegalStateException.class, file::asFolder);

        Folder folder = root.mkdir("folder");
        assertTrue(folder.exists());
        assertTrue(folder.isFolder());
        assertFalse(folder.isFile());
        assertSame(folder, folder.asFolder());
        assertThrows(IllegalStateException.class, folder::asFile);

        Entry nonExisting = provider.get("/nonExisting");
        assertFalse(nonExisting.exists());
        assertFalse(nonExisting.isFolder());
        assertFalse(nonExisting.isFile());
        assertThrows(IllegalStateException.class, nonExisting::asFolder);
        assertThrows(IllegalStateException.class, nonExisting::asFile);
    }

    @Test
    protected void testWriteSupported() {
        assertTrue(provider.isWriteSupported());
    }

    /**
     * Test the read-only mode.
     */
    @Test
    protected void testReadOnlyMode() throws IOException {
        assertFalse(provider.isReadOnly(), "Provider should not be read-only by default");
        int initialSize = root.list().size();

        // Create a new file
        File testFile = root.copy("test.txt", createMockFile("content"), null);

        // When read-only is set
        provider.setReadOnly(true);
        assertEquals(initialSize + 1, root.list().size());
        // But file can be read and directory listed
        try (InputStream is = testFile.getInputStream()) {
            byte[] readContent = is.readAllBytes();
            assertEquals("content", new String(readContent, StandardCharsets.UTF_8));
        }

        // All modifications should fail
        assertTrue(provider.isReadOnly(), "Provider should be read-only");
        // All modifications should fail
        assertThrows(IOException.class, () -> root.mkdir("subfolder"));
        File mockedFile = createMockFile("content");
        assertThrows(IOException.class, () -> root.copy("copy.txt", mockedFile, null));
        assertThrows(IOException.class, testFile::delete);

        // When read-only is unset, all modifications should work
        provider.setReadOnly(false);
        assertFalse(provider.isReadOnly(), "Provider should not be read-only after unsetting");
        assertDoesNotThrow(() -> root.mkdir("subfolder"));
        assertDoesNotThrow(() -> root.copy("copy.txt", testFile, null));
        assertDoesNotThrow(testFile::delete);
    }
}
