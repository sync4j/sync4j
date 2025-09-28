package com.fathzer.sync4j.sync;

import java.io.IOException;

import com.fathzer.sync4j.Entry;

public class DeleteTask extends Task<Void> {
    private final Entry entry;

    DeleteTask(Context context, Entry entry) {
        super(context, entry.isFile() ? context.statistics().deletedFiles() : context.statistics().deletedFolders());
        this.entry = entry;
    }

    @Override
    public Void execute() throws IOException {
        System.out.println("Deleting " + entry.getParentPath()+ "/" + entry.getName());
        // TODO: progress listener
        if (!context().params().dryRun()) {
            entry.delete();
            counter.done().incrementAndGet();
        }
        return null;
    }
}
