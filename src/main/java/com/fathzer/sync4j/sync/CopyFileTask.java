package com.fathzer.sync4j.sync;

import java.io.IOException;

import com.fathzer.sync4j.File;
import com.fathzer.sync4j.Folder;

public class CopyFileTask extends Task {
    private final File source;
    private final Folder destination;

    CopyFileTask(Context context, File source, Folder destination) {
        super(context);
        this.source = source;
        this.destination = destination;
    }

    public void execute() throws IOException {
        System.out.println("Copying " + source.getParentPath()+ "/" + source.getName() + " to " + destination.getParentPath()+ "/" + destination.getName());
        // TODO: progress listener
        if (!context().params().dryRun()) {
            destination.copy(source.getName(), source, null);
        }
    }
}
