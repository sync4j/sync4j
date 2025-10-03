package com.fathzer.sync4j.file;

import static org.junit.jupiter.api.Assertions.*;
import static com.fathzer.sync4j.file.LocalProvider.INSTANCE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import com.fathzer.sync4j.Entry;

class LocalFileTest {
    @Test
    void testGetParent() throws IOException {
        final Path path = Paths.get("toto.txt");
        Entry file = INSTANCE.get(path.toString());
        assertEquals(countParent(path), countParent(file));

        // Create a tmp file
        Path tmpFilePath = Files.createTempFile("sync4j", "tmp");
        tmpFilePath.toFile().deleteOnExit();
        // Test exception is thrown when parent is a regular file
        Entry fakeFile = INSTANCE.get(tmpFilePath.resolve("toto.txt").toString());
        assertThrows (IOException.class, fakeFile::getParent);
    }

    @Test
    void testGetParentPath() throws IOException {
        final Path path = Paths.get("toto.txt");
        Entry file = INSTANCE.get(path.toString());
        String expected = path.toAbsolutePath().getParent().toString();
        assertEquals(expected, file.getParentPath());

        Entry root = getRoot();
        assertEquals(null, root.getParentPath());
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
