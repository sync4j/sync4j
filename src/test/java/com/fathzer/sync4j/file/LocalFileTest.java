package com.fathzer.sync4j.file;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.File;
import com.fathzer.sync4j.FileProvider;
import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.HashAlgorithm;
import com.fathzer.sync4j.test.AbstractFileProviderTest;

@ExtendWith(MockitoExtension.class)
class LocalFileTest extends AbstractFileProviderTest {
    private static final String CREATION_TIME = "creationTime";

    @TempDir
    private Path tempDir;

    @Override
    @SuppressWarnings("java:S2924")
    protected FileProvider createFileProvider(TestInfo testInfo) {
        return new LocalProvider(tempDir);
    }

    private class LocalFileSystem implements AbstractFileProviderTest.UnderlyingFileSystem {
		private Path toPath(String path) {
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            return tempDir.resolve(path);
        }

        @Override
        public void createFolder(String path) throws IOException {
            Files.createDirectory(toPath(path));
        }

        @Override
        public void createFile(String path) throws IOException {
            Files.createFile(toPath(path));
        }

        @Override
        public void delete(String path) throws IOException {
            Files.delete(toPath(path));
        }

        @Override
        public void assertUnderlyingFileEquals(String path, File file) throws IOException {
            Path filePath = toPath(path);
            assertEquals(file.getSize(), Files.size(filePath));
            try (var stream = file.getInputStream()) {
                assertArrayEquals(Files.readAllBytes(filePath), stream.readAllBytes());
            }
            assertEquals(Files.getLastModifiedTime(filePath).toMillis(), file.getLastModifiedTime());
            final FileTime attribute = (FileTime) Files.getAttribute(filePath, CREATION_TIME);
            assertEquals(attribute.toMillis(), file.getCreationTime());
        }

        @Override
        public void assertUnderlyingFolderExist(String path) throws IOException {
            assertTrue(Files.exists(toPath(path)));
        }
    }

    @Override
    protected UnderlyingFileSystem getUnderlyingFileSystem() {
        return new LocalFileSystem();
    }

    @Mock
    private HashAlgorithm mockHashAlgorithm;

    @Test
    void testIllegalArgumentExceptionInConstructor() throws IOException {
        assertThrows(NullPointerException.class, () -> new LocalProvider(null));
        final Path path = tempDir.resolve("nonExisting");
        assertFalse(Files.exists(path));
        assertThrows(IllegalArgumentException.class, () -> new LocalProvider(path));
        Files.createFile(path);
        assertThrows(IllegalArgumentException.class, () -> new LocalProvider(path));
    }

    @Test
    void testHashList() {
        assertEquals(Arrays.asList(HashAlgorithm.values()), provider.getSupportedHash());
    }

    @Test
    void testCreationTimePrecision() throws IOException {
        Path path = tempDir.resolve("creationTimeTest");
        if (Files.exists(path)) {
            throw new IllegalStateException("Unable to check creation time support without side effect: File " + path + " already exists");
        }
        try {
            Files.createFile(path);
            FileTime creationTime = FileTime.fromMillis(123456789);
            Files.setAttribute(path, CREATION_TIME, creationTime);
            boolean isCreationTimeImmutable = !Files.getAttribute(path, CREATION_TIME).equals(creationTime);
            assertEquals(isCreationTimeImmutable?Long.MAX_VALUE:0, provider.getCreationTimePrecision());
        } finally {
            Files.delete(path);
        }
    }

    @Test
    void testGetHashCallsComputeHash() throws IOException {
        // Given
        File localFile = root.copy("file.txt", createMockFile("content"), null);
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
        ufs.createFile("file.txt");
        File localFile = provider.get("/file.txt").asFile();

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
        Folder tmpFolder = root.mkdir(tmpDirPath);
        assertTrue(Files.exists(tempDir.resolve(tmpDirPath)));
        assertTrue(tmpFolder.exists());

        Folder subFolder = tmpFolder.mkdir("sub");
        assertTrue(subFolder.exists());

        // Check list
        List<Entry> entries = root.list();
        assertEquals(List.of("tmp"), entries.stream().map(Entry::getName).toList());

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

    @Test
    void bug20251113() throws IOException {
        try (LocalProvider other = new LocalProvider(java.nio.file.Paths.get(""))) {
            // getParent() and getName() of root folder did not return the right value
            Entry entry = other.get(FileProvider.ROOT_PATH);
            assertNull(entry.getParent());
            assertTrue(entry.getName().isEmpty());
        }
    }
}
