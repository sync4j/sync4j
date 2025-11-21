package com.fathzer.sync4j.sync;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StatisticsTest {
    private Statistics stats;

    @BeforeEach
    void setUp() {
        stats = new Statistics();
    }

    @Test
    void testInitialState() {
        assertCounterInitialized(stats.preloadedFolders());
        assertCounterInitialized(stats.listedFolders());
        assertCounterInitialized(stats.checkedFiles());
        assertCounterInitialized(stats.deletedFiles());
        assertCounterInitialized(stats.copiedFiles());
        assertCounterInitialized(stats.copiedBytes());
        assertCounterInitialized(stats.deletedFolders());
        assertCounterInitialized(stats.createdFolders());
        
        assertEquals(0, stats.skippedFiles().get());
        assertEquals(0, stats.skippedFolders().get());
    }

    @Test
    void testCounterIncrement() {
        // Test incrementing counters
        stats.preloadedFolders().total().incrementAndGet();
        stats.listedFolders().done().incrementAndGet();
        stats.checkedFiles().total().addAndGet(5);
        stats.deletedFiles().done().incrementAndGet();
        stats.copiedFiles().total().incrementAndGet();
        stats.copiedBytes().done().addAndGet(1024);
        stats.deletedFolders().total().incrementAndGet();
        stats.createdFolders().done().incrementAndGet();
        stats.skippedFiles().incrementAndGet();
        stats.skippedFolders().addAndGet(2);

        // Verify the increments
        assertEquals(1, stats.preloadedFolders().total().get());
        assertEquals(1, stats.listedFolders().done().get());
        assertEquals(5, stats.checkedFiles().total().get());
        assertEquals(1, stats.deletedFiles().done().get());
        assertEquals(1, stats.copiedFiles().total().get());
        assertEquals(1024, stats.copiedBytes().done().get());
        assertEquals(1, stats.deletedFolders().total().get());
        assertEquals(1, stats.createdFolders().done().get());
        assertEquals(1, stats.skippedFiles().get());
        assertEquals(2, stats.skippedFolders().get());
    }

    @Test
    void testCounterRecord() {
        // Test the Counter record functionality
        Statistics.Counter counter = new Statistics.Counter();
        assertEquals(0, counter.total().get());
        assertEquals(0, counter.done().get());

        counter.total().incrementAndGet();
        counter.done().incrementAndGet();
        
        assertEquals(1, counter.total().get());
        assertEquals(1, counter.done().get());

        // Test the constructor with parameters
        AtomicLong total = new AtomicLong(10);
        AtomicLong done = new AtomicLong(5);
        counter = new Statistics.Counter(total, done);
        
        assertEquals(10, counter.total().get());
        assertEquals(5, counter.done().get());
    }

    @Test
    void testToString() {
        // Modify some values
        stats.copiedFiles().total().set(5);
        stats.copiedFiles().done().set(3);
        stats.skippedFiles().set(2);

        String str = stats.toString();
        assertTrue(str.contains("copiedFiles=Counter[total=5, done=3]"));
        assertTrue(str.contains("skippedFiles=2"));
    }

    private void assertCounterInitialized(Statistics.Counter counter) {
        assertNotNull(counter);
        assertNotNull(counter.total());
        assertNotNull(counter.done());
        assertEquals(0, counter.total().get());
        assertEquals(0, counter.done().get());
    }
}
