package com.fathzer.sync4j.memory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
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
        MemoryFile newFile = new MemoryFile(childPath, provider, data, System.currentTimeMillis(), System.currentTimeMillis());
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

    /**
     * Creates a file in this folder with the given content.
     * <p>
     * The file is created with the current time as creation and last modified time.
     * If the file already exists, it is overwritten and its last modified time is updated (creation time is left unchanged).
     * </p>
     * @param name    the name of the file (not a path)
     * @param content the content of the file
     * @return the created file
     * @throws IOException if the folder does not exist, the file name is invalid, or the provider is read-only
     */
    @Nonnull
    public File createFile(@Nonnull String name, @Nonnull byte[] content) throws IOException {
        return createFile(name, content, OptionalLong.empty(), OptionalLong.empty());
    }

    /**
     * Creates a file in this folder with the given content and timestamps.
     * <p>
     * The file is created with the current time as creation and last modified time.
     * </p>
     * @param name         the name of the file (not a path)
     * @param content      the content of the file
     * @param creationTime the creation time in milliseconds since epoch
     * @param lastModified the last modified time in milliseconds since epoch
     * @return the created file
     * @throws IOException if the folder does not exist, the file name is invalid, or the provider is read-only
     */
    @Nonnull
    public File createFile(@Nonnull String name, @Nonnull byte[] content, long creationTime, long lastModified) throws IOException {
        return createFile(name, content, OptionalLong.of(creationTime), OptionalLong.of(lastModified));
    }
    
    private File createFile(@Nonnull String name, @Nonnull byte[] content, OptionalLong creationTime, OptionalLong lastModified) throws IOException {
        if (!exists()) {
            throw new IOException("Folder does not exist: " + path);
        }
        checkFileName(name);

        String childPath = buildChildPath(name);
        long now = System.currentTimeMillis();
        long modifiedTime = lastModified.isPresent() ? lastModified.getAsLong() : now;

        // Check if already exists
        Entry existing = provider.get(childPath);
        if (existing.isFolder()) {
            throw new IOException("Folder already exists: " + childPath);
        } else if (existing.isFile()) {
            MemoryFile file = (MemoryFile) existing;
            file.setContent(content, modifiedTime);
            if (creationTime.isPresent()) {
                file.setCreationTime(creationTime.getAsLong());
            }
            return file;
        } else {
            MemoryFile newFile = new MemoryFile(childPath, provider, content, creationTime.isPresent() ? creationTime.getAsLong() : now, modifiedTime);
            provider.addEntry(childPath, newFile);
            return newFile;
        }
    }

    private String buildChildPath(String name) {
        return path.equals("/") ? "/" + name : path + "/" + name;
    }
}
