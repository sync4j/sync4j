package com.fathzer.sync4j.memory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.File;
import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.HashAlgorithm;
import com.fathzer.sync4j.helper.AbstractFileProvider;

import jakarta.annotation.Nonnull;

/**
 * In-memory implementation of FileProvider.
 * Files and folders are stored in memory using a Map structure.
 * <br><br>
 * This provider supports all hash algorithms and read-only mode.
 */
public class MemoryFileProvider extends AbstractFileProvider {
    //FIXME: This implementation is not thread-safe.

    private final Map<String, MemoryEntry> entries;

    /**
     * Creates a new in-memory file provider.
     */
    public MemoryFileProvider() {
        super(true, List.of(HashAlgorithm.values()), false);
        this.entries = new ConcurrentHashMap<>();
        // Create root folder
        entries.put("/", new MemoryFolder("/", this));
    }

    @Override
    @Nonnull
    public Entry get(@Nonnull String path) throws IOException {
        MemoryEntry entry = entries.get(path);
        return entry == null ? new NonExistingEntry(path, this) : entry;
    }

    /**
     * Adds an entry to the in-memory file system.
     * 
     * @param path  the path of the entry
     * @param entry the entry to add
     * @throws IOException if the provider is read-only
     */
    void addEntry(@Nonnull String path, @Nonnull MemoryEntry entry) throws IOException {
        checkReadOnly();
        entries.put(path, entry);
    }

    /**
     * Deletes an entry from the in-memory file system.
     * If the entry is a folder, all children are deleted recursively.
     * 
     * @param path the path of the entry to delete
     * @throws IOException if the provider is read-only
     */
    void deleteEntry(@Nonnull String path) throws IOException {
        checkReadOnly();
        MemoryEntry entry = entries.get(path);
        if (entry == null) {
            return;
        }

        // If it's a folder, delete all children recursively
        if (entry.isFolder()) {
            List<Entry> children = listChildren(path);
            for (Entry child : children) {
                child.delete();
            }
        }

        entries.remove(path);
    }

    boolean exists(@Nonnull String path) {
        return entries.containsKey(path);
    }

    /**
     * Lists all direct children of a folder.
     * 
     * @param folderPath the path of the folder
     * @return list of child entries
     */
    @Nonnull
    List<Entry> listChildren(@Nonnull String folderPath) {
        List<Entry> children = new ArrayList<>();
        String prefix = folderPath.equals("/") ? "/" : folderPath + "/";

        for (Map.Entry<String, MemoryEntry> entry : entries.entrySet()) {
            String entryPath = entry.getKey();
            if (entryPath.startsWith(prefix) && !entryPath.equals(folderPath)) {
                // Check if it's a direct child (no additional slashes after prefix)
                String remainder = entryPath.substring(prefix.length());
                if (!remainder.contains("/")) {
                    children.add(entry.getValue());
                }
            }
        }

        return children;
    }

    /**
     * Validates that the parent directory of the given path exists and is a folder.
     * 
     * @param path the path to validate
     * @throws IOException if the parent does not exist or is not a folder
     */
    private void validateParentExists(@Nonnull String path) throws IOException {
        if (path.equals("/")) {
            return; // Root has no parent
        }

        int lastSlash = path.lastIndexOf('/');
        if (lastSlash <= 0) {
            return; // Direct child of root
        }

        String parentPath = path.substring(0, lastSlash);
        if (parentPath.isEmpty()) {
            parentPath = "/";
        }

        Entry parent = get(parentPath);
        if (!parent.exists()) {
            throw new IOException("Parent directory does not exist: " + parentPath);
        }
        if (!parent.isFolder()) {
            throw new IOException("Parent is not a directory: " + parentPath);
        }
    }

    /**
     * Non-existing entry implementation.
     */
    private static class NonExistingEntry extends MemoryEntry {
        NonExistingEntry(@Nonnull String path, @Nonnull MemoryFileProvider provider) {
            super(path, provider);
        }

        @Override
        public boolean isFile() {
            return false;
        }

        @Override
        public boolean isFolder() {
            return false;
        }

        @Override
        public boolean exists() {
            return false;
        }
    }
}
