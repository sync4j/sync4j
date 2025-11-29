package com.fathzer.sync4j.memory;

import java.io.IOException;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.FileProvider;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Abstract base class for in-memory entries (files and folders).
 */
abstract class MemoryEntry implements Entry {
    protected final String path;
    protected final MemoryFileProvider provider;

    protected MemoryEntry(@Nonnull String path, @Nonnull MemoryFileProvider provider) {
        this.path = path;
        this.provider = provider;
    }

    @Override
    @Nonnull
    public String getName() {
        return path;
    }

    @Override
    @Nullable
    public String getParentPath() throws IOException {
        if (path.equals("/")) {
            return null;
        }
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash == 0) {
            return "/";
        }
        if (lastSlash > 0) {
            return path.substring(0, lastSlash);
        }
        return null;
    }

    @Override
    @Nullable
    public Entry getParent() throws IOException {
        String parentPath = getParentPath();
        if (parentPath == null) {
            return null;
        }
        return provider.get(parentPath);
    }

    @Override
    public void delete() throws IOException {
        if (!exists()) {
            return;
        }
        provider.deleteEntry(path);
    }

    @Override
    public boolean exists() {
        return provider.exists(path);
    }

    @Override
    @Nonnull
    public FileProvider getFileProvider() {
        return provider;
    }
}
