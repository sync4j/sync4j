package com.fathzer.sync4j;

import java.io.IOException;
import java.util.List;

import jakarta.annotation.Nonnull;

public interface FileProvider<T extends File> extends AutoCloseable {
    
    @Nonnull
    default List<HashAlgorithm> getSupportedHash() {
        return List.of();
    }

    @Nonnull
    default String getHash(@Nonnull T file, @Nonnull HashAlgorithm hashAlgorithm) throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    default void close() {
        // Does nothing by default
    }
}

