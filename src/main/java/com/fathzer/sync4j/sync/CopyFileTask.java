package com.fathzer.sync4j.sync;

import java.io.IOException;

import com.fathzer.sync4j.File;
import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.sync.Event.CopyFileAction;

public class CopyFileTask extends Task<Void, CopyFileAction> {

    CopyFileTask(Context context, File source, Folder destination) throws IOException {
        super(context, new CopyFileAction(source, destination), context.statistics().copiedFiles());
        context().statistics().copiedBytes().total().addAndGet(source.getSize());
    }

    public Void execute() throws IOException {
//        System.out.println("Copying " + source.getParentPath()+ "/" + source.getName() + " to " + destination.getParentPath()+ "/" + destination.getName());
        // FIXME: progress listener
        action.destination().copy(action.source().getName(), action.source(), null);
        context().statistics().copiedBytes().done().addAndGet(action.source().getSize());
        return null;
    }
}
