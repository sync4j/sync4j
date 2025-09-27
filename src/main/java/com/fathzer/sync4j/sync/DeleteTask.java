package com.fathzer.sync4j.sync;

import java.io.IOException;

import com.fathzer.sync4j.Entry;

public class DeleteTask extends Task {
    private final Entry entry;

    DeleteTask(Context context, Entry entry) {
        super(context);
        this.entry = entry;
    }

    @Override
    public void execute() throws IOException {
        // TODO: progress listener
        if (!context().params().dryRun()) {
            entry.delete();
        }
    }
}
