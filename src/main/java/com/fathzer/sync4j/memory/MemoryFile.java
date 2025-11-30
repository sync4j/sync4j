package com.fathzer.sync4j.memory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.fathzer.sync4j.File;
import com.fathzer.sync4j.HashAlgorithm;

import jakarta.annotation.Nonnull;

/**
 * In-memory implementation of a file.
 */
class MemoryFile extends MemoryEntry implements File {
    private byte[] content;
    private long creationTime;
    private long lastModified;

    MemoryFile(@Nonnull String path, @Nonnull MemoryFileProvider provider, @Nonnull byte[] content, long creationTime,
            long lastModified) {
        super(path, provider);
        this.content = content.clone();
        this.creationTime = creationTime;
        this.lastModified = lastModified;
    }

    void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    /**
     * Sets the content of the file.
     * <p>
     * This method updates the last modified time to the current time.
     * </p>
     * 
     * @param content the content of the file
     */
    public void setContent(byte[] content) {
        this.setContent(content, System.currentTimeMillis());
    }

    void setContent(byte[] content, long lastModified) {
        this.content = content.clone();
        this.lastModified = lastModified;
    }

    @Override
    public boolean isFile() {
        return exists();
    }

    @Override
    public boolean isFolder() {
        return false;
    }

    @Override
    public long getSize() throws IOException {
        if (!exists()) {
            throw new IOException("File does not exist: " + path);
        }
        return content.length;
    }

    @Override
    public long getCreationTime() throws IOException {
        if (content == null) {
            throw new IOException("File does not exist: " + path);
        }
        return creationTime;
    }

    @Override
    public long getLastModified() throws IOException {
        if (content == null) {
            throw new IOException("File does not exist: " + path);
        }
        return lastModified;
    }

    @Override
    @Nonnull
    public String getHash(@Nonnull HashAlgorithm hashAlgorithm) throws IOException {
        if (content == null) {
            throw new IOException("File does not exist: " + path);
        }
        return hashAlgorithm.computeHash(content);
    }

    @Override
    @Nonnull
    public InputStream getInputStream() throws IOException {
        if (content == null) {
            throw new IOException("File does not exist: " + path);
        }
        return new ByteArrayInputStream(content);
    }

    @Override
    public void delete() throws IOException {
        super.delete();
        content = null;
    }
}
