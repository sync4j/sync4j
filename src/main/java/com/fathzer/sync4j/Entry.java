package com.fathzer.sync4j;

import java.io.IOException;
import java.util.LinkedList;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * A generic entry (file or folder), whatever is its provider.
 */
public interface Entry {
    /**
     * Returns true if this entry is an existing file.
     * @return true if this entry is an existing file, false if it is a folder or a non existing entry
     */
    boolean isFile();

    /**
     * Returns true if this entry is an existing folder.
     * @return true if this entry is an existing folder, false if it is a file or a non existing entry
     */
    boolean isFolder();

    /**
     * Returns this entry as a file.
     * @return this entry as a file
     * @throws IllegalStateException if this entry is not a file
     * @see #isFile()
     */
    @Nonnull
    default File asFile() {
        if (isFile()) {
            return (File)this;
        }
        throw new IllegalStateException("Entry " + getName() + " is not a file");
    }

    /**
     * Returns this entry as a folder.
     * @return this entry as a folder
     * @throws IllegalStateException if this entry is not a folder
     * @see #isFolder()
     */
    @Nonnull
    default Folder asFolder() {
        if (isFolder()) {
            return (Folder)this;
        }
        throw new IllegalStateException("Entry " + getName() + " is not a folder");
    }

    /**
     * Returns true if this entry exists.
     * <br>This method returns true if this entry is a file or a folder.
     * @return true if this entry exists, false if it does not exist
     */
    default boolean exists() {
        return isFile() || isFolder();
    }

    /**
     * Returns the name of this entry.
     * @return the name of this entry
     */
    @Nonnull
    String getName();

    /**
     * Returns the parent of this entry.
     * @return the parent of this entry, or null if this entry is the root.
     * <br>Warning, there is no guarantee on the effective type of the returned entry:
     * <br> - if this exists it is a folder.
     * <br> - if this does not exist it is a folder, a non existing entry and even a file.
     * @throws IOException if an I/O error occurs
     */
    @Nullable
    Entry getParent() throws IOException;

    /**
     * Returns the path of this entry.
     * @return the full path (relative to the root, including the entry name) of this entry
     */
    @Nonnull
    default String getPath() throws IOException {
        LinkedList<String> segments = new LinkedList<>();
        Entry current = this;
        while (current != null) {
            segments.addFirst(current.getName());
            current = current.getParent();
        }
        return segments.stream().collect(Collectors.joining("/"));
    }
    
    /**
     * Deletes this entry.
     * <br>This method deletes recursively folders.
     * <br>For non existing entries, the method does nothing.
     * <br>Warning, depending on the {@link FileProvider} implementation, using this instance after deletion may have unexpected results.
     * <br>And, in case of this being a folder, using any of its children may also have unexpected results!
     * @throws IOException if an I/O error occurs
     */
    void delete() throws IOException;

    /**
     * Returns the provider that created this entry.
     * @return a FileProvider
     */
    @Nonnull
    FileProvider getFileProvider();
}
