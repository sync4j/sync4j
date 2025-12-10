package com.fathzer.sync4j.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.HashAlgorithm;
import com.fathzer.sync4j.helper.AbstractFileProvider;

/**
 * A local file provider.
 */
public class LocalProvider extends AbstractFileProvider {
    final Path rootPath;

    /**
     * Constructor.
     * @param rootPath the path of the folder that will be the root of the provider.
     */
    public LocalProvider(Path rootPath) {
        super(true, List.of(HashAlgorithm.values()), false);
        if (!Files.isDirectory(rootPath)) {
            throw new IllegalArgumentException("Root path must be a directory");
        }
        this.rootPath = rootPath;
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
