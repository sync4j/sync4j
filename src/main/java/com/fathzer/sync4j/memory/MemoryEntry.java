package com.fathzer.sync4j.memory;

import java.io.IOException;
import java.util.Objects;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.FileProvider;
import com.fathzer.sync4j.helper.PathUtils;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Abstract base class for in-memory entries (files and folders).
 */
abstract class MemoryEntry implements Entry {
    final String path;
    final MemoryFileProvider provider;

    protected MemoryEntry(@Nonnull String path, @Nonnull MemoryFileProvider provider) {
        provider.checkPath(path);
        this.path = path;
        this.provider = Objects.requireNonNull(provider);
    }

    @Override
    @Nonnull
    public String getName() {
        return PathUtils.getName(path);
    }


    @Override
    @Nullable
    public Entry getParent() throws IOException {
        String parentPath = PathUtils.getParent(path);
        if (parentPath == null) {
            return null;
        }
        return provider.get(parentPath);
    }

    @Override
    @Nonnull
    public FileProvider getFileProvider() {
        return provider;
    }

    void deleteParentReference() throws IOException {
        provider.checkWriteOperationsAllowed();
        Entry parent = getParent();
        if (parent.exists()) {
            ((MemoryFolder)parent).removeChild(getName(), this);
        }
    }
}
