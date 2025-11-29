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
@SuppressWarnings("java:S6548")
public class LocalProvider implements FileProvider {
    private boolean readOnly;

    /**
     * Constructor.
     */
    public LocalProvider() {
        this.readOnly = false;
    }

    @Override
    public boolean isReadOnlySupported() {
        return true;
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    @Override
    public List<HashAlgorithm> getSupportedHash() {
        return List.of(HashAlgorithm.values());
    }

    @Override
    public Entry get(String path) throws IOException {
        return new LocalFile(Paths.get(path), this);
    }
}
