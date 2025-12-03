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
public class MemoryFile extends MemoryEntry implements File {
    private byte[] content;
    private long creationTime;
    private long lastModified;

    MemoryFile(@Nonnull String path, @Nonnull MemoryFileProvider provider, byte[] content, long creationTime,
            long lastModified) {
        super(path, provider);
        this.content = content == null ? null : content.clone();
        this.creationTime = creationTime;
        this.lastModified = lastModified;
    }

    /**
     * Sets the creation time of the file.
     * 
     * @param creationTime the creation time of the file
     * @throws IOException if the provider is read-only
     */
    public void setCreationTime(long creationTime) throws IOException {
        provider.checkWriteOperationsAllowed();
        this.creationTime = creationTime;
    }

    /**
     * Sets the last modified time of the file.
     * 
     * @param lastModified the last modified time of the file
     * @throws IOException if the provider is read-only
     */
    public void setLastModifiedTime(long lastModified) throws IOException {
        provider.checkWriteOperationsAllowed();
        this.lastModified = lastModified;
    }

    /**
     * Sets the content of the file.
     * <p>
     * This method updates the last modified time to the current time.
     * </p>
     * 
     * @param content the content of the file
     * @throws IOException if the provider is read-only
     */
    public void setContent(byte[] content) throws IOException {
        provider.checkWriteOperationsAllowed();
        this.content = content.clone();
        this.lastModified = System.currentTimeMillis();
    }

    @Override
    public boolean exists() {
        return content != null;
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
    public long getLastModifiedTime() throws IOException {
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
        deleteParentReference();
        markDeleted();
    }

    void markDeleted() {
        content = null;
    }
}
