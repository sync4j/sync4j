package com.fathzer.sync4j.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.stream.Stream;

import com.fathzer.sync4j.File;
import com.fathzer.sync4j.HashAlgorithm;

public class LocalFile implements File {
    private final Path path;

    LocalFile(Path path) {
        this.path = path;
    }

    @Override
    public boolean exists() {
        return Files.exists(path);
    }
    
    @Override
    public boolean isFile() {
        return Files.isRegularFile(path);
    }

    @Override
    public String getName() {
        return path.getFileName().toString();
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
    public List<File> list() throws IOException {
        try (Stream<Path> stream = Files.list(path)) {
            return stream.map(p -> (File) new LocalFile(p)).toList();
        }
    }
}

