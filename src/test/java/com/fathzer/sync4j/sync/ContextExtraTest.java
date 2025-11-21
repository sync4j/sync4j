package com.fathzer.sync4j.sync;

import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

class ContextExtraTest {
    
    @Test
    void testDaemonThreadFactory() {
        // Test thread naming
        Context.DaemonThreadFactory factory = new Context.DaemonThreadFactory("test-prefix");
    
        // Create a few threads to test the counter
        Thread t1 = factory.newThread(() -> {});
        Thread t2 = factory.newThread(() -> {});
    
        // Verify thread properties
        assertTrue(t1.isDaemon(), "Thread should be a daemon thread");
        assertTrue(t1.getName().startsWith("test-prefix-"), "Thread name should start with prefix");
        assertNotEquals(t1.getName(), t2.getName(), "Thread names should be unique");
    
        // Verify the counter increments
        int t1Num = Integer.parseInt(t1.getName().substring("test-prefix-".length()));
        int t2Num = Integer.parseInt(t2.getName().substring("test-prefix-".length()));
        assertEquals(1, t2Num - t1Num, "Thread numbers should be sequential");

        // verify null argument throws NPE
        assertThrows(NullPointerException.class, () -> new Context.DaemonThreadFactory(null));
        assertThrows(NullPointerException.class, () -> factory.newThread(null));

    }
    
    @Test
    void testTaskCounterIncrementDecrement() {
        Context.TaskCounter counter = new Context.TaskCounter();
    
        // Initial state
        assertEquals(0, counter.getPendingTasks(), "Initial pending tasks should be 0");
    
        // Test increment
        counter.increment();
        assertEquals(1, counter.getPendingTasks(), "Should have 1 pending task after increment");
    
        // Test decrement
        counter.decrement();
        assertEquals(0, counter.getPendingTasks(), "Should have 0 pending tasks after decrement");
    }
    
    @Test
    void testTaskCounterMultipleIncrements() {
        Context.TaskCounter counter = new Context.TaskCounter();
    
        // Increment multiple times
        for (int i = 0; i < 5; i++) {
            counter.increment();
        }
        assertEquals(5, counter.getPendingTasks(), "Should have 5 pending tasks");
    
        // Decrement multiple times
        for (int i = 0; i < 3; i++) {
            counter.decrement();
        }
        assertEquals(2, counter.getPendingTasks(), "Should have 2 pending tasks after some decrements");
    }
    
    @Test
    void testTaskCounterAwait() throws InterruptedException {
        Context.TaskCounter counter = new Context.TaskCounter();
        final int TASK_COUNT = 3;
    
        // Set up a thread to wait for tasks to complete
        AtomicBoolean allTasksCompleted = new AtomicBoolean(false);
        CountDownLatch startLatch = new CountDownLatch(1);
    
        Thread waiter = new Thread(() -> {
            try {
                startLatch.await();
                counter.await();
                allTasksCompleted.set(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        waiter.start();
    
        // Add some tasks
        for (int i = 0; i < TASK_COUNT; i++) {
            counter.increment();
        }
    
        // Start the waiter and verify it's still waiting
        startLatch.countDown();
        await().atMost(Duration.ofMillis(500)).until(() -> waiter.getState() == Thread.State.WAITING);
        assertFalse(allTasksCompleted.get(), "Should not complete while tasks are pending");
    
        // Complete the tasks and verify the waiter is released
        for (int i = 0; i < TASK_COUNT; i++) {
            counter.decrement();
        }
    
        // Wait for the waiter to complete
        waiter.join(1000);
        assertTrue(allTasksCompleted.get(), "Should complete after all tasks are done");
        assertFalse(waiter.isAlive(), "Waiter thread should have completed");
    }
    
    @Test
    void testTaskCounterAwaitInterrupted() throws InterruptedException {
        Context.TaskCounter counter = new Context.TaskCounter();
        counter.increment(); // Make sure there's a pending task
    
        // Set up a thread that will be interrupted
        Thread waiter = new Thread(() -> {
            try {
                counter.await();
                fail("Should have been interrupted");
            } catch (InterruptedException e) {
                // Expected
                Thread.currentThread().interrupt(); // Restore the interrupt status
            }
        });
    
        waiter.start();
        await().atMost(Duration.ofMillis(500)).until(() -> waiter.getState() == Thread.State.WAITING);
    
        // Interrupt the waiter
        waiter.interrupt();
        waiter.join(1000);
    
        assertFalse(waiter.isAlive(), "Waiter thread should have completed");
        assertTrue(waiter.isInterrupted(), "Waiter thread should be interrupted");
    }
}
