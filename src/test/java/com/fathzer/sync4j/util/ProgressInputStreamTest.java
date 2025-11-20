package com.fathzer.sync4j.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongConsumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProgressInputStreamTest {
    private byte[] testData;
    private InputStream mockInputStream;
    
    @BeforeEach
    void setUp() {
        testData = "Hello, World! This is test data for ProgressInputStream.".getBytes();
        mockInputStream = new ByteArrayInputStream(testData);
    }
    
    @Test
    void testConstructorWithNullArguments() {
        assertThrows(NullPointerException.class, () -> new ProgressInputStream(null, bytes -> {}));

        InputStream delegate = new ByteArrayInputStream(new byte[0]);
        assertThrows(NullPointerException.class, () -> new ProgressInputStream(delegate, null));
    }
    
    @Test
    void testConstructorSendsInitialProgress() {
        AtomicLong progress = new AtomicLong(5);
        LongConsumer listener = progress::set;
        
        try {
            try (InputStream inputStream = new ByteArrayInputStream(new byte[0])) {
                try (ProgressInputStream progressStream = new ProgressInputStream(inputStream, listener)) {
                    // Do nothing, jsut test the constructor
                }
            }
        } catch (IOException e) {
            fail("Should not throw IOException: " + e.getMessage());
        }
        
        assertEquals(0, progress.get(), "Initial progress should be 0");
    }
    
    @Test
    void testSingleByteRead() throws IOException {
        List<Long> progressUpdates = new ArrayList<>();
        LongConsumer listener = progressUpdates::add;
        
        try (ProgressInputStream progressStream = new ProgressInputStream(mockInputStream, listener)) {
            int firstByte = progressStream.read();
            assertNotEquals(-1, firstByte, "Should read a valid byte");
            
            assertEquals(2, progressUpdates.size(), "Should have initial and first read progress updates");
            assertEquals(0, progressUpdates.get(0), "First update should be 0");
            assertEquals(1, progressUpdates.get(1), "Second update should be 1 after reading one byte");
        }
    }
    
    @Test
    void testMultipleByteRead() throws IOException {
        List<Long> progressUpdates = new ArrayList<>();
        LongConsumer listener = progressUpdates::add;
        
        try (ProgressInputStream progressStream = new ProgressInputStream(mockInputStream, listener)) {
            byte[] buffer = new byte[10];
            int bytesRead = progressStream.read(buffer);
            
            assertTrue(bytesRead > 0, "Should read some bytes");
            assertEquals(2, progressUpdates.size(), "Should have initial and read progress updates");
            assertEquals(0, progressUpdates.get(0), "First update should be 0");
            assertEquals(bytesRead, progressUpdates.get(1).longValue(), "Second update should match bytes read");
        }
    }
    
    @Test
    void testReadWithOffsetAndLength() throws IOException {
        List<Long> progressUpdates = new ArrayList<>();
        LongConsumer listener = progressUpdates::add;
        
        try (ProgressInputStream progressStream = new ProgressInputStream(mockInputStream, listener)) {
            byte[] buffer = new byte[20];
            int bytesRead = progressStream.read(buffer, 5, 10);
            
            assertTrue(bytesRead > 0, "Should read some bytes");
            assertEquals(2, progressUpdates.size(), "Should have initial and read progress updates");
            assertEquals(bytesRead, progressUpdates.get(1).longValue(), "Progress should match bytes read");
        }
    }
    
    @Test
    void testSkip() throws IOException {
        List<Long> progressUpdates = new ArrayList<>();
        LongConsumer listener = progressUpdates::add;
        
        try (ProgressInputStream progressStream = new ProgressInputStream(mockInputStream, listener)) {
            long bytesSkipped = progressStream.skip(5);
            
            assertTrue(bytesSkipped > 0, "Should skip some bytes");
            assertEquals(2, progressUpdates.size(), "Should have initial and skip progress updates");
            assertEquals(bytesSkipped, progressUpdates.get(1).longValue(), "Progress should match bytes skipped");
        }
    }
    
    @Test
    void testReadToEnd() throws IOException {
        List<Long> progressUpdates = new ArrayList<>();
        LongConsumer listener = progressUpdates::add;
        
        try (ProgressInputStream progressStream = new ProgressInputStream(mockInputStream, listener)) {
            byte[] buffer = new byte[1024];
            int totalBytesRead = 0;
            int bytesRead;
            
            while ((bytesRead = progressStream.read(buffer)) != -1) {
                totalBytesRead += bytesRead;
            }
            
            assertEquals(testData.length, totalBytesRead, "Should read all test data");
            assertTrue(progressUpdates.size() > 1, "Should have multiple progress updates");
            assertEquals(testData.length, progressUpdates.get(progressUpdates.size() - 1).longValue(), 
                "Final progress should match total bytes read");
        }
    }
    
    @Test
    void testAvailable() throws IOException {
        LongConsumer listener = bytes -> {};
        
        try (ProgressInputStream progressStream = new ProgressInputStream(mockInputStream, listener)) {
            int available = progressStream.available();
            assertEquals(testData.length, available, "Available bytes should match test data length");
        } catch (IOException e) {
            fail("Should not throw IOException: " + e.getMessage());
        }
    }
    
    @Test
    void testMarkSupported() {
        InputStream markSupportedStream = new ByteArrayInputStream(testData);
        LongConsumer listener = bytes -> {};
        
        try (ProgressInputStream progressStream = new ProgressInputStream(markSupportedStream, listener)) {
            assertTrue(progressStream.markSupported(), "Should support mark if delegate supports it");
        } catch (IOException e) {
            fail("Should not throw IOException: " + e.getMessage());
        }
    }
    
    @Test
    void testMarkAndReset() throws IOException {
        List<Long> progressUpdates = new ArrayList<>();
        LongConsumer listener = progressUpdates::add;
        
        try (ProgressInputStream progressStream = new ProgressInputStream(mockInputStream, listener)) {
            // Read some bytes
            byte[] buffer = new byte[5];
            int bytesRead = progressStream.read(buffer);
            
            // Mark position
            progressStream.mark(100);
            
            // Read more bytes
            int moreBytesRead = progressStream.read(buffer);
            
            // Reset to marked position
            progressStream.reset();
            
            // Read again from marked position
            int resetBytesRead = progressStream.read(buffer);
            
            assertTrue(bytesRead > 0, "Initial read should succeed");
            assertTrue(moreBytesRead > 0, "Second read should succeed");
            assertTrue(resetBytesRead > 0, "Read after reset should succeed");
        }
    }
    
    @Test
    void testClose() throws IOException {
        LongConsumer listener = bytes -> {};
        AtomicBoolean closed = new AtomicBoolean(false);
        InputStream closeableStream = new ByteArrayInputStream(testData) {
            @Override
            public void close() throws IOException {
                closed.set(true);
                super.close();
            }
        };
        assertFalse(closed.get(), "Underlying stream should not be closed yet");
        
        ProgressInputStream progressStream = new ProgressInputStream(closeableStream, listener);
        
        // Close the stream
        progressStream.close();
        
        assertTrue(closed.get(), "Underlying stream should be closed");
    }
    
    @Test
    void testProgressAccumulation() throws IOException {
        List<Long> progressUpdates = new ArrayList<>();
        LongConsumer listener = progressUpdates::add;
        
        try (ProgressInputStream progressStream = new ProgressInputStream(mockInputStream, listener)) {
            // Read in multiple chunks
            byte[] buffer = new byte[3];
            int totalBytesRead = 0;
            
            for (int i = 0; i < 5; i++) {
                int bytesRead = progressStream.read(buffer);
                if (bytesRead == -1) break;
                totalBytesRead += bytesRead;
                
                // Check that progress accumulates correctly
                long currentProgress = progressUpdates.get(progressUpdates.size() - 1);
                assertEquals(totalBytesRead, currentProgress, 
                    "Progress should accumulate correctly after chunk " + (i + 1));
            }
        }
    }
    
    @Test
    void testNoProgressUpdateOnEOF() throws IOException {
        List<Long> progressUpdates = new ArrayList<>();
        LongConsumer listener = progressUpdates::add;
        
        try (ProgressInputStream progressStream = new ProgressInputStream(mockInputStream, listener)) {
            // Read all data
            byte[] buffer = new byte[1024];
            while (progressStream.read(buffer) != -1) {
                // Continue reading until EOF
            }
            
            // Try to read again (should return -1)
            int eofRead = progressStream.read(buffer);
            assertEquals(-1, eofRead, "Should return -1 at EOF");
            
            // Progress should not be updated after EOF read
            long lastProgress = progressUpdates.get(progressUpdates.size() - 1);
            assertEquals(testData.length, lastProgress, "Progress should not change after EOF");
        } catch (IOException e) {
            fail("Should not throw IOException: " + e.getMessage());
        }
    }
}
