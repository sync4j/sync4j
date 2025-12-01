package com.fathzer.sync4j;

import java.io.IOException;
import java.io.InputStream;

import jakarta.annotation.Nonnull;

/**
 * A generic file, whatever is its provider.
 */
public interface File extends Entry {
    /**
     * Returns the size of this file.
     * <br>For folders and non existing files, the method is not specified (it could throw an exception or return a value).
     * @return the size of this file
     * @throws IOException if an I/O error occurs
     */
    long getSize() throws IOException;

    /**
     * Returns the creation time of this file in ms since the epoch.
     * <br>For folders and non existing files, the method is not specified (it could throw an exception or return a value).
     * @return the creation time of this file
     * @throws IOException if an I/O error occurs
     */
    default long getCreationTime() throws IOException {
        return getLastModifiedTime();
    }

    /**
     * Returns the last modified time of this file in ms since the epoch.
     * <br>For folders and non existing files, the method is not specified (it could throw an exception or return a value).
     * @return the last modified time of this file
     * @throws IOException if an I/O error occurs
     */
    long getLastModifiedTime() throws IOException;

    /**
     * Returns the hash of this file.
     * @param hashAlgorithm the hash algorithm to use
     * @return the hash of this file
     * @throws IOException if an I/O error occurs or if this file is not a file
     * @throws UnsupportedOperationException if the hash algorithm is not supported
     */
    @Nonnull
    default String getHash(@Nonnull HashAlgorithm hashAlgorithm) throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Returns an input stream to read the content of this file.
     * @return an input stream to read the content of this file
     * @throws IOException if an I/O error occurs
     */
    @Nonnull
    InputStream getInputStream() throws IOException;
}
