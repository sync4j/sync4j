package com.fathzer.sync4j.file;

import static org.junit.jupiter.api.Assertions.*;
import static com.fathzer.sync4j.file.LocalProvider.INSTANCE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fathzer.sync4j.Entry;

class LocalFileTest {
    @TempDir
    private static Path tempDir;

    private static Entry folder;
    private static Entry file;
    private static Entry nonExisting;

    @BeforeAll
    static void setup() throws IOException {
        String path = tempDir.toString();
        folder = INSTANCE.get(Files.createDirectory(tempDir.resolve("folder")).toString());
        file = INSTANCE.get(Files.createFile(tempDir.resolve("file.txt")).toString());
        nonExisting = INSTANCE.get(path + "/nonExisting");
    }

    @Test
    void testGetParent() throws IOException {
        assertEquals(countParent(tempDir.resolve("file.txt")), countParent(file));

        // Test no exception is thrown when parent is a regular file
        assertDoesNotThrow (nonExisting::getParent);
    }

    @Test
    void testGetParentPath() throws IOException {
        String expected = tempDir.resolve("file.txt").toAbsolutePath().getParent().toString();
        assertEquals(expected, file.getParentPath());

        Entry root = getRoot();
        assertEquals(null, root.getParentPath());
    }

    @Test
    void testGetProvider() throws IOException {
        Entry root = getRoot();
        assertSame(INSTANCE, root.getFileProvider());
        assertSame(INSTANCE, folder.getFileProvider());
        assertSame(INSTANCE, file.getFileProvider());
        assertSame(INSTANCE, nonExisting.getFileProvider());
    }

    @Test
    void testIsFileAndSimilar() throws IOException {
        Entry root = getRoot();
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

    private int countParent(Entry entry) throws IOException {
        int count = 0;
        while (entry.getParent()!=null) {
            entry = entry.getParent();
            count++;
        }
        return count;
    }

    private int countParent(Path path) {
        int count = 0;
        path = path.toAbsolutePath();
        while (path.getParent() != null) {
            path = path.getParent();
            count++;
        }
        return count;
    }

    @Test
    void testGetName() throws IOException {
        // Check that getName() does not throw exception when called on root file
        Entry root = getRoot();
        assertEquals("", root.getName());
    }

    private Entry getRoot() throws IOException {
        final Path path = Paths.get("toto.txt");
        Entry file = INSTANCE.get(path.toString());
        for (Entry parent = file; parent!=null; parent = parent.getParent()) {
            file = parent;
        }
        return file;
    }
}
