package com.fathzer.sync4j.sync;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.ExecutorService;

import static com.fathzer.sync4j.util.PrivateFields.getFieldValue;


import org.junit.jupiter.api.Test;

import com.fathzer.sync4j.sync.parameters.SyncParameters;

class ContextTest {

    @Test
    void test() {
        // When
        SyncParameters parameters = new SyncParameters();
        ExecutorService walkService;
        ExecutorService checkService;
        ExecutorService copyService;
        try (Context context = new Context(parameters)) {
            // Then - Use reflection to access private fields
            assertSame(parameters, context.params());
            assertEquals(new Statistics(), context.statistics());
            assertTrue(context.errors().isEmpty());
            assertFalse(context.isCancelled());
            assertEquals(0, context.taskCounter().getPendingTasks());

            walkService = getFieldValue(context, "walkService", ExecutorService.class);
            checkService = getFieldValue(context, "checkService", ExecutorService.class);
            copyService = getFieldValue(context, "copyService", ExecutorService.class);
            
            assertNotNull(walkService);
            assertNotNull(checkService);
            assertNotNull(copyService);
        }

        // Verify close shutdown the executor services
        assertTrue(walkService.isShutdown());
        assertTrue(checkService.isShutdown());
        assertTrue(copyService.isShutdown());


        // Verify that creating Context with null parameters throws NPE
        assertThrows(NullPointerException.class, () -> new Context(null));
    }

    @Test
    void testWithZeroParameters() {
        // When
        SyncParameters parameters = new SyncParameters();
        parameters.performance().maxComparisonThreads(0).maxCopyThreads(0);
        ExecutorService walkService;
        try (Context context = new Context(parameters)) {
            walkService = getFieldValue(context, "walkService", ExecutorService.class);
            assertNull(getFieldValue(context, "checkService", Object.class));
            assertNull(getFieldValue(context, "copyService", Object.class));
            assertNotNull(walkService);
        }

        // Verify close shutdown the walk service
        assertTrue(walkService.isShutdown());
    }

}
