package com.fathzer.sync4j.sync;

import java.io.IOException;

import com.fathzer.sync4j.File;
import com.fathzer.sync4j.Folder;

public class CompareFileTask extends Task<Void> {
    private final File source;
    private final Folder destinationFolder;
    private final File destination;

    CompareFileTask(Context context, File source, Folder destinationFolder, File destination) {
        super(context, context.statistics().checkedFiles());
        this.source = source;
        this.destinationFolder = destinationFolder;
        this.destination = destination;
    }

    public Void execute() throws IOException {
        boolean areSame = context().params().fileComparator().areSame(source, destination);
        counter.done().incrementAndGet();
        if (!areSame) {
            context().doCopy(source, destinationFolder);
        }
        return null;
    }
}
