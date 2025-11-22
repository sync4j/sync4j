package com.fathzer.sync4j.sync;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import com.fathzer.sync4j.Folder;

class FoldersTest {
    @Test
    void test() {
        Folders folders = new Folders(null, null);
        assertNull(folders.source);
        assertNull(folders.destination);

        Folder source = mock(Folder.class);
        Folder destination = mock(Folder.class);
        folders = new Folders(source, destination);
        assertSame(source, folders.source);
        assertSame(destination, folders.destination);
    }

}
