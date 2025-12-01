package com.fathzer.sync4j.sync;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.LongConsumer;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.File;
import com.fathzer.sync4j.FileProvider;
import com.fathzer.sync4j.Folder;

import jakarta.annotation.Nonnull;

/**
 * A folder that does nothing, used for dry run to mock an empty folder.
 * <br>Methods {@link #getFileProvider()}, {@link #getParent()}, {@link #delete()}, {@link #copy(String, File, LongConsumer)} and
 * {@link #mkdir(String)} throw an {@link UnsupportedOperationException}.
 */
class DryRunFolder implements Folder {
    private final Path path;

    DryRunFolder(@Nonnull Path path) {
        this.path = Objects.requireNonNull(path);
    }

    @Override
    public FileProvider getFileProvider() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isFile() {
        return false;
    }

    @Override
    public boolean isFolder() {
        return true;
    }

    @Override
    public Folder getParent() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getParentPath() {
        final Path parent = path.getParent();
        return parent==null ? null : parent.toString();    }

    @Override
    public String getName() {
        return path.getFileName().toString();
    }

    @Override
    public List<Entry> list() throws IOException {
        return List.of();
    }

    @Override
    public void delete() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public File copy(String fileName, File content, LongConsumer progressListener) throws IOException {
        Objects.requireNonNull(fileName);
        Objects.requireNonNull(content);
        Objects.requireNonNull(progressListener);
        throw new UnsupportedOperationException();
    }

    @Override
    public Folder mkdir(String folderName) throws IOException {
        Objects.requireNonNull(folderName);
        throw new UnsupportedOperationException();
    }

}
