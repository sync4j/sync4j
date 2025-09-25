package com.fathzer.sync4j.file;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fathzer.sync4j.Entry;

class LocalFileTest {
    @Test
    void testGetParent() throws IOException {
        final Path path = Paths.get("toto.txt");
        Entry file = new LocalFile(path);
        assertEquals(countParent(path), countParent(file));

        // Create a tmp file
        Path tmpFilePath = Files.createTempFile("sync4j", "tmp");
        tmpFilePath.toFile().deleteOnExit();
        // Test exception is thrown when parent is a regular file
        Entry fakeFile = new LocalFile(tmpFilePath.resolve("toto.txt"));
        assertThrows (IOException.class, fakeFile::getParent);
    }

    private int countParent(Entry entry) throws IOException {
        int count = 0;
        while (entry.getParent().isPresent()) {
            entry = entry.getParent().get();
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
        Entry file = new LocalFile(path);
        for (Optional<Entry> parent = Optional.of(file); parent.isPresent(); parent = parent.get().getParent()) {
            file = parent.get();
        }
        return file;
    }
}
