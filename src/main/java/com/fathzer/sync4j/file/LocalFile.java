package com.fathzer.sync4j.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Objects;
import java.util.function.LongConsumer;
import java.util.stream.Stream;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.File;
import com.fathzer.sync4j.FileProvider;
import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.HashAlgorithm;
import com.fathzer.sync4j.util.ProgressInputStream;

/**
 * A local file.
 */
class LocalFile implements File, Folder {
    private final Path path;
    private final LocalProvider provider;

    /**
     * Constructor.
     * @param path the path of the file
     */
    LocalFile(Path path, LocalProvider provider) {
        this.path = path.toAbsolutePath();
        this.provider = provider;
    }

    @Override
    public FileProvider getFileProvider() {
        return provider;
    }
    
    @Override
    public boolean isFile() {
        return Files.isRegularFile(path);
    }

    @Override
    public boolean exists() {
        return Files.exists(path);
    }

    @Override
    public boolean isFolder() {
        return Files.isDirectory(path);
    }

    @Override
    public Entry getParent() {
        final Path parent = path.getParent();
        return parent == null ? null : new LocalFile(parent, provider);
    }
    
    @Override
    public String getParentPath() {
        final Path parent = path.getParent();
        return parent==null ? null : parent.toString();
    }
    
    @Override
    public String getName() {
        final Path fileName = path.getFileName();
        return fileName == null ? "" : fileName.toString();
    }

    @Override
    public long getSize() throws IOException {
        return Files.size(path);
    }

    @Override
    public long getCreationTime() throws IOException {
        final FileTime attribute = (FileTime) Files.getAttribute(path, "creationTime");
        return attribute.toMillis();
    }

    @Override
    public long getLastModified() throws IOException {
        return Files.getLastModifiedTime(path).toMillis();
    }

    @Override
    public String getHash(HashAlgorithm hashAlgorithm) throws IOException {
        return hashAlgorithm.computeHash(path);
    }

    @Override
    public List<Entry> list() throws IOException {
        try (Stream<Path> stream = Files.list(path)) {
            return stream.map(p -> (Entry) new LocalFile(p, provider)).toList();
        }
    }

    @Override
    public void delete() throws IOException {
        checkReadOnly();
        if (isFolder()) {
            deletedFolder(path);
        } else if (exists()) {
            Files.delete(path);
        }
    }

    private void deletedFolder(Path pathToBeDeleted) throws IOException {
        Files.walkFileTree(pathToBeDeleted, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return Files.newInputStream(path);
    }

    @Override
    public File copy(String fileName, File content, LongConsumer progressListener) throws IOException {
        checkReadOnly();
        checkFileName(fileName);
        Objects.requireNonNull(content, "Content cannot be null");
        
        final Path targetPath = path.resolve(fileName);
        
        try (InputStream in = progressListener != null 
                ? new ProgressInputStream(content.getInputStream(), progressListener)
                : content.getInputStream()) {
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
        
        // Copy file attributes
        Files.setLastModifiedTime(targetPath, FileTime.fromMillis(content.getLastModified()));
        
        // Note: Setting creation time is platform dependent and may not work on all systems
        try {
            Files.setAttribute(targetPath, "creationTime", FileTime.fromMillis(content.getCreationTime()));
        } catch (UnsupportedOperationException | IOException e) {
            // Ignore if setting creation time is not supported
        }
        return new LocalFile(targetPath, provider);
    }

    @Override
    public Folder mkdir(String folderName) throws IOException {
        checkReadOnly();
        checkFileName(folderName);
        final Path targetPath = path.resolve(folderName);
        Files.createDirectory(targetPath);
        return new LocalFile(targetPath, provider);
    }

    private void checkReadOnly() throws IOException {
        if (provider.isReadOnly()) {
            throw new IOException("Provider is read-only");
        }
    }

    @Override
    public String toString() {
        return "local:" + path.toAbsolutePath().toString() + (provider.isReadOnly() ? " (read-only)" : "");
    }
}

