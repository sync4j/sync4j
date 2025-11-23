package com.fathzer.sync4j.sync;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.fathzer.sync4j.util.PrivateFields.getFieldValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.sync.Event.Action;
import com.fathzer.sync4j.sync.parameters.SyncParameters;

class WalkTaskTest {
    @FunctionalInterface
    static interface IOFunction<V> {
        V apply(Task<V, ?> task) throws IOException;
    }
    
    private Context context;
    private Folder sourceFolder;
    private Folder destinationFolder;
    private List<Entry> sourceList;
    private List<Entry> destinationList;
    private SyncParameters parameters;

    private IOFunction<?> syncFunction;
    private BiConsumer<Throwable, Action> errorFunction;
    
    @BeforeEach
    void setUp() {
        parameters = new SyncParameters();
        context = new Context(parameters) {
            @SuppressWarnings("unchecked")
            @Override
            <V> V executeSync(Task<V, ?> task) throws IOException {
                return ((IOFunction<V>)syncFunction).apply(task);
            }

            @Override
            void processError(Throwable e, Action action) {
                errorFunction.accept(e, action);
            }
        };

        // Create mocks
        sourceFolder = mock(Folder.class);
        destinationFolder = mock(Folder.class);
        
        // Initialize empty lists
        sourceList = new ArrayList<>();
        destinationList = new ArrayList<>();
    }
    
    @Test
    void testConstructor() {
        // When
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
    void testList() {
        WalkTask walkTask = new WalkTask(context, sourceFolder, destinationFolder, destinationList);
        // When
        syncFunction = t -> sourceList;
        List<Entry> result = walkTask.list(sourceFolder);
        
        // Then
        assertEquals(sourceList, result);

        // When
        IOException error = new IOException();
        syncFunction = t -> { throw error; };
        errorFunction = (e, action) -> {
            assertSame(error, e);
            assertSame(Event.ListAction.class, action.getClass());
        };
        result = walkTask.list(sourceFolder);
        
        // Then
        assertNull(result);
    }
}
