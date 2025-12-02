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
    /** The root path that can be passed to {@link #get(String)}. */
    public static final String ROOT_PATH = "";

    private final MemoryFolder root;

    /**
     * Creates a new in-memory file provider.
     */
    public MemoryFileProvider() {
        super(true, List.of(HashAlgorithm.values()), false);
        this.root = new MemoryFolder(ROOT_PATH, this);
    }

    @Override
    @Nonnull
    public Entry get(@Nonnull String path) throws IOException {
        if (path.equals(ROOT_PATH)) {
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

    void checkWriteOperationsAllowed() throws IOException {
        super.checkReadOnly();
    }
}
