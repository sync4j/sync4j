package com.fathzer.sync4j;

import java.io.IOException;
import java.util.List;

import jakarta.annotation.Nonnull;

/**
 * A generic file or a folder, whatever is its provider.
 */
public interface File {
    /**
     * Returns true if this file is a file.
     * @return true if this file is a file, false if it is a folder or a non existing file
     */
    boolean isFile();

    /**
     * Returns the name of this file.
     * @return the name of this file
     */
    String getName();

    /**
     * Returns true if this file exists.
     * @return true if this file exists
     */
    boolean exists();

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
        return getLastModified();
    }

    /**
     * Returns the last modified time of this file in ms since the epoch.
     * <br>For folders and non existing files, the method is not specified (it could throw an exception or return a value).
     * @return the last modified time of this file
     * @throws IOException if an I/O error occurs
     */
    long getLastModified() throws IOException;

    /**
     * List the direct children of this file.
     * @return the list of children
     * @throws IOException if an I/O error occurs or if this file is not a folder
     */
    List<File> list() throws IOException;

    /**
     * Returns the hash of this file.
     * @param hashAlgorithm the hash algorithm to use
     * @return the hash of this file
     * @throws IOException if an I/O error occurs or if this file is not a file
     * @throws UnsupportedOperationException if the hash algorithm is not supported
     */
    default String getHash(@Nonnull HashAlgorithm hashAlgorithm) throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }
}
