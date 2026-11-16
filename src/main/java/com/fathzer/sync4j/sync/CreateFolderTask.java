package com.fathzer.sync4j.sync;

import java.io.IOException;

import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.sync.Event.CreateFolderAction;

class CreateFolderTask extends Task<Folder, CreateFolderAction> {

    private final boolean afterDelete;

    CreateFolderTask(Context context, Folder destination, String name) {
        this(context, destination, name, false);
    }

    /**
     * Constructor.
     * @param afterDelete true if the task follows the deletion of an entry with the same name
     */
    CreateFolderTask(Context context, Folder destination, String name, boolean afterDelete) {
        super(context, new CreateFolderAction(destination, name), context.statistics().createdFolders());
        this.afterDelete = afterDelete;
    }

    @Override
    protected boolean isPropagationDelay(IOException e) {
        return context().isPropagationDelay(e, action().folder(), afterDelete);
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
