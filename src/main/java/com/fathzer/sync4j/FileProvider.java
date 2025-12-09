package com.fathzer.sync4j;

import java.io.IOException;
import java.util.List;

import com.fathzer.sync4j.sync.parameters.SyncParameters;

import jakarta.annotation.Nonnull;

/**
 * A minimalist provider of files.
 * <br>A file provider is responsible for fetching files and folders from a remote or local storage.
 * <br>Here is a list of the rules that must be respected by a file provider:
 * <ul>
 * <li>It exposes a strict tree of files and folders (no symlinks, no hard links, no junctions, only one root).</li>
 * <li>It uses a standard path syntax with '/' as the path separator.</li>
 * <li>File and folder names can't be empty, except for the root folder.</li>
 * <li>The root folder can't be deleted.</li>
 * <li>File and folder names can't contain '/' (depending on the provider, other characters may also be forbidden).</li>
 * <li>It should preferably be thread-safe. If it does not support concurrent operations, the {@link SyncParameters#performance()} must be configured to use prevent unsupported concurrent operations.
 * <br>The {@link Entry} objects returned by the provider can be thread-safe or not.</li>
 * </ul>
*/
public interface FileProvider extends AutoCloseable {
    /**
     * The root path.
     * <br>Calling {@link #get(String)} with this path returns the root folder.
     */
    public static final String ROOT_PATH = "";

    /**
     * Returns the list of hash algorithms supported by this provider.
     * @return the list of hash algorithms supported by this provider ordered from best to worst.
     */
    @Nonnull
    default List<HashAlgorithm> getSupportedHash() {
        return List.of();
    }

    /**
     * Returns true if the provider supports the fast-list feature.
     * <br>The fast list feature allows to recursively fetch metadata of all files
     * and (sub-)folders of a folder during the {@link FileProvider#get(String)} call.
     * Even if it is provider dependent, it is recommended to implement this feature by a unique call
     * instead of fetching metadata of each file and (sub-)folder separately.
     * <br>Of course, for very large datasets, using this feature may cause out of memory errors.
     * @return a boolean
     * @see Folder#preload()
     */
    default boolean isFastListSupported() {
        return false;
    }

    /**
     * Returns true if the provider supports read-only operations.
     * <br>By default, this method returns true.
     * @return a boolean
     */
    default boolean isWriteSupported() {
        return true;
    }

    /**
     * Returns true if the provider is in read-only mode.
     * <br>By default, this method returns true if the provider does not support write operations and false otherwise.
     * @return true if this provider is in read-only mode, false otherwise
     */
    default boolean isReadOnly() {
        return !this.isWriteSupported();
    }

    /**
     * Sets the provider to read-only mode.
     * <br>
     * By default, this method throws an UnsupportedOperationException if the provider does not support write operations and the provider is <code>>readOnly</code> is true.
     * It does nothing otherwise.
     * @param readOnly true to set the provider to read-only mode, false otherwise
     */
    default void setReadOnly(boolean readOnly) {
        if (!isWriteSupported() && !readOnly) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Returns the file or folder at the given path.
     * @param path the path of the file. <br>
     * <b>Warning</b>: The path syntax is provider and/or platform dependent.
     * It is the responsibility of the caller to provide a valid path for the provider and the platform.
     * @return the file or folder, even if it does not exist (see {@link Entry#exists()})
     * @throws IOException if an I/O error occurs
     */
    @Nonnull
    Entry get(@Nonnull String path) throws IOException;

    /**
     * Closes this provider.
     * <br>This method is called when the provider is no longer needed.
     * <br>It should release any resources held by the provider.
     */
    @Override
    default void close() {
        // Does nothing by default
    }
}