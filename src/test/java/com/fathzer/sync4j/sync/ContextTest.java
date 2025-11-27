package com.fathzer.sync4j.sync;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.fathzer.sync4j.util.PrivateFields.getFieldValue;

import org.junit.jupiter.api.Test;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.sync.Context.Result;
import com.fathzer.sync4j.sync.Event.Action;
import com.fathzer.sync4j.sync.Statistics.Counter;
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
    void testWithZeroThreadsParameters() {
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

    @Test
    void testSkip() {
        // When
        SyncParameters parameters = new SyncParameters();
        try (Context context = new Context(parameters)) {
            // check skip increments skippedFiles and skippedFolders
            Entry entry = mock(Entry.class);
            when(entry.isFolder()).thenReturn(true);
            when(entry.isFile()).thenReturn(false);
            context.skip(entry);
            assertEquals(0, context.statistics().skippedFiles().get());
            assertEquals(1, context.statistics().skippedFolders().get());

            when(entry.isFolder()).thenReturn(false);
            when(entry.isFile()).thenReturn(true);
            context.skip(entry);
            assertEquals(1, context.statistics().skippedFiles().get());
            assertEquals(1, context.statistics().skippedFolders().get());
        }
    }

    @Test
    void testTryExecute() {
        // When
        SyncParameters parameters = new SyncParameters();
        AtomicReference<Throwable> error = new AtomicReference<>();
        AtomicReference<Action> action = new AtomicReference<>();
        try (Context context = new Context(parameters) {
            @Override
            void processError(Throwable e, Action a) {
                error.set(e);
                action.set(a);
            }
        }) {
            // check tryExecute calls the callable
            String expectedMessage = "test";
            Result<String> result = context.tryExecute(() -> expectedMessage, null);
            assertEquals(expectedMessage, result.value());
            assertFalse(result.failed());
            assertNull(error.get());
            assertNull(action.get());

            // check tryExecute increments errors on error
            IOException expectedException = new IOException("test");
            Action expectedAction = new Action(){};
            Callable<String> callable = () -> { throw expectedException; };
            result = context.tryExecute(callable, () -> expectedAction);
            assertTrue(result.failed());
            assertEquals(expectedException, error.get());
            assertEquals(expectedAction, action.get());
        }
    }

    @Test
    void testProcessError() {
        // When
        SyncParameters parameters = new SyncParameters();
        try (Context context = new Context(parameters)) {
            // check processError calls the error manager
            IOException expectedException = new IOException("test");
            Action expectedAction = new Action(){};
            AtomicBoolean called = new AtomicBoolean(false);
            parameters.errorManager((ex, a) -> {
                assertEquals(expectedException, ex);
                assertEquals(expectedAction, a);
                called.set(true);
                return false;
            });
            context.processError(expectedException, expectedAction);
            assertTrue(called.get());
            assertFalse(context.isCancelled());
            assertEquals(List.of(expectedException), context.errors());

            // check processError cancels the context if the error manager returns true
            // And put exceptions encapsulated in CompletionException in errors.
            called.set(false);
            parameters.errorManager((ex, a) -> {
                assertEquals(expectedException, ex);
                assertEquals(expectedAction, a);
                called.set(true);
                return true;
            });
            context.processError(new CompletionException(expectedException), expectedAction);
            assertTrue(called.get());
            assertTrue(context.isCancelled());
            assertEquals(List.of(expectedException, expectedException), context.errors());
        }
    }

    @Test
    void testExecuteSync() throws IOException {
        // When
        SyncParameters parameters = new SyncParameters();
        Action action = new Action(){};
        List<Event.Status> statuses = new LinkedList<>();
        parameters.eventListener(event -> {
            assertEquals(action, event.getAction());
            statuses.add(event.getStatus());
        });
        Counter counter = new Counter();
        try (Context context = new Context(parameters)) {
            Task<String, ? extends Action> task = new Task<>(context, action, counter) {
                @Override
                public String execute() throws IOException {
                    return "test";
                }
            };
            String result = context.executeSync(task);
            assertEquals("test", result);
            assertEquals(statuses.size(), 3);
            assertEquals(List.of(Event.Status.PLANNED, Event.Status.STARTED, Event.Status.COMPLETED), statuses);
        }
    }
}
