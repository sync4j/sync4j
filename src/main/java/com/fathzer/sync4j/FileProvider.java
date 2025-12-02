package com.fathzer.sync4j;

import java.io.IOException;
import java.util.List;

import jakarta.annotation.Nonnull;

/**
 * A provider of files.
  */
public interface FileProvider extends AutoCloseable {
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
     * <br>
     * By default, this method returns false.
     * 
     * @return a boolean
     */
    default boolean isReadOnlySupported() {
        return false;
    }

    /**
     * Returns true if the provider is read-only.
     * <br>
     * By default, this method returns false.
     * 
     * @return a boolean
     */
    default boolean isReadOnly() {
        return false;
    }

    /**
     * Sets the provider to read-only mode.
     * <br>
     * By default, this method throws an UnsupportedOperationException.
     * 
     * @param readOnly a boolean
     */
    default void setReadOnly(boolean readOnly) {
        throw new UnsupportedOperationException();
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