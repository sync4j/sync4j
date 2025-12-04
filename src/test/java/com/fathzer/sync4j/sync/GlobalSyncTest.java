package com.fathzer.sync4j.sync;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.File;
import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.memory.MemoryFile;
import com.fathzer.sync4j.memory.MemoryFileProvider;
import com.fathzer.sync4j.memory.MemoryFolder;
import com.fathzer.sync4j.sync.Event.Action;
import com.fathzer.sync4j.sync.parameters.SyncParameters;

class GlobalSyncTest {
    private MemoryFolder source;
    private MemoryFolder destination;
    private List<Event> events;
    private SyncParameters parameters;

    @SuppressWarnings("resource")
    @BeforeEach
    void setUp() throws IOException {
        source = (MemoryFolder) new MemoryFileProvider().get("").asFolder();
        destination = (MemoryFolder) new MemoryFileProvider().get("").asFolder();
        events = new LinkedList<>();
        parameters = new SyncParameters();
        parameters.eventListener(events::add);
    }

    @AfterEach
    void tearDown() {
        source.getFileProvider().close();
        destination.getFileProvider().close();
    }
    
    @Test
    void test() throws Exception {
        parameters.errorManager((e, action) -> true);

        // Build source structure
        MemoryFolder folder = (MemoryFolder) source.mkdir("folder").asFolder();
        MemoryFolder subSubFolder = folder.mkdir("sub-sub-folder");
        folder.mkdir("another-sub-folder");
        source.createFile("same.txt", "same".getBytes());
        source.createFile("missing.txt", "missing".getBytes());
        source.createFile("notSameContent.txt", "notSameContentSource".getBytes());
        subSubFolder.createFile("sub-file.txt", "sub-file".getBytes());

        // Sync to non existing destination => Should fail
        Folder nonExisting = destination.mkdir("nonExisting");
        nonExisting.delete();
        try (Synchronization synchronization = new Synchronization(source, nonExisting, parameters)) {
            execute(synchronization);
            assertEquals(1, synchronization.getErrors().size(), "One error expected on non existing destination but was " + synchronization.getErrors());
            assertTrue(events.stream().anyMatch(e -> e.status() == Event.Status.FAILED), "One failed event expected but was " + events);
        }
        events.clear();

        // Sync to read only existing destination => Should fail
        destination.getFileProvider().setReadOnly(true);
        try (Synchronization synchronization = new Synchronization(source, destination, parameters)) {
            execute(synchronization);
            assertFalse(synchronization.getErrors().isEmpty(), "At least one error expected on read only existing destination but the was none");
            assertTrue(events.stream().anyMatch(e -> e.status() == Event.Status.FAILED), "One failed event expected but was " + events);
        }
        events.clear();
        destination.getFileProvider().setReadOnly(false);

        // Sync readonly source to existing destination => Should succeed
        source.getFileProvider().setReadOnly(true);
        try (Synchronization synchronization = new Synchronization(source, destination, parameters)) {
            execute(synchronization);
            assertTrue(synchronization.getErrors().isEmpty(), "No error expected on existing destination but was " + synchronization.getErrors());
            assertFalse(events.stream().anyMatch(e -> e.status() == Event.Status.FAILED), "No failed event expected but found "
                            + events.stream().filter(e -> e.status() == Event.Status.FAILED).toList());
            assertEquals(new Statistics.Counter(4,4), synchronization.getStatistics().copiedFiles());
            assertEquals(new Statistics.Counter(39,39), synchronization.getStatistics().copiedBytes());
            assertEquals(new Statistics.Counter(3,3), synchronization.getStatistics().createdFolders());
            assertEquals(new Statistics.Counter(0,0), synchronization.getStatistics().deletedFolders());
            assertEquals(new Statistics.Counter(0,0), synchronization.getStatistics().deletedFiles());
        }
        // Check all actions where planned, executed and are successfull
        List<Action> actions = events.stream().map(Event::action).toList();
        actions.forEach(a -> assertEquals(List.of(Event.Status.PLANNED, Event.Status.STARTED, Event.Status.COMPLETED), getStatuses(events, a), "Create actions are not successful"));
        checkSame(source, destination);

        events.clear();
        // Perform some changes in source
        source.getFileProvider().setReadOnly(false);
        source.mkdir("not in destination folder");
        MemoryFile sameFile = (MemoryFile) source.getFileProvider().get("/same.txt").asFile();
        sameFile.setLastModifiedTime(sameFile.getLastModifiedTime() + 1000);
        source.createFile("new.txt", "new".getBytes());
        source.getFileProvider().get("/missing.txt").delete();
        // Perform some changes in destination
        destination.createFile("not in source.txt", "new".getBytes());
        destination.mkdir("not in source folder");

        try (Synchronization synchronization = new Synchronization(source, destination, parameters)) {
            execute(synchronization);
            assertTrue(synchronization.getErrors().isEmpty(), "No error expected on existing destination but was " + synchronization.getErrors());
            assertFalse(events.stream().anyMatch(e -> e.status() == Event.Status.FAILED), "No failed event expected but found "
                            + events.stream().filter(e -> e.status() == Event.Status.FAILED).toList());
            assertEquals(new Statistics.Counter(2,2), synchronization.getStatistics().copiedFiles());
            assertEquals(new Statistics.Counter(7,7), synchronization.getStatistics().copiedBytes());
            assertEquals(new Statistics.Counter(1,1), synchronization.getStatistics().createdFolders());
            assertEquals(new Statistics.Counter(1,1), synchronization.getStatistics().deletedFolders());
            assertEquals(new Statistics.Counter(2,2), synchronization.getStatistics().deletedFiles());
        }
    }
    
    private List<Event.Status> getStatuses(List<Event> events, Action action) {
        return events.stream().filter(e -> e.action() == action).map(Event::status).toList();
    }
    
    static void listAll(Folder folder, String indent) throws IOException {
        List<Entry> entries = folder.list();
        for (Entry entry : entries) {
            System.out.print(indent + entry.getName());
            if (entry.isFile()) {
                System.out.println(" " + ((File) entry).getCreationTime() + ":" + ((File) entry).getLastModifiedTime()
                        + " " + ((File) entry).getSize() + "bytes");
            } else if (entry.isFolder()) {
                System.out.println(" (folder)");
                listAll((Folder) entry, indent + "  ");
            }
        }
    }
    
    private void checkSame(Folder expected, Folder folder) throws IOException {
        List<Entry> expectedEntries = expected.list();
        Map<String, Entry> actualEntries = folder.list().stream().collect(Collectors.toMap(Entry::getName, Function.identity()));
        assertEquals(expectedEntries.size(), actualEntries.size(), "Number of entries does not match: " + expectedEntries + " != " + actualEntries.values());
        for (int i = 0; i < expectedEntries.size(); i++) {
            Entry expectedEntry = expectedEntries.get(i);
            Entry actualEntry = actualEntries.get(expectedEntry.getName());
            assertEquals(expectedEntry.getName(), actualEntry.getName());
            if (expectedEntry.isFile()) {
                checkSame(expectedEntry.asFile(), actualEntry.asFile());
            } else if (expectedEntry.isFolder()) {
                assertTrue(actualEntry.isFolder());
                checkSame(expectedEntry.asFolder(), actualEntry.asFolder());
            }
        }
    }
    
    private void checkSame(File expected, Entry entry) throws IOException {
        assertTrue(entry.isFile());
        File file = (File) entry;
        assertEquals(expected.getSize(), file.getSize());
        assertEquals(expected.getCreationTime(), file.getCreationTime());
        assertEquals(expected.getLastModifiedTime(), file.getLastModifiedTime());
        try (InputStream expectedStream = expected.getInputStream(); InputStream actualStream = file.getInputStream()) {
            assertArrayEquals(expectedStream.readAllBytes(), actualStream.readAllBytes());
        }
    }

    static void execute(Synchronization synchronization) throws InterruptedException {
        synchronization.start();
        assertTrue(synchronization.waitFor(10, TimeUnit.SECONDS), "Synchronization did not finish within 10 seconds");
    }
}
