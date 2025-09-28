package com.fathzer.sync4j.sync;

import java.io.IOException;
import java.io.UncheckedIOException;

public class PreloadTask implements Runnable {
    private final Folders folders;
    private final boolean source;
    
    PreloadTask(Folders folders, boolean source) {
        this.folders = folders;
        this.source = source;
    }

    public void run() {
        try {
            if (source) {
                System.out.println("Preloading source");
                folders.source = folders.source.preload();
            } else {
                System.out.println("Preloading destination");
                folders.destination = folders.destination.preload();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
