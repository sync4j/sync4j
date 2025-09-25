package com.fathzer.sync4j;

import java.io.IOException;
import java.util.List;
import java.util.function.LongConsumer;

import jakarta.annotation.Nonnull;

public interface Folder extends Entry {
    /**
     * List the direct children of this file.
     * @return the list of children
     * @throws IOException if an I/O error occurs or if this file is not a folder
     */
    List<Entry> list() throws IOException;

    /**
     * Preload the tree structure of this folder (this is the fast-list feature). 
     * <br>
     * The default implementation throws an {@link UnsupportedOperationException} which is the expected behavior
     * if the provider does not support the fast-list feature.
     * @return this folder or a new folder with the same path but with the tree structure preloaded.
     * <br>If the folder is already preloaded, this method returns this folder and does nothing.
     * @throws IOException if an I/O error occurs
     * @throws UnsupportedOperationException if the provider does not support the fast-list feature
     * @see FileProvider#isFastListSupported()
     */
    default Folder preload() throws IOException {
        throw new UnsupportedOperationException("Preload (fast list) is not supported");
    }

    /**
     * Write a file in this folder.
     * <br>
     * If the file already exists, it is overwritten.
     * @param fileName the name of the file to write (can't be empty or null, can't contain path separator)
     * @param content the content of the file to write (can't be null)
     * @param progressListener an optional listener to track copy progress (can be null)
     * @throws IOException if an I/O error occurs
     */
    void copy(@Nonnull String fileName, @Nonnull File content, LongConsumer progressListener) throws IOException;
    
    default void checkFileName(String fileName) {
        if (fileName == null) {
            throw new NullPointerException("File name cannot be null");
        }
        if (fileName.trim().isEmpty() || fileName.contains("/")) {
            throw new IllegalArgumentException("File name cannot be empty or contain path separator");
        }
    }
}
