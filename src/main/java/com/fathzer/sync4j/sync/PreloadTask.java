package com.fathzer.sync4j.sync;

import java.io.IOException;

public class PreloadTask extends Task {
    private final Folders folders;
    private final boolean source;
    
    PreloadTask(Context context, Folders folders, boolean source) {
        super(context);
        this.folders = folders;
        this.source = source;
    }

    public void execute() throws IOException {
        if (source) {
            System.out.println("Preloading source");
            folders.source = folders.source.preload();
        } else {
            System.out.println("Preloading destination");
            folders.destination = folders.destination.preload();
        }
    }
}
