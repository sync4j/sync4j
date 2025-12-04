package com.fathzer.sync4j.memory;

import java.io.IOException;
import java.util.Objects;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.FileProvider;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Abstract base class for in-memory entries (files and folders).
 */
abstract class MemoryEntry implements Entry {
    final String path;
    final MemoryFileProvider provider;

    protected MemoryEntry(@Nonnull String path, @Nonnull MemoryFileProvider provider) {
        this.path = Objects.requireNonNull(path, "Path must not be null");
        if (!path.isEmpty() && !path.startsWith("/")) {
            throw new IllegalArgumentException("Path must start with '/'");
        }
        this.provider = Objects.requireNonNull(provider);
    }

    @Override
    @Nonnull
    public String getName() {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    @Override
    @Nullable
    public String getParentPath() throws IOException {
        if (MemoryFileProvider.ROOT_PATH.equals(path)) {
            return null;
        }
        int lastSlash = path.lastIndexOf('/');
        return lastSlash == 0 ? MemoryFileProvider.ROOT_PATH : path.substring(0, lastSlash);
    }

    @Override
    @Nullable
    public MemoryFolder getParent() throws IOException {
        String parentPath = getParentPath();
        if (parentPath == null) {
            return null;
        }
        return (MemoryFolder)provider.get(parentPath);
    }

    @Override
    @Nonnull
    public FileProvider getFileProvider() {
        return provider;
    }

    void deleteParentReference() throws IOException {
        provider.checkWriteOperationsAllowed();
        getParent().removeChild(getName(), this);
    }
}
