package com.fathzer.sync4j.sync.parameters;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fathzer.sync4j.File;
import com.fathzer.sync4j.FileProvider;
import com.fathzer.sync4j.HashAlgorithm;

@ExtendWith(MockitoExtension.class)
class FileComparatorTest {
    @Mock
    private File file1;
    
    @Mock
    private File file2;
    
    @Mock
    private FileProvider provider1;
    
    @Mock
    private FileProvider provider2;

    @Test
    void testSizeComparator() throws Exception {
        // Given
        when(file1.getSize()).thenReturn(1000L);
        when(file2.getSize()).thenReturn(1000L);
        
        // When/Then
        assertTrue(FileComparator.SIZE.areSame(file1, file2), "Files with same size should be equal");
        
        when(file2.getSize()).thenReturn(2000L);
        assertFalse(FileComparator.SIZE.areSame(file1, file2), "Files with different sizes should not be equal");
    }
    
    @Test
    void testModDateComparator() throws Exception {
        // Given
        when(file1.getLastModified()).thenReturn(1000L);
        when(file2.getLastModified()).thenReturn(1000L);
        
        // When/Then
        assertTrue(FileComparator.MOD_DATE.areSame(file1, file2), "Files with same modification date should be equal");
        
        when(file2.getLastModified()).thenReturn(2000L);
        assertFalse(FileComparator.MOD_DATE.areSame(file1, file2), "Files with different modification dates should not be equal");
    }
    
    @Test
    void testHashComparator() throws Exception {
        // Given
        when(file1.getHash(any(HashAlgorithm.class))).thenReturn("hash1");
        when(file2.getHash(any(HashAlgorithm.class))).thenReturn("hash1");
        
        // When
        FileComparator comparator = FileComparator.hash(mock(HashAlgorithm.class));
        
        // Then
        assertTrue(comparator.areSame(file1, file2), "Files with same hash should be equal");
        
        when(file2.getHash(any(HashAlgorithm.class))).thenReturn("hash2");
        assertFalse(comparator.areSame(file1, file2), "Files with different hashes should not be equal");
    }
    
    @Test
    void testBestHashAlgorithm() {
        // Given
        when(provider1.getSupportedHash()).thenReturn(List.of(HashAlgorithm.MD5, HashAlgorithm.SHA1, HashAlgorithm.SHA256));
        when(provider2.getSupportedHash()).thenReturn(List.of(HashAlgorithm.SHA1, HashAlgorithm.SHA256));
        
        // When/Then
        assertEquals(HashAlgorithm.SHA1, FileComparator.hash(provider1, provider2), "Should return the first common hash algorithm");
        
        when(provider1.getSupportedHash()).thenReturn(List.of(HashAlgorithm.MD5));
        when(provider2.getSupportedHash()).thenReturn(List.of(HashAlgorithm.SHA256));
        assertNull(FileComparator.hash(provider1, provider2), "Should return null when no common hash algorithm");
    }
    
    @Test
    void testCombinedComparator() throws Exception {
        // Given
        FileComparator sizeComparator = (f1, f2) -> f1.getSize() == f2.getSize();
        FileComparator modDateComparator = (f1, f2) -> f1.getLastModified() == f2.getLastModified();
        
        // When
        FileComparator combined = FileComparator.of(Arrays.asList(sizeComparator, modDateComparator));
        
        // Then
        when(file1.getSize()).thenReturn(1000L);
        when(file2.getSize()).thenReturn(1000L);
        when(file1.getLastModified()).thenReturn(2000L);
        when(file2.getLastModified()).thenReturn(2000L);
        
        assertTrue(combined.areSame(file1, file2), "Should be equal when all comparators return true");
        
        when(file2.getLastModified()).thenReturn(3000L);
        assertFalse(combined.areSame(file1, file2), "Should not be equal when any comparator returns false");
    }
}
