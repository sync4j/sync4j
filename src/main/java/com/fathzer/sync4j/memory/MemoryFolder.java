package com.fathzer.sync4j.memory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongConsumer;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.File;
import com.fathzer.sync4j.Folder;

import jakarta.annotation.Nonnull;

/**
 * In-memory implementation of a folder.
 */
class MemoryFolder extends MemoryEntry implements Folder {

    MemoryFolder(@Nonnull String path, @Nonnull MemoryFileProvider provider) {
        super(path, provider);
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
    public List<Entry> list() throws IOException {
        if (!exists()) {
            throw new IOException("Folder does not exist: " + path);
        }
        return new ArrayList<>(provider.listChildren(path));
    }

    @Override
    @Nonnull
    public File copy(@Nonnull String fileName, @Nonnull File content, LongConsumer progressListener) throws IOException {
        if (!exists()) {
            throw new IOException("Folder does not exist: " + path);
        }
        checkFileName(fileName);

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
        MemoryFile newFile = new MemoryFile(childPath, provider, data);
        provider.addEntry(childPath, newFile);

        return newFile;
    }

    @Override
    @Nonnull
    public Folder mkdir(@Nonnull String folderName) throws IOException {
        if (!exists()) {
            throw new IOException("Folder does not exist: " + path);
        }
        checkFileName(folderName);

        String childPath = buildChildPath(folderName);

        // Check if already exists
        Entry existing = provider.get(childPath);
        if (existing.exists()) {
            throw new IOException("Entry already exists: " + childPath);
        }

        MemoryFolder newFolder = new MemoryFolder(childPath, provider);
        provider.addEntry(childPath, newFolder);

        return newFolder;
    }

    private String buildChildPath(String name) {
        return path.equals("/") ? "/" + name : path + "/" + name;
    }
}
