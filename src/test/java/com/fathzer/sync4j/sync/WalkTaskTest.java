package com.fathzer.sync4j.sync;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static com.fathzer.sync4j.util.PrivateFields.getFieldValue;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.sync.Context.TaskCounter;
import com.fathzer.sync4j.sync.parameters.SyncParameters;

@ExtendWith(MockitoExtension.class)
class WalkTaskTest {
    @FunctionalInterface
    static interface IOFunction<V> {
        V apply(Task<V, ?> task) throws IOException;
    }

    @Mock
    private Context context;
    @Mock
    private Folder sourceFolder;
    @Mock
    private Folder destinationFolder;

    @BeforeEach
    void setUp() {
        TaskCounter taskCounter = new TaskCounter();
        when(context.taskCounter()).thenReturn(taskCounter);
    }

    @Test
    void testConstructor() {
        // When
        List<Entry> destinationList = List.of();
        WalkTask task = new WalkTask(context, sourceFolder, destinationFolder, destinationList);

        // Then - Use reflection to access private fields
        assertSame(context, getFieldValue(task, "context", Context.class));
        assertSame(sourceFolder, getFieldValue(task, "sourceFolder", Folder.class));
        assertSame(destinationFolder, getFieldValue(task, "destinationFolder", Folder.class));
        assertSame(destinationList, getFieldValue(task, "destinationList", List.class));

        // Test null checks
        assertThrows(NullPointerException.class, () -> new WalkTask(null, sourceFolder, destinationFolder, destinationList));
        assertThrows(NullPointerException.class, () -> new WalkTask(context, null, destinationFolder, destinationList));
        assertThrows(NullPointerException.class, () -> new WalkTask(context, sourceFolder, null, destinationList));
        // destinationList can be null
        new WalkTask(context, sourceFolder, destinationFolder, null);
    }

    @Test
    void testList() throws IOException {
        SyncParameters parameters = new SyncParameters();
        when(context.params()).thenReturn(parameters);
        Statistics statistics = new Statistics();
        when(context.statistics()).thenReturn(statistics);

        // When
        List<Entry> sourceList = List.of();
        WalkTask walkTask = new WalkTask(context, sourceFolder, destinationFolder, null);
        when(context.executeSync(any(ListTask.class))).thenReturn(sourceList);
        List<Entry> result = walkTask.list(sourceFolder);

        // Then
        assertEquals(sourceList, result);

        // When
        IOException error = new IOException();
        when(context.executeSync(any(ListTask.class))).thenAnswer(invocation -> {
            throw error;
        });
        result = walkTask.list(sourceFolder);

        // Then
        assertNull(result);
        verify(context).processError(same(error), any(Event.ListAction.class));
    }
}
