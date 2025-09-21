package com.fathzer.sync4j;

import java.io.IOException;
import java.util.List;

import jakarta.annotation.Nonnull;

public interface File {
    boolean isFile();

    String getName();

    long getSize() throws IOException;

    long getLastModified() throws IOException;

    /**
     * List the direct children of this file.
     * @return the list of children
     * @throws IOException if an I/O error occurs
     */
    List<File> list() throws IOException;

    default String getHash(@Nonnull HashAlgorithm hashAlgorithm) throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }
}
