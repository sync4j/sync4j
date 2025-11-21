package com.fathzer.sync4j.file;

import static org.junit.jupiter.api.Assertions.*;
import static com.fathzer.sync4j.file.LocalProvider.INSTANCE;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mockStatic;


import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.File;
import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.HashAlgorithm;

@ExtendWith(MockitoExtension.class)
class LocalFileTest {
    @TempDir
    private static Path tempDir;

    private static Entry folder;
    private static Entry file;
    private static Entry nonExisting;
    
    @Mock
    private HashAlgorithm mockHashAlgorithm;

    @BeforeAll
    static void setup() throws IOException {
        String path = tempDir.toString();
        folder = INSTANCE.get(Files.createDirectory(tempDir.resolve("folder")).toString());
        file = INSTANCE.get(Files.createFile(tempDir.resolve("file.txt")).toString());
        nonExisting = INSTANCE.get(path + "/nonExisting");
    }

    @Test
    void testHashList() {
        assertEquals(Arrays.asList(HashAlgorithm.values()), INSTANCE.getSupportedHash());
    }

    @Test
    void testGetParent() throws IOException {
        assertEquals(countParent(tempDir.resolve("file.txt")), countParent(file));

        // Test no exception is thrown when parent is a regular file
        assertDoesNotThrow (nonExisting::getParent);
    }

    @Test
    void testGetParentPath() throws IOException {
        String expected = tempDir.resolve("file.txt").toAbsolutePath().getParent().toString();
        assertEquals(expected, file.getParentPath());

        Entry root = getRoot();
        assertEquals(null, root.getParentPath());
    }

    @Test
    void testGetProvider() throws IOException {
        Entry root = getRoot();
        assertSame(INSTANCE, root.getFileProvider());
        assertSame(INSTANCE, folder.getFileProvider());
        assertSame(INSTANCE, file.getFileProvider());
        assertSame(INSTANCE, nonExisting.getFileProvider());
    }

    @Test
    void testIsFileAndSimilar() throws IOException {
        Entry root = getRoot();
        assertTrue(root.exists());
        assertTrue(root.isFolder());
        assertFalse(root.isFile());
        assertSame(root, root.asFolder());

        assertTrue(file.exists());
        assertTrue(file.isFile());
        assertFalse(file.isFolder());
        assertSame(file, file.asFile());
        assertThrows(IllegalStateException.class, () -> file.asFolder());

        assertTrue(folder.exists());
        assertTrue(folder.isFolder());
        assertFalse(folder.isFile());
        assertSame(folder, folder.asFolder());
        assertThrows(IllegalStateException.class, () -> folder.asFile());

        assertFalse(nonExisting.exists());
        assertFalse(nonExisting.isFolder());
        assertFalse(nonExisting.isFile());
        assertThrows(IllegalStateException.class, () -> nonExisting.asFolder());
        assertThrows(IllegalStateException.class, () -> nonExisting.asFile());
    }

    private int countParent(Entry entry) throws IOException {
        int count = 0;
        while (entry.getParent()!=null) {
            entry = entry.getParent();
            count++;
        }
        return count;
    }

    private int countParent(Path path) {
        int count = 0;
        path = path.toAbsolutePath();
        while (path.getParent() != null) {
            path = path.getParent();
            count++;
        }
        return count;
    }

    @Test
    void testGetName() throws IOException {
        // Check that getName() does not throw exception when called on root file
        Entry root = getRoot();
        assertEquals("", root.getName());
    }
    
    @Test
    void testGetHashCallsComputeHash() throws IOException {
        // Given
        File localFile = (File) file;
        String expectedHash = "test-hash";
        when(mockHashAlgorithm.computeHash(any(Path.class))).thenReturn(expectedHash);
        
        // When
        String actualHash = localFile.getHash(mockHashAlgorithm);
        
        // Then
        assertEquals(expectedHash, actualHash);
        verify(mockHashAlgorithm).computeHash(any(Path.class));
    }
    
    @Test
    void testFilesBasedCalls() throws IOException {
        // Given
        File localFile = file.asFile();
        
        try (var mockedFiles = mockStatic(Files.class);
             var inputStream = new ByteArrayInputStream("test content".getBytes())) {
            
            // Setup mock returns
            long expectedSize = 12345L;
            long expectedLastModified = System.currentTimeMillis();
            long expectedCreationTime = expectedLastModified - 10000; // 10 seconds before last modified
            
            mockedFiles.when(() -> Files.size(any(Path.class))).thenReturn(expectedSize);
            mockedFiles.when(() -> Files.getLastModifiedTime(any(Path.class))).thenReturn(FileTime.fromMillis(expectedLastModified));
            mockedFiles.when(() -> Files.getAttribute(any(Path.class), eq("creationTime"))).thenReturn(FileTime.fromMillis(expectedCreationTime));
            mockedFiles.when(() -> Files.newInputStream(any(Path.class))).thenReturn(inputStream);
            
            // Test getSize()
            long actualSize = localFile.getSize();
            assertEquals(expectedSize, actualSize);
            mockedFiles.verify(() -> Files.size(any(Path.class)));
            
            // Test getLastModified()
            long actualLastModified = localFile.getLastModified();
            assertEquals(expectedLastModified, actualLastModified);
            mockedFiles.verify(() -> Files.getLastModifiedTime(any(Path.class)));
            
            // Test getCreationTime()
            long actualCreationTime = localFile.getCreationTime();
            assertEquals(expectedCreationTime, actualCreationTime);
            mockedFiles.verify(() -> Files.getAttribute(any(Path.class), eq("creationTime")));
            
            // Test getInputStream()
            try (var actualStream = localFile.getInputStream()) {
                assertNotNull(actualStream);
                assertEquals(inputStream, actualStream);
            }
            mockedFiles.verify(() -> Files.newInputStream(any(Path.class)));
        }
    }

    private Entry getRoot() throws IOException {
        final Path path = Paths.get("toto.txt");
        Entry root = INSTANCE.get(path.toString());
        while (root.getParent() != null) {
            root = root.getParent();
        }
        return root;
    }

    @Test
    void testMkdirCopyAndDelete() throws IOException {
        final Path tmpDirPath = tempDir.resolve("tmp");
        // First check tmp does not exist
        assertFalse(Files.exists(tmpDirPath));

        // Create a local Tree file (=> check mkdir)
        Folder tmpFolder = INSTANCE.get(tempDir.toString()).asFolder().mkdir(tmpDirPath.getFileName().toString());
        assertTrue(Files.exists(tmpDirPath));
        assertTrue(tmpFolder.exists());

        Folder subFolder = tmpFolder.mkdir("sub");
        assertTrue(subFolder.exists());

        // Check list
        List<Entry> entries = INSTANCE.get(tempDir.toString()).asFolder().list();
        assertEquals(3, entries.size());
        entries.forEach(entry -> {
            if (file.getName().equals(entry.getName())) {
                assertTrue(entry.isFile());
            } else {
                assertTrue(entry.isFolder());
            }
        });

        // Copy a fake file in sub folder to a non existing file
        String content = "test content";
        subFolder.copy("toBeCopied.txt", createFile(content), null);
        // Check that the file has been copied
        assertTrue(Files.exists(tmpDirPath.resolve("sub/toBeCopied.txt")));
        assertEquals(content, Files.readString(tmpDirPath.resolve("sub/toBeCopied.txt")));

        // Copy a fake file in sub folder to an existing file with consumer
        AtomicLong counter = new AtomicLong();
        content = "test content 2";
        subFolder.copy("toBeCopied.txt", createFile(content), counter::addAndGet);
        // Check that the file has been copied
        assertTrue(Files.exists(tmpDirPath.resolve("sub/toBeCopied.txt")));
        assertEquals(content, Files.readString(tmpDirPath.resolve("sub/toBeCopied.txt")));
        assertEquals(content.length(), counter.get());

        // Copy another fake file in sub folder to an existing file with consumer
        File other =subFolder.copy("other.txt", createFile("hello"), counter::addAndGet);

        // check delete file
        other.delete();
        assertFalse(other.exists());

        // Check delete the folder tree
        tmpFolder.delete();
        // Check that the file has been deleted
        assertFalse(Files.exists(tmpDirPath));
    }

    private File createFile(String content) throws IOException{
        File result = Mockito.mock(File.class);
        when(result.getInputStream()).thenReturn(new ByteArrayInputStream(content.getBytes()));
        return result;
    }
}
