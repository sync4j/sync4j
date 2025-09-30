package com.fathzer.sync4j.sync;

import java.io.IOException;

import com.fathzer.sync4j.File;
import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.sync.Event.CopyFileAction;

class CopyFileTask extends Task<Void, CopyFileAction> {
    private long bytesCopied;

    CopyFileTask(Context context, File source, Folder destination) throws IOException {
        super(context, new CopyFileAction(source, destination), context.statistics().copiedFiles());
        context().statistics().copiedBytes().total().addAndGet(action.size());
    }

    public Void execute() throws IOException {
        action.destination().copy(action.source().getName(), action.source(), this::progress);
        context().statistics().copiedBytes().done().addAndGet(action.source().getSize());
        return null;
    }

    private void progress(long bytes) {
        context().statistics().copiedBytes().done().addAndGet(bytes-bytesCopied);
        bytesCopied = bytes;
        action.progressListener().accept(bytes);
    }
}
