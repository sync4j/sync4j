package com.fathzer.sync4j.file;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fathzer.sync4j.AbstractFileProviderTest;
import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.File;
import com.fathzer.sync4j.FileProvider;
import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.HashAlgorithm;

@ExtendWith(MockitoExtension.class)
class LocalFileTest extends AbstractFileProviderTest {
    @TempDir
    private Path tempDir;

    @Override
    protected FileProvider createFileProvider() {
        return new LocalProvider(tempDir);
    }

    @Override
    protected void createFolder(String path) throws IOException {
        Files.createDirectory(tempDir.resolve(path));
    }

    @Override
    protected void createFile(String path) throws IOException {
        Files.createFile(tempDir.resolve(path));
    }

    @Mock
    private HashAlgorithm mockHashAlgorithm;

    @Test
    void testHashList() {
        assertEquals(Arrays.asList(HashAlgorithm.values()), provider.getSupportedHash());
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
            long actualLastModified = localFile.getLastModifiedTime();
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

    @Test
    void testMkdirCopyAndDelete() throws IOException {
        final String tmpDirPath = "tmp";

        // Create a local Tree file (=> check mkdir)
        Entry dummy = provider.get(tempDir.toString());
        Folder tmpFolder = dummy.asFolder().mkdir(tmpDirPath);
        assertTrue(Files.exists(tempDir.resolve(tmpDirPath)));
        assertTrue(tmpFolder.exists());

        Folder subFolder = tmpFolder.mkdir("sub");
        assertTrue(subFolder.exists());

        // Check list
        List<Entry> entries = provider.get(tempDir.toString()).asFolder().list();
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
        subFolder.copy("toBeCopied.txt", createMockFile(content), null);
        // Check that the file has been copied
        assertTrue(Files.exists(tempDir.resolve(tmpDirPath).resolve("sub/toBeCopied.txt")));
        assertEquals(content, Files.readString(tempDir.resolve(tmpDirPath).resolve("sub/toBeCopied.txt")));

        // Copy a fake file in sub folder to an existing file with consumer
        AtomicLong counter = new AtomicLong();
        content = "test content 2";
        subFolder.copy("toBeCopied.txt", createMockFile(content), counter::addAndGet);
        // Check that the file has been copied
        assertTrue(Files.exists(tempDir.resolve(tmpDirPath).resolve("sub/toBeCopied.txt")));
        assertEquals(content, Files.readString(tempDir.resolve(tmpDirPath).resolve("sub/toBeCopied.txt")));
        assertEquals(content.length(), counter.get());

        // Copy another fake file in sub folder to an existing file with consumer
        File other = subFolder.copy("other.txt", createMockFile("hello"), counter::addAndGet);

        // check delete file
        other.delete();
        assertFalse(other.exists());

        // Check delete the folder tree
        tmpFolder.delete();
        // Check that the file has been deleted
        assertFalse(Files.exists(tempDir.resolve(tmpDirPath)));
    }
}
