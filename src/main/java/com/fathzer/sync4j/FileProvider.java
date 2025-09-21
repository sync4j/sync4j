package com.fathzer.sync4j;

import java.io.IOException;
import java.util.List;

import jakarta.annotation.Nonnull;

public interface FileProvider extends AutoCloseable {
    @Nonnull
    default List<HashAlgorithm> getSupportedHash() {
        return List.of();
    }

    default boolean isFastListSupported() {
        return false;
    }

    @Nonnull
    File get(@Nonnull String path, boolean recursive) throws IOException;

    default void close() {
        // Does nothing by default
    }
}