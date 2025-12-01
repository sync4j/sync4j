package com.fathzer.sync4j.memory;

import java.io.IOException;
import java.util.List;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.HashAlgorithm;
import com.fathzer.sync4j.helper.AbstractFileProvider;

import jakarta.annotation.Nonnull;

/**
 * In-memory implementation of FileProvider.
 * Files and folders are stored in memory using a thread safe tree structure where each folder maintains its own children.
 * <br><br>
 * This provider supports all hash algorithms and read-only mode.
 */
public class MemoryFileProvider extends AbstractFileProvider {
    private final MemoryFolder root;

    /**
     * Creates a new in-memory file provider.
     */
    public MemoryFileProvider() {
        super(true, List.of(HashAlgorithm.values()), false);
        this.root = new MemoryFolder("/", this);
    }

    @Override
    @Nonnull
    public Entry get(@Nonnull String path) throws IOException {
        if (path.equals("/")) {
            return root;
        }

        // Split path into segments and navigate the tree
        String[] segments = path.substring(1).split("/");
        MemoryFolder currentFolder = root;

        for (int i = 0; i < segments.length - 1; i++) {
            MemoryEntry child = currentFolder.getChild(segments[i]);
            if (child == null || !child.isFolder()) {
                // A parent folder does not exist
                return new MemoryFile(path, this, null, 0, 0);
            }
            currentFolder = (MemoryFolder) child;
        }

        // Get the final entry
        String lastName = segments[segments.length - 1];
        MemoryEntry entry = currentFolder.getChild(lastName);
        return entry == null ? new MemoryFile(path, this, null, 0, 0) : entry;
    }

    /**
     * Deletes an entry from the in-memory file system.
     * If the entry is a folder, all children are deleted recursively.
     * 
     * @param path the path of the entry to delete
     * @throws IOException if the provider is read-only
     *
    void deleteEntry(@Nonnull String path) throws IOException {
        checkReadOnly();
        
        if (path.equals("/")) {
            throw new IOException("Cannot delete root folder");
        }

        // Navigate to parent and remove child
        int lastSlash = path.lastIndexOf('/');
        String parentPath = lastSlash <= 0 ? "/" : path.substring(0, lastSlash);
        String name = path.substring(lastSlash + 1);

        Entry parentEntry = get(parentPath);
        if (!parentEntry.isFolder()) {
            return; // Parent doesn't exist or is not a folder
        }

        MemoryFolder parent = (MemoryFolder) parentEntry;
        parent.removeChild(name);
    }

    boolean exists(@Nonnull String path) {
        if (path.equals("/")) {
            return true; // Root always exists
        }

        // Navigate the tree to check existence
        String[] segments = path.substring(1).split("/");
        MemoryFolder currentFolder = root;

        for (int i = 0; i < segments.length - 1; i++) {
            MemoryEntry child = currentFolder.getChild(segments[i]);
            if (child == null || !child.isFolder()) {
                return false;
            }
            currentFolder = (MemoryFolder) child;
        }

        // Check if the final entry exists
        String lastName = segments[segments.length - 1];
        return currentFolder.getChild(lastName) != null;
    }*/

    void checkWriteOperationsAllowed() throws IOException {
        super.checkReadOnly();
    }
}
