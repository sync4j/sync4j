package com.fathzer.sync4j.memory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongConsumer;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.File;
import com.fathzer.sync4j.Folder;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * In-memory implementation of a folder.
 */
public class MemoryFolder extends MemoryEntry implements Folder {
    private final AtomicBoolean exists;
    private Map<String, MemoryEntry> children;

    MemoryFolder(@Nonnull String path, @Nonnull MemoryFileProvider provider) {
        super(path, provider);
        this.exists = new AtomicBoolean(true);
        this.children = new HashMap<>();
    }
    
    /**
     * Gets a child entry by name.
     * 
     * @param name the name of the child (not a path)
     * @return the child entry, or null if not found
     */
    @Nullable
    synchronized MemoryEntry getChild(@Nonnull String name) {
        return children.get(name);
    }
    
    /**
     * Removes a child entry by name.
     * 
     * @param name the name of the child to remove
     * @param expectedEntry the expected entry to remove
     */
    synchronized void removeChild(@Nonnull String name, @Nonnull Entry expectedEntry) {
        // Warning if entry has been replaced by another thread, it should not been replaced
        if (expectedEntry == children.get(name)) {
            children.remove(name);
        }
    }

    @Override
    public boolean isFile() {
        return false;
    }

    @Override
    public boolean isFolder() {
        return exists();
    }

    @Override
    @Nonnull
    public MemoryFolder mkdir(@Nonnull String folderName) throws IOException {
        if (!exists()) {
            throw new IOException("Folder does not exist: " + path);
        }
        checkFileName(folderName);

        provider.checkWriteOperationsAllowed();

        String childPath = buildChildPath(folderName);

        // Synchronized to prevent race condition on check-then-create
        synchronized (this) {
            checkChildExists(folderName);
            MemoryFolder newFolder = new MemoryFolder(childPath, provider);
            children.put(folderName, newFolder);
            return newFolder;
        }
    }

    private void checkChildExists(@Nonnull String childName) throws IOException {
        MemoryEntry existing = children.get(childName);
        if (existing != null && existing.exists()) {
            // Warning if entry is being deleted by another thread, it could still be in the child map
            throw new IOException("Entry already exists: " + buildChildPath(childName));
        }
    }

    @Override
    public void delete() throws IOException {
        if (MemoryFileProvider.ROOT_PATH.equals(this.path)) {
            throw new IOException("Cannot delete root folder");
        }
        if (exists.compareAndSet(true, false)) {
            deleteParentReference();
            deleteRecursively();
        }
    }

    private void deleteRecursively() {
        MemoryEntry[] entries;
        synchronized (this) {
            entries = this.children.values().toArray(new MemoryEntry[0]);
            this.children.clear();
        }
        for (MemoryEntry entry : entries) {
            if (entry.isFile()) {
                ((MemoryFile)entry).markDeleted();
            } else if (entry.isFolder()) {
                MemoryFolder folder = (MemoryFolder) entry;
                folder.exists.set(false);
                folder.deleteRecursively();
            }
        }
    }

    @Override
    @Nonnull
    public List<Entry> list() throws IOException {
        if (!exists()) {
            throw new IOException("Folder does not exist: " + path);
        }
        synchronized (this) {
            return new ArrayList<>(children.values());
        }
    }

    @Override
    @Nonnull
    public MemoryFile copy(@Nonnull String fileName, @Nonnull File content, LongConsumer progressListener) throws IOException {
        if (!exists()) {
            throw new IOException("Folder does not exist: " + path);
        }
        checkFileName(fileName);

        provider.checkWriteOperationsAllowed();

        String childPath = buildChildPath(fileName);

        // Read content from source file
        byte[] data;
        try (InputStream is = content.getInputStream()) {
            data = is.readAllBytes();
            if (progressListener != null) {
                progressListener.accept(data.length);
            }
        }

        // Create new file in this folder
        MemoryFile newFile = new MemoryFile(childPath, provider, data, content.getCreationTime(), content.getLastModifiedTime());
        
        synchronized (this) {
            children.put(fileName, newFile);
        }

        return newFile;
    }

    /**
     * Creates a file in this folder with the given content.
     * <p>
     * The file is created with the current time as creation and last modified time.
     * If the file already exists, it is overwritten and its last modified time is updated (creation time is left unchanged).
     * </p>
     * @param name    the name of the file (not a path)
     * @param content the content of the file
     * @return the created file
     * @throws IOException if the folder does not exist, the file name is invalid, the provider is read-only or the file exists
     */
    @Nonnull
    public MemoryFile createFile(@Nonnull String name, @Nonnull byte[] content) throws IOException {
        if (!exists()) {
            throw new IOException("Folder does not exist: " + path);
        }
        checkFileName(name);

        String childPath = buildChildPath(name);
        long now = System.currentTimeMillis();

        provider.checkWriteOperationsAllowed();

        // Synchronized to prevent race condition on check-then-create/update
        MemoryFile newFile;
        synchronized (this) {
            checkChildExists(name);
            newFile = new MemoryFile(childPath, provider, content, now, now);
            children.put(name, newFile);
        }
        newFile.setCreationTime(now);
        newFile.setLastModifiedTime(now);
        return newFile;
    }

    private String buildChildPath(String name) {
        return path + "/" + name;
    }

    @Override
    public boolean exists() {
        return exists.get();
    }

    @Override
    public String toString() {
        return "MemoryFolder{" + path + '}';
    }
}
