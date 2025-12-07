package com.fathzer.sync4j;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import com.fathzer.sync4j.file.LocalProvider;

public abstract class AbstractFileProviderTest {
    protected Folder root;
    protected Entry folder;
    protected Entry file;
    protected Entry nonExisting;
    protected LocalProvider provider;

    protected abstract FileProvider createFileProvider();

    protected abstract void createFile(String path) throws IOException;

    protected abstract void createFolder(String path) throws IOException;

    @BeforeEach
    void setup() throws IOException {
        createFolder("folder");
        createFile("file.txt");
        createFile("folder/file.txt");

        provider = (LocalProvider) createFileProvider();
        root = provider.get(FileProvider.ROOT_PATH).asFolder();
        folder = provider.get("/folder");
        file = provider.get("/folder/file.txt");
        nonExisting = provider.get("/nonExisting");
    }

    @AfterEach
    void teardown() {
        root.getFileProvider().close();
    }

    protected File createMockFile(String content) throws IOException {
        File result = Mockito.mock(File.class);
        when(result.getInputStream()).thenReturn(new ByteArrayInputStream(content.getBytes()));
        return result;
    }

    @Test
    void testGetParent() throws IOException {
        assertNull(root.getParent());
        Entry parent = file.getParent();
        assertEquals("folder", parent.getName());
        parent = parent.getParent();
        assertEquals("", parent.getName());
        assertNull(parent.getParent());

        // Test no exception is thrown when parent is a regular file
        assertDoesNotThrow(nonExisting::getParent);
    }

    @Test
    void testGetProvider() throws IOException {
        assertSame(provider, root.getFileProvider());
        assertSame(provider, folder.getFileProvider());
        assertSame(provider, file.getFileProvider());
        assertSame(provider, nonExisting.getFileProvider());
    }

    
    @Test
    void testIsFileAndSimilar() throws IOException {
        assertTrue(root.exists());
        assertTrue(root.isFolder());
        assertFalse(root.isFile());
        assertSame(root, root.asFolder());

        assertTrue(file.exists());
        assertTrue(file.isFile());
        assertFalse(file.isFolder());
        assertSame(file, file.asFile());
        assertThrows(IllegalStateException.class, () -> file.asFolder());

        assertTrue(folder.exists());
        assertTrue(folder.isFolder());
        assertFalse(folder.isFile());
        assertSame(folder, folder.asFolder());
        assertThrows(IllegalStateException.class, () -> folder.asFile());

        assertFalse(nonExisting.exists());
        assertFalse(nonExisting.isFolder());
        assertFalse(nonExisting.isFile());
        assertThrows(IllegalStateException.class, () -> nonExisting.asFolder());
        assertThrows(IllegalStateException.class, () -> nonExisting.asFile());
    }

    @Test
    void testRootCantBeDeleted(@TempDir Path tempDir) throws IOException {
        try (FileProvider provider = new LocalProvider(tempDir)) {
            Entry root = provider.get(FileProvider.ROOT_PATH);
            assertThrows(IOException.class, () -> root.delete());
        }
    }

    /**
     * Test that the read-only mode is supported and works.
     * <br>To exclude this test (typically if provider does not support read-only mode), override this method in the test super class.
     */
    @Test
    protected void testReadOnly() throws IOException {
        assertTrue(provider.isReadOnlySupported(), "MemoryFileProvider should support read-only mode");
        assertFalse(provider.isReadOnly(), "Provider should not be read-only by default");
        int initialSize = root.list().size();

        // Create a new file
        File testFile = root.copy("test.txt", createMockFile("content"), null);

        // When read-only is set
        provider.setReadOnly(true);

        // All modifications should fail
        assertThrows(IOException.class, testFile::delete);
        assertThrows(IOException.class, () -> root.mkdir("subfolder"));
        assertThrows(IOException.class, () -> root.copy("copy.txt", testFile, null));

        // But file can be read and directory listed
        try (InputStream is = testFile.getInputStream()) {
            byte[] readContent = is.readAllBytes();
            assertEquals("content", new String(readContent, StandardCharsets.UTF_8));
        }
        assertEquals(initialSize + 1, root.list().size());

        // When read-only is unset, all modifications should work
        provider.setReadOnly(false);
        assertFalse(provider.isReadOnly(), "Provider should not be read-only after unsetting");
        assertDoesNotThrow(() -> root.mkdir("subfolder"));
        assertDoesNotThrow(() -> root.copy("copy.txt", testFile, null));
        assertDoesNotThrow(testFile::delete);
    }
}
