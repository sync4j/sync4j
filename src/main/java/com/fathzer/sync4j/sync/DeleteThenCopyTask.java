package com.fathzer.sync4j.sync;

import java.io.IOException;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.File;
import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.sync.Statistics.Counter;

public class DeleteThenCopyTask extends Task<Void> {
    private final DeleteTask deleteTask;
    private final CopyFileTask copyTask;

    DeleteThenCopyTask(Context context, Entry toBeDeleted, File source, Folder destination) throws IOException {
        super(context, new Counter());
        this.deleteTask = new DeleteTask(context, toBeDeleted);
        this.copyTask = new CopyFileTask(context, source, destination);
    }

    @Override
    public Void execute() throws IOException {
        deleteTask.execute();
        copyTask.execute();
        return null;
    }
}
