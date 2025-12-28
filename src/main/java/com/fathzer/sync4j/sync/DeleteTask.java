package com.fathzer.sync4j.sync;

import java.io.IOException;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.sync.Event.DeleteEntryAction;

class DeleteTask extends Task<Void, DeleteEntryAction> {
    private final Entry entry;

    DeleteTask(Context context, Entry entry) {
        super(context, new DeleteEntryAction(entry), entry.isFile() ? context.statistics().deletedFiles() : context.statistics().deletedFolders());
        this.entry = entry;
    }

    @Override
    public Void execute() throws IOException {
        entry.delete();
        return null;
    }
}
