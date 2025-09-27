package com.fathzer.sync4j.sync;

import java.io.IOException;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.File;
import com.fathzer.sync4j.Folder;

public class DeleteThenCopyTask extends CopyFileTask {
    private final Entry destinationEntry;

    DeleteThenCopyTask(Context context, Entry destinationEntry, File source, Folder destination) {
        super(context, source, destination);
        this.destinationEntry = destinationEntry;
    }

    @Override
    public void execute() throws IOException {
        new DeleteTask(context(), destinationEntry).execute();
        super.execute();
    }
}
