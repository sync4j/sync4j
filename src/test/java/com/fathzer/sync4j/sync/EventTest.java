package com.fathzer.sync4j.sync;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import com.fathzer.sync4j.File;
import com.fathzer.sync4j.Folder;

class EventTest {

    @Test
    void testEvent() {
        Event.Action action = new Event.Action() {}; // Anonymous implementation for testing
        Event event = new Event(action, Event.Status.PLANNED);
        
        assertEquals(Event.Status.PLANNED, event.status());
        assertSame(action, event.action());

        assertThrows(NullPointerException.class, () -> new Event(null, Event.Status.PLANNED));
        assertThrows(NullPointerException.class, () -> new Event(action, null));
    }

    @Test
    void testCompareFileAction() {
        File source = mock(File.class);
        File dest = mock(File.class);
        Event.CompareFileAction action = new Event.CompareFileAction(source, dest);
        
        assertSame(source, action.source());
        assertSame(dest, action.destination());
        
        // Test null checks in constructor
        assertThrows(NullPointerException.class, () -> new Event.CompareFileAction(null, dest));
        assertThrows(NullPointerException.class, () -> new Event.CompareFileAction(source, null));
    }

    @Test
    void testCopyFileAction() {
        File source = mock(File.class);
        Folder dest = mock(Folder.class);
        Event.CopyFileAction action = new Event.CopyFileAction(source, dest);
        
        assertSame(source, action.source());
        assertSame(dest, action.destination());
        
        // Test progress listener
        final AtomicLong progress = new AtomicLong(0);
        action.setProgressListener(progress::set);
        action.progressListener().accept(500L);
        assertEquals(500L, progress.get());
        
        // Test null checks
        assertThrows(NullPointerException.class, () -> new Event.CopyFileAction(null, dest));
        assertThrows(NullPointerException.class, () -> new Event.CopyFileAction(source, null));
        assertThrows(NullPointerException.class, () -> action.setProgressListener(null));
    }

    @Test
    void testCreateFolderAction() {
        Folder parent = mock(Folder.class);
        String folderName = "newFolder";
        Event.CreateFolderAction action = new Event.CreateFolderAction(parent, folderName);
        
        assertSame(parent, action.folder());
        assertEquals(folderName, action.name());
        
        // Test null checks
        assertThrows(NullPointerException.class, () -> new Event.CreateFolderAction(null, folderName));
        assertThrows(NullPointerException.class, () -> new Event.CreateFolderAction(parent, null));
    }

    @Test
    void testDeleteEntryAction() {
        File file = mock(File.class);
        Event.DeleteEntryAction action = new Event.DeleteEntryAction(file);
        
        assertSame(file, action.entry());
        
        // Test null check
        assertThrows(NullPointerException.class, () -> new Event.DeleteEntryAction(null));
    }

    @Test
    void testListAction() {
        Folder folder = mock(Folder.class);
        Event.ListAction action = new Event.ListAction(folder);
        
        assertSame(folder, action.folder());
        
        // Test null check
        assertThrows(NullPointerException.class, () -> new Event.ListAction(null));
    }

    @Test
    void testPreloadAction() {
        Folder folder = mock(Folder.class);
        Event.PreloadAction action = new Event.PreloadAction(folder);
        
        // Verify inheritance - PreloadAction is a ListAction
        assertTrue(action instanceof Event.ListAction);
        
        // Verify folder() method works through inheritance
        assertSame(folder, action.folder());
        
        // Test null check
        assertThrows(NullPointerException.class, () -> new Event.PreloadAction(null));
    }
}