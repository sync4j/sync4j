package com.fathzer.sync4j;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fathzer.sync4j.file.LocalProvider;

public abstract class AbstractFileProviderTest {
    /** An annotation to declare the readonly support. */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface NoReadOnlySupport {
    }

    protected Folder root;
    protected Entry folder;
    protected Entry file;
    protected Entry nonExisting;
    protected FileProvider provider;

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
    void testRoot() throws IOException {
        assertTrue(root.exists(), "Root should exist");
        assertTrue(root.isFolder(), "Root should be a folder");
        assertFalse(root.isFile(), "Root should not be a file");
        assertEquals("", root.getName(), "Root should have no name");
        assertNull(root.getParent(), "Root should have no parent");
    }

    @Test
    void testGetParent() throws IOException {
        Entry parent = file.getParent();
        assertEquals("folder", parent.getName());
        parent = parent.getParent();
        assertEquals("", parent.getName());
        // Now parent is root => no parent
        assertNull(parent.getParent());

        // Test no exception is thrown when parent is a regular file
        assertDoesNotThrow(nonExisting::getParent);
    }

    @Test
    void testGetProvider() {
        assertSame(provider, root.getFileProvider());
        assertSame(provider, folder.getFileProvider());
        assertSame(provider, file.getFileProvider());
        assertSame(provider, nonExisting.getFileProvider());
    }
    
    @Test
    void testIsFileAndSimilar() {
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
    void testRootCantBeDeleted() {
        assertThrows(IOException.class, () -> root.delete());
    }

    /**
     * Test that the read-only mode is supported and works.
     * <br>By default, this test assumes that the provider supports read-only mode. Mark the test class with @NoReadOnlySupport if provider does not support read-only mode.
     */
    @Test
    protected void testReadOnly() throws IOException {
        boolean expectedReadOnlySupported = !this.getClass().isAnnotationPresent(NoReadOnlySupport.class);
        assertEquals(expectedReadOnlySupported, provider.isReadOnlySupported());
        assertFalse(provider.isReadOnly(), "Provider should not be read-only by default");
        if (!expectedReadOnlySupported) {
            return;
        }
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
