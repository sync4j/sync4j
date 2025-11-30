package com.fathzer.sync4j.file;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.HashAlgorithm;
import com.fathzer.sync4j.helper.AbstractFileProvider;

/**
 * A local file provider.
 */
@SuppressWarnings("java:S6548")
public class LocalProvider extends AbstractFileProvider {

    /**
     * Constructor.
     */
    public LocalProvider() {
        super(true, List.of(HashAlgorithm.values()), false);
    }

    @Override
    public Entry get(String path) throws IOException {
        return new LocalFile(Paths.get(path), this);
    }

    void checkWriteable() throws IOException {
        super.checkReadOnly();
    }
}
