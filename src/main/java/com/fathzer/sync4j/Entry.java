package com.fathzer.sync4j;

import java.io.IOException;

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
     * Returns the path of the parent of this entry.
     * @return the path of the parent of this entry, or null if this entry is the root.
     * @throws IOException if an I/O error occurs or if this entry does not exists and its parent exists and is a file
     */
    @Nullable
    String getParentPath() throws IOException;

    /**
     * Returns the parent of this entry.
     * @return the parent of this entry, or null if this entry is the root.
     * @throws IOException if an I/O error occurs or if this entry does not exists and its parent exists and is a file
     */
    @Nullable
    Entry getParent() throws IOException;
    
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
