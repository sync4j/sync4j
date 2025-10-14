package com.fathzer.sync4j.sync;

import java.io.IOException;
import java.util.List;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.sync.Event.ListAction;

class ListTask extends Task<List<Entry>, ListAction> {
    ListTask(Context context, ListAction action) {
        super(context, action, context.statistics().listedFolders());
    }

    @Override
    protected boolean skipOnDryRun() {
        return false;
    }

    @Override
    protected List<Entry> execute() throws IOException {
        return action.folder().list();
    }
}
