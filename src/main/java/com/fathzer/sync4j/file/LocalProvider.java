package com.fathzer.sync4j.file;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.HashAlgorithm;
import com.fathzer.sync4j.helper.AbstractFileProvider;

/**
 * A local file provider.
 */
public class LocalProvider extends AbstractFileProvider {
    private static final String CREATION_TIME = "creationTime";
    private static final long CREATION_TIME_PRECISION;

    final Path rootPath;

    static {
        try {
            Path path = Files.createTempFile("sync4j", "test");
            try {
                FileTime creationTime = FileTime.fromMillis(123456789);
                Files.setAttribute(path, CREATION_TIME, creationTime);
                boolean isCreationTimeImmutable = !Files.getAttribute(path, CREATION_TIME).equals(creationTime);
                CREATION_TIME_PRECISION = isCreationTimeImmutable?Long.MAX_VALUE:0;
            } finally {
                Files.delete(path);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Constructor.
     * @param rootPath the path of the folder that will be the root of the provider.
     */
    public LocalProvider(Path rootPath) {
        super(true, List.of(HashAlgorithm.values()), false);
        this.rootPath = rootPath.toAbsolutePath();
        if (!Files.isDirectory(this.rootPath)) {
            throw new IllegalArgumentException("Root path (" + rootPath + ") must be a directory");
        }
    }

    @Override
    public long getCreationTimePrecision() {
        return CREATION_TIME_PRECISION;
    }

    @Override
    public Entry get(String path) throws IOException {
        this.checkPath(path);
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return new LocalFile(rootPath.resolve(path), this);
    }

    void checkWriteable() throws IOException {
        super.checkReadOnly();
    }
}
