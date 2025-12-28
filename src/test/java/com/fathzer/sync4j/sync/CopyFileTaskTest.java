package com.fathzer.sync4j.sync;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.LongConsumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fathzer.sync4j.File;
import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.sync.parameters.SyncParameters;

class CopyFileTaskTest {
    
    private Context context;
    private Event.CopyFileAction action;
    private Statistics statistics;
    private File sourceFile;
    private Folder destinationFolder;
    private File copiedFile;
    
    @BeforeEach
    void setUp() throws IOException {
        // Create mocks
        context = mock(Context.class);
        statistics = new Statistics();
        sourceFile = mock(File.class);
        destinationFolder = mock(Folder.class);
        copiedFile = mock(File.class);
        
        // Setup default behavior
        when(context.statistics()).thenReturn(statistics);
        SyncParameters parameters = new SyncParameters();
        when(context.params()).thenReturn(parameters);
        
        // Setup source file behavior
        when(sourceFile.getName()).thenReturn("test.txt");
        when(sourceFile.getSize()).thenReturn(1000L);
        
        // Setup action
        action = new Event.CopyFileAction(sourceFile, destinationFolder);
    }

    private File fakeCopy(File file, LongConsumer consumer) throws IOException {
        long size = file.getSize();
        consumer.accept(size/2);
        consumer.accept(size);
        when(copiedFile.getSize()).thenReturn(size);
        return copiedFile;
    }
    
    @Test
    void testConstructor() throws IOException {
        // When
        CopyFileTask task = new CopyFileTask(context, action);
        
        // Then
        assertSame(context, task.context());
        assertSame(action, task.action());
        
        // Verify statistics were updated
        assertEquals(1, statistics.copiedFiles().total().get());
        assertEquals(1000L, statistics.copiedBytes().total().get());

        // Test null context
        assertThrows(NullPointerException.class, () -> new CopyFileTask(null, action));
        
        // Test null action
        assertThrows(NullPointerException.class, () -> new CopyFileTask(context, null));
    }

    @Test
    void testExecuteWithIOException() throws IOException {
        // Given
        IOException expectedException = new IOException("Copy failed");
        when(destinationFolder.copy(any(), any(), any())).thenThrow(expectedException);
        CopyFileTask task = new CopyFileTask(context, action);
        
        // When/Then
        IOException thrown = assertThrows(IOException.class, task::execute);
        assertSame(expectedException, thrown);
    }
    
    @Test
    void testExecute() throws IOException {
        // Given
        when (destinationFolder.copy(anyString(), any(File.class), any(LongConsumer.class))).thenAnswer(invocation -> {
            return fakeCopy(invocation.getArgument(1), invocation.getArgument(2));
        });
        CopyFileTask task = new CopyFileTask(context, action);

        // When
        Void result = task.execute();
        
        // Then
        assertNull(result);
        assertEquals(sourceFile.getSize(), statistics.copiedBytes().done().get());        
    }
    
    @Test
    void testProgress() throws Exception {
        // Given
        when (destinationFolder.copy(anyString(), any(File.class), any(LongConsumer.class))).thenAnswer(invocation -> {
            return fakeCopy(invocation.getArgument(1), invocation.getArgument(2));
        });
        CopyFileTask task = new CopyFileTask(context, action);

        List<Long> progress = new LinkedList<>();
        action.setProgressListener(progress::add);
        
        // When
        Void result = task.execute();
        
        // Then
        assertNull(result);
        assertEquals(sourceFile.getSize(), statistics.copiedBytes().done().get());        
        assertEquals(2, progress.size());
        assertEquals(sourceFile.getSize()/2, progress.get(0));
        assertEquals(sourceFile.getSize(), progress.get(1));
    }
}
