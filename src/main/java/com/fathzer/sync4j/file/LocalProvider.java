package com.fathzer.sync4j.file;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.FileProvider;
import com.fathzer.sync4j.HashAlgorithm;

/**
 * A local file provider.
 */
public class LocalProvider implements FileProvider {
    @Override
    public List<HashAlgorithm> getSupportedHash() {
        return List.of(HashAlgorithm.values());
    }

    @Override
    public Entry get(String path) throws IOException {
        return new LocalFile(Paths.get(path));
    }
}
