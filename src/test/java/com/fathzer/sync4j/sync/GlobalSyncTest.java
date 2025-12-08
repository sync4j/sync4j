package com.fathzer.sync4j.sync;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
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
import com.fathzer.sync4j.sync.Context.TaskCounter;
import com.fathzer.sync4j.sync.Event.Action;
import com.fathzer.sync4j.sync.Event.Status;
import com.fathzer.sync4j.sync.parameters.SyncParameters;
import com.fathzer.sync4j.util.PrivateFields;

class GlobalSyncTest {
    private MemoryFolder source;
    private MemoryFolder destination;
    private EventList events;
    private SyncParameters parameters;

    private static class EventList extends LinkedList<Event> {
        private static final long serialVersionUID = 1L;
        
		private AtomicBoolean closed = new AtomicBoolean();
        private Map<Action, Status> actions = new ConcurrentHashMap<>();
        private List<Throwable> errors = new LinkedList<>();
        
        @Override
        public boolean add(Event e) {
            if (closed.get()) {
                errors.add(new IllegalStateException("List is closed"));
            }
            Event.Status previousStatus = actions.get(e.action());
            if (previousStatus != e.status().previous()) {
                errors.add(new IllegalArgumentException("Expected previous status " + e.status().previous() + " but was " + previousStatus));
            } else {
                actions.put(e.action(), e.status());
            }
            if (e.status() == Event.Status.STARTED) {
                delay(50);
            }
            boolean added;
            synchronized (this) {
                added = super.add(e);
            }
            return added;
        }

        public void close() {
            closed.set(true);
        }

        /** Intentional delay to simulate realistic task execution timing.
         * This helps expose potential synchronization issues that might be hidden
         * when mocked tasks execute too quickly.
         */
        @SuppressWarnings("java:S2925") // Intentional sleep for realistic test timing
        private void delay(long ms) {
            try {
                TimeUnit.MILLISECONDS.sleep(ms);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public void clear() {
            super.clear();
            actions.clear();
            errors.clear();
            closed.set(false);
        }

        public void checkErrors() {
            errors.forEach(Throwable::printStackTrace);
            if (!errors.isEmpty()) {
                fail("Errors occurred in events management");
            }
        }

        public void checkSuccessfull(boolean allSuccessfull) {
            if (allSuccessfull) {
                assertFalse(stream().anyMatch(this::isFailed), "No failed event expected but found " + stream().filter(this::isFailed).toList());
            } else {
                assertTrue(stream().anyMatch(this::isFailed), "One failed event expected but was " + this);
            }
            checkErrors();
        }
        
        private boolean isFailed(Event event) {
            return event.status() == Event.Status.FAILED;
        }
    }

    @SuppressWarnings("resource")
    @BeforeEach
    void setUp() throws IOException {
        source = (MemoryFolder) new MemoryFileProvider().get("").asFolder();
        destination = (MemoryFolder) new MemoryFileProvider().get("").asFolder();
        events = new EventList();
        parameters = new SyncParameters();
        parameters.eventListener(events::add);
        parameters.errorManager((e, action) -> true);

        populate(source);
    }


    @AfterEach
    void tearDown() {
        source.getFileProvider().close();
        destination.getFileProvider().close();
    }
    
    private void populate(MemoryFolder target) throws IOException {
        MemoryFolder folder = target.mkdir("folder");
        MemoryFolder subSubFolder = folder.mkdir("sub-sub-folder");
        folder.mkdir("another-sub-folder");
        target.createFile("same.txt", "same".getBytes());
        target.createFile("missing.txt", "missing".getBytes());
        target.createFile("notSameContent.txt", "notSameContentSource".getBytes());
        subSubFolder.createFile("sub-file.txt", "sub-file".getBytes());
    }

    /** This method is used to spy on the task counter. It is not currently used in the tests but can be useful to debug when the test fails.
     * @param synchronization 
     */
    @SuppressWarnings("unused")
    private void setTaskCounterSpy(Synchronization synchronization) {
        BiConsumer<TaskCounter, Boolean> spy = (tc, increment) -> {
            System.out.println("Task counter " + (increment ? "incremented" : "decremented") + " to " + tc.getPendingTasks());
        };
        PrivateFields.getFieldValue(synchronization, "context", Context.class).taskCounter().setEventConsumer(spy);
    }

    @Test
    void testNonExistingDestination() throws Exception {
        // Sync to non existing destination => Should fail
        Folder nonExisting = destination.mkdir("nonExisting");
        nonExisting.delete();
        try (Synchronization synchronization = new Synchronization(source, nonExisting, parameters)) {
            execute(synchronization);
            assertEquals(1, synchronization.getErrors().size(), "One error expected on non existing destination but was " + synchronization.getErrors());
            events.checkSuccessfull(false);
        }
    }
    
    @Test
    void testReadOnlyDestination() throws Exception {
        // Sync to read only existing destination => Should fail
        destination.getFileProvider().setReadOnly(true);
        try (Synchronization synchronization = new Synchronization(source, destination, parameters)) {
            execute(synchronization);
            assertFalse(synchronization.getErrors().isEmpty(), "At least one error expected on read only existing destination but the was none");
            events.checkSuccessfull(false);
            assertTrue(destination.list().isEmpty(), "Destination should remain empty but was " + destination.list());
        }
    }
    
    @Test
    void testReadOnlySource() throws Exception {
        // Sync readonly source to existing destination => Should succeed
        source.getFileProvider().setReadOnly(true);
        try (Synchronization synchronization = new Synchronization(source, destination, parameters)) {
            execute(synchronization);
            checkNoErrors(synchronization);
            events.checkSuccessfull(true);
            assertEquals(new Statistics.Counter(4, 4), synchronization.getStatistics().copiedFiles());
            assertEquals(new Statistics.Counter(39, 39), synchronization.getStatistics().copiedBytes());
            assertEquals(new Statistics.Counter(3, 3), synchronization.getStatistics().createdFolders());
            assertEquals(new Statistics.Counter(0, 0), synchronization.getStatistics().deletedFolders());
            assertEquals(new Statistics.Counter(0, 0), synchronization.getStatistics().deletedFiles());
        }
        // Check all actions where planned, executed and are successfull
        List<Action> actions = events.stream().map(Event::action).toList();
        actions.forEach(a -> assertEquals(List.of(Event.Status.PLANNED, Event.Status.STARTED, Event.Status.COMPLETED),
                getStatuses(events, a), "Action " + a + " is not successful"));
        checkSame(source, destination);
    }
    
    @Test
    void testDryRun() throws Exception {
        // Dry run should succeed and changes nothing
        parameters.dryRun(true);
        destination.mkdir("dest-folder");
        destination.createFile("dest-file", "new".getBytes());

        try (Synchronization synchronization = new Synchronization(source, destination, parameters)) {
            execute(synchronization);
            checkNoErrors(synchronization);
            events.checkSuccessfull(true);
            assertEquals(new Statistics.Counter(4, 0), synchronization.getStatistics().copiedFiles(), "Copied files");
            assertEquals(new Statistics.Counter(39, 0), synchronization.getStatistics().copiedBytes(), "Copied bytes");
            assertEquals(new Statistics.Counter(3, 0), synchronization.getStatistics().createdFolders(), "Created folders");
            assertEquals(new Statistics.Counter(1, 0), synchronization.getStatistics().deletedFolders(), "Deleted folders");
            assertEquals(new Statistics.Counter(1, 0), synchronization.getStatistics().deletedFiles(), "Deleted files");
        }
        assertEquals(Set.of("dest-folder", "dest-file"), destination.list().stream().map(Entry::getName).collect(Collectors.toSet()), "Destination should remain unchanged but was " + destination.list());
    }
    
    @Test
    void test() throws Exception {
        // Perform some changes in source
        source.mkdir("not in destination folder");
        MemoryFile sameFile = (MemoryFile) source.getFileProvider().get("/same.txt").asFile();
        source.createFile("new.txt", "new".getBytes());
        source.getFileProvider().get("/missing.txt").delete();
        // Perform some changes in destination
        destination.createFile("notSameContent.txt", "xxx".getBytes());
        destination.mkdir("new.txt");
        MemoryFile notSameTime = (MemoryFile) destination.copy("same.txt", sameFile, null);
        notSameTime.setLastModifiedTime(sameFile.getLastModifiedTime() + 1000);
        destination.mkdir("not in source folder");
        MemoryFolder destFolder = destination.mkdir("folder");
        destFolder.createFile("sub-sub-folder", "should be replaced by a file".getBytes());

        try (Synchronization synchronization = new Synchronization(source, destination, parameters)) {
            execute(synchronization);
            checkNoErrors(synchronization);
            events.checkSuccessfull(true);
            assertEquals(new Statistics.Counter(4, 4), synchronization.getStatistics().copiedFiles(), "Copied files");
            assertEquals(new Statistics.Counter(35, 35), synchronization.getStatistics().copiedBytes(), "Copied bytes");
            assertEquals(new Statistics.Counter(3, 3), synchronization.getStatistics().createdFolders(), "Created folders");
            assertEquals(new Statistics.Counter(2, 2), synchronization.getStatistics().deletedFolders(), "Deleted folders");
            assertEquals(new Statistics.Counter(1, 1), synchronization.getStatistics().deletedFiles(), "Deleted files");
        }
        checkSame(sameFile, sameFile);
    }
    
    private List<Event.Status> getStatuses(List<Event> events, Action action) {
        return events.stream().filter(e -> e.action() == action).map(Event::status).toList();
    }
    
    static void list(Folder folder, String indent) throws IOException {
        List<Entry> entries = folder.list();
        for (Entry entry : entries) {
            System.out.print(indent + entry.getName());
            if (entry.isFile()) {
                System.out.println(" " + ((File) entry).getCreationTime() + ":" + ((File) entry).getLastModifiedTime()
                        + " " + ((File) entry).getSize() + "bytes");
            } else if (entry.isFolder()) {
                System.out.println(" (folder)");
                list((Folder) entry, indent + "  ");
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

    private void checkNoErrors(Synchronization synchronization) {
        if (!synchronization.getErrors().isEmpty()) {
            synchronization.getErrors().forEach(Throwable::printStackTrace);
            fail("No error expected on existing destination but was " + synchronization.getErrors());
        }
        events.checkSuccessfull(true);
    }

    void execute(Synchronization synchronization) throws InterruptedException {
        synchronization.start();
        assertTrue(synchronization.waitFor(1, TimeUnit.SECONDS), "Synchronization did not finish within 10 seconds");
        events.close();
    }
}
