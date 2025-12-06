package com.fathzer.sync4j.sync;

import java.io.IOException;

import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.sync.Event.CreateFolderAction;

class CreateFolderTask extends Task<Folder, CreateFolderAction> {

    CreateFolderTask(Context context, Folder destination, String name) {
        super(context, new CreateFolderAction(destination, name), context.statistics().createdFolders());
    }

    @Override
    public Folder execute() throws IOException {
        return action().folder().mkdir(action().name());
    }

    @Override
    protected Folder defaultValue() throws IOException {
        return new DryRunFolder(action().folder(), action().name());
    }

    @Override
    protected boolean onlySynchronous() {
        // Prevent the task to be executed asynchronously
        return true;
    }
}
