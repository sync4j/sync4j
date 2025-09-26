package com.fathzer.sync4j;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;

import com.fathzer.sync4j.parameters.SyncParameters;

import jakarta.annotation.Nonnull;

public class Synchronizer {
    private final SyncParameters parameters;

    public Synchronizer(@Nonnull SyncParameters parameters) {
        this.parameters = Objects.requireNonNull(parameters);
    }

    public void run() throws IOException {
        if (!parameters.getSource().exists()) {
            throw new FileNotFoundException("Source does not exist");
        }
        if (parameters.getDestination().isFile() && !parameters.getSource().isFile()) {
            throw new IllegalArgumentException("Destination must be a directory if source is one");
        }
        System.out.println("Sync4j " + parameters.getSource()+" -> " + parameters.getDestination());
        // Be aware that the synchronization can be on 2 folders but also on 2 files and even on a file with a folder!
    }
}

