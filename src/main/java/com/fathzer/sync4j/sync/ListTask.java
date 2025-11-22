package com.fathzer.sync4j.sync;

import java.io.IOException;
import java.util.List;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.sync.Event.ListAction;

class ListTask extends Task<List<Entry>, ListAction> {
    ListTask(Context context, Folder folder) {
        super(context, new ListAction(folder), context.statistics().listedFolders());
    }

    @Override
    protected List<Entry> execute() throws IOException {
        return action.folder().list();
    }

    @Override
    protected Kind kind() {
        return Kind.WALKER;
    }
}
