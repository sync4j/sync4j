package com.fathzer.sync4j.sync;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.eventually.EventuallyConsistentProvider;
import com.fathzer.sync4j.memory.MemoryFileProvider;
import com.fathzer.sync4j.memory.MemoryFolder;
import com.fathzer.sync4j.sync.Event.ListAction;
import com.fathzer.sync4j.sync.parameters.SyncParameters;

/** Tests the passes (high level retries) of a synchronization. */
class SynchronizationPassesTest {
    private MemoryFolder source;
    private MemoryFolder underlyingDestination;
    private List<Event> events;
    private SyncParameters parameters;

    @BeforeEach
    void setUp() throws IOException {
        source = (MemoryFolder) new MemoryFileProvider().get("").asFolder();
        underlyingDestination = (MemoryFolder) new MemoryFileProvider().get("").asFolder();
        MemoryFolder folder = source.mkdir("folder");
        folder.createFile("file.txt", "content".getBytes());
        source.createFile("root.txt", "root".getBytes());
        events = new CopyOnWriteArrayList<>();
        parameters = new SyncParameters();
        parameters.eventListener(events::add);
        // Continue after an error, a new pass is only possible if the synchronization is not cancelled
        parameters.errorManager((e, action) -> false);
        // No retry of operations, only passes
        parameters.retry().maxAttempts(1).initialDelay(Duration.ofMillis(1)).maxDelay(Duration.ofSeconds(1));
    }

    @AfterEach
    void tearDown() {
        source.getFileProvider().close();
        underlyingDestination.getFileProvider().close();
    }

    private long passCount() {
        return events.stream().filter(e -> e.status() == Event.Status.PLANNED && e.action() instanceof ListAction l && l.folder() == source).count();
    }

    private Synchronization run(Folder destination) throws Exception {
        Synchronization synchronization = new Synchronization(source, destination, parameters);
        synchronization.start();
        assertTrue(synchronization.waitFor(5, TimeUnit.SECONDS), "Synchronization did not finish");
        return synchronization;
    }

    private Set<String> names(Folder folder) throws IOException {
        return folder.list().stream().map(Entry::getName).collect(Collectors.toSet());
    }

    @Test
    void testNewPassSolvesPropagationDelay() throws Exception {
        // Writing in a folder that was just created fails during the first pass (strict mode) and the folder is visible during the second one
        parameters.retry().initialDelay(Duration.ofMillis(200));
        Folder destination = new EventuallyConsistentProvider(underlyingDestination.getFileProvider(), 100).get("").asFolder();
        try (Synchronization synchronization = run(destination)) {
            assertEquals(List.of(), synchronization.getErrors(), "Errors of previous passes should be forgotten");
            assertTrue(passCount() >= 2, "A new pass was expected");
            // The statistics do not count twice what failed in the first pass
            assertEquals(new Statistics.Counter(2, 2), synchronization.getStatistics().copiedFiles());
            assertEquals(new Statistics.Counter(11, 11), synchronization.getStatistics().copiedBytes());
            assertEquals(new Statistics.Counter(1, 1), synchronization.getStatistics().createdFolders());
        }
        assertEquals(Set.of("folder", "root.txt"), names(underlyingDestination));
        assertEquals(Set.of("file.txt"), names(underlyingDestination.getFileProvider().get("/folder").asFolder()));
    }

    @Test
    void testMaxPasses() throws Exception {
        // The destination never becomes consistent during the test
        parameters.retry().maxPasses(2);
        Folder destination = new EventuallyConsistentProvider(underlyingDestination.getFileProvider(), 60_000).get("").asFolder();
        try (Synchronization synchronization = run(destination)) {
            assertFalse(synchronization.getErrors().isEmpty());
            assertEquals(2, passCount());
        }
    }

    @Test
    void testNoNewPassWhenCancelledByErrorManager() throws Exception {
        parameters.errorManager((e, action) -> true);
        Folder destination = new EventuallyConsistentProvider(underlyingDestination.getFileProvider(), 60_000).get("").asFolder();
        try (Synchronization synchronization = run(destination)) {
            assertFalse(synchronization.getErrors().isEmpty());
            assertTrue(synchronization.isCancelled());
            assertEquals(1, passCount());
        }
    }

    @Test
    void testNoNewPassOnDefinitiveError() throws Exception {
        // A read-only destination can't be solved by a new pass
        underlyingDestination.getFileProvider().setReadOnly(true);
        try (Synchronization synchronization = run(underlyingDestination)) {
            assertFalse(synchronization.getErrors().isEmpty());
            assertEquals(1, passCount());
        }
    }

    @Test
    void testNoNewPassWithStronglyConsistentDestination() throws Exception {
        // FileNotFoundException & co are definitive errors on a strongly consistent provider, even if they could be a propagation delay on another one
        parameters.retry().maxPasses(3);
        try (Synchronization synchronization = run(underlyingDestination)) {
            assertEquals(List.of(), synchronization.getErrors());
            assertEquals(1, passCount());
        }
        assertEquals(Set.of("folder", "root.txt"), names(underlyingDestination));
    }

    @Test
    void testRateLimit() throws Exception {
        parameters.performance().maxOperationsPerSecond(40).maxWalkThreads(4).maxCopyThreads(2);
        final long start = System.nanoTime();
        final Statistics statistics;
        try (Synchronization synchronization = run(underlyingDestination)) {
            assertEquals(List.of(), synchronization.getErrors());
            statistics = synchronization.getStatistics();
        }
        final long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        final long operations = statistics.listedFolders().total().get() + statistics.copiedFiles().total().get()
                + statistics.createdFolders().total().get() + statistics.checkedFiles().total().get();
        assertTrue(operations >= 6, "Unexpected number of operations: " + operations);
        // 40 operations per second => 25ms between two operations, whatever the number of threads
        assertTrue(elapsedMs >= (operations - 1) * 25 * 9 / 10, "Synchronization of " + operations + " operations took only " + elapsedMs + "ms");
    }
}
