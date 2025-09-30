package com.fathzer.sync4j.sync;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.function.Supplier;

import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.sync.Event.CreateFolderAction;

class CreateFolderTask extends Task<Folder, CreateFolderAction> {

    CreateFolderTask(Context context, Folder destination, String name) {
        super(context, new CreateFolderAction(destination, name), context.statistics().createdFolders());
    }

    @Override
    public Folder execute() throws IOException {
        System.out.println("Thread " + Thread.currentThread().getName() + ": Creating folder " + action.folder().getParentPath()+ "/" + action.folder().getName() + "/" + action.name());
        return action.folder().mkdir(action.name());
    }

    @Override
    protected Folder getDefaultValue() throws IOException {
        return new DryRunFolder(Paths.get(action.folder().getParentPath(), action.folder().getName(), action.name()));
    }

    @Override
    protected Supplier<Folder> buildAsyncSupplier() {
        // Prevent the task to be executed asynchronously
        throw new UnsupportedOperationException();
    }
}
