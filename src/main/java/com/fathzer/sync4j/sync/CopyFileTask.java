package com.fathzer.sync4j.sync;

import java.io.IOException;

import com.fathzer.sync4j.File;
import com.fathzer.sync4j.Folder;

public class CopyFileTask extends Task<Void> {
    private final File source;
    private final Folder destination;

    CopyFileTask(Context context, File source, Folder destination) throws IOException {
        super(context, context.statistics().copiedFiles());
        context().statistics().copiedBytes().total().addAndGet(source.getSize());
        this.source = source;
        this.destination = destination;
    }

    public Void execute() throws IOException {
//        System.out.println("Copying " + source.getParentPath()+ "/" + source.getName() + " to " + destination.getParentPath()+ "/" + destination.getName());
        if (!context().params().dryRun()) {
//            System.out.println("Dry run: would copy " + source.getParentPath()+ "/" + source.getName() + " to " + destination.getParentPath()+ "/" + destination.getName());
        // FIXME: progress listener
            destination.copy(source.getName(), source, null);
            context().statistics().copiedBytes().done().addAndGet(source.getSize());
            counter.done().incrementAndGet();
        }
        return null;
    }
}
