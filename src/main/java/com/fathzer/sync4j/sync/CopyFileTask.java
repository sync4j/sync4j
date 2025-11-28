package com.fathzer.sync4j.sync;

import java.io.IOException;

import com.fathzer.sync4j.sync.Event.CopyFileAction;

import jakarta.annotation.Nonnull;

class CopyFileTask extends Task<Void, CopyFileAction> {
    private long bytesCopied;

    CopyFileTask(@Nonnull Context context, @Nonnull CopyFileAction action) throws IOException {
        super(context, action, context.statistics().copiedFiles());
        context.statistics().copiedBytes().total().addAndGet(action.source().getSize());
    }

    public Void execute() throws IOException {
        action().destination().copy(action().source().getName(), action().source(), this::progress);
        return null;
    }

    private void progress(long bytes) {
        context().statistics().copiedBytes().done().addAndGet(bytes-bytesCopied);
        bytesCopied = bytes;
        action().progressListener().accept(bytes);
    }
}
