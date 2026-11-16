package com.fathzer.sync4j.sync;

import java.io.IOException;

import com.fathzer.sync4j.sync.Event.CopyFileAction;

import jakarta.annotation.Nonnull;

class CopyFileTask extends Task<Void, CopyFileAction> {
    private long bytesCopied;
    private final boolean afterDelete;

    CopyFileTask(@Nonnull Context context, @Nonnull CopyFileAction action) throws IOException {
        this(context, action, false);
    }

    /**
     * Constructor.
     * @param afterDelete true if the task follows the deletion of an entry with the same name
     */
    CopyFileTask(@Nonnull Context context, @Nonnull CopyFileAction action, boolean afterDelete) throws IOException {
        super(context, action, context.statistics().copiedFiles());
        context.statistics().copiedBytes().total().addAndGet(action.source().getSize());
        this.afterDelete = afterDelete;
    }

    @Override
    protected boolean isPropagationDelay(IOException e) {
        return context().isPropagationDelay(e, action().destination(), afterDelete);
    }

    @Override
    protected void rollback() {
        // Forget the bytes of the failed attempt (a new attempt restarts from the beginning)
        context().statistics().copiedBytes().done().addAndGet(-bytesCopied);
        bytesCopied = 0;
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
