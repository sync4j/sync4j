package com.fathzer.sync4j.sync;

import java.io.IOException;

import com.fathzer.sync4j.File;
import com.fathzer.sync4j.Folder;

public class CompareFileTask extends Task {
    private final File source;
    private final Folder destinationFolder;
    private final File destination;

    CompareFileTask(Context context, File source, Folder destinationFolder, File destination) {
        super(context);
        this.source = source;
        this.destinationFolder = destinationFolder;
        this.destination = destination;
    }

    public void execute() throws IOException {
        // TODO: progress listener
        if (!context().params().fileComparator().areSame(source, destination)) {
            context().doCopyTask(new CopyFileTask(context(), source, destinationFolder));
        }
    }
}
