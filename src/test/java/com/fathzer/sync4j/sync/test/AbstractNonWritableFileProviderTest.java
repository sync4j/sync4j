package com.fathzer.sync4j.sync.test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.File;
import com.fathzer.sync4j.Folder;

public abstract class AbstractNonWritableFileProviderTest extends AbstractFileProviderTest {

    @Override
    protected UnderlyingFileSystem getUnderlyingFileSystem() {
        return null;
    }

    @Override
    @Test
    protected void testGet() {
        assertThrows(IllegalArgumentException.class, () -> provider.get("/folder//file.txt"), "Invalid path should not be retrieved");
        assertThrows(IllegalArgumentException.class, () -> provider.get("folder/file.txt"), "Invalid path should not be retrieved");
        System.err.println("WARNING: testGet() is not implemented for inconsistent path is not implemented");
    }





    @Override
    @Test
    protected void testWriteSupported() {
        assertFalse(provider.isWriteSupported());
    }

    @Override
    @Test
    protected void testReadOnlyMode() throws IOException {
        assertTrue(provider.isReadOnly(), "Provider should be read-only");
        // All modifications should fail
        assertThrows(IOException.class, () -> root.mkdir("subfolder"));
        File mockedFile = createMockFile("content");
        assertThrows(IOException.class, () -> root.copy("copy.txt", mockedFile, null));
        File availableFile = searchFor(root, Entry::isFile, "file", "delete is throwing exception").asFile();
        assertThrows(IOException.class, availableFile::delete);
    }
    
    private Entry searchFor(Folder folder, Predicate<Entry> filter, String what, String why) throws IOException {
        Entry entry = searchFor(folder, filter);
        if (entry == null) {
            fail("No " + what + " found in the provider - No way to test " + why);
        }
        return entry;
    }

    private Entry searchFor(Folder folder, Predicate<Entry> filter) throws IOException {
        for (Entry entry : folder.list()) {
            if (filter.test(entry)) {
                return entry;
            } else {
                Entry fileInSubfolder = searchFor(entry.asFolder(), filter);
                if (fileInSubfolder != null) {
                    return fileInSubfolder;
                }
            }
        }
        return null;
    }
}
