package com.fathzer.sync4j.sync;

import java.io.IOException;
import java.nio.file.Paths;

import com.fathzer.sync4j.Folder;

public class CreateFolderTask extends Task<Folder> {
    private final Folder destination;
    private final String name;

    CreateFolderTask(Context context, Folder destination, String name) {
        super(context, context.statistics().createdFolders());
        this.destination = destination;
        this.name = name;
    }

    @Override
    public Folder execute() throws IOException {
        System.out.println("Creating folder " + destination.getParentPath()+ "/" + destination.getName() + "/" + name);
        final Folder folder;
        if (context().params().dryRun()) {
            folder = new DryRunFolder(Paths.get(destination.getParentPath(), destination.getName(), name));
        } else {
            folder = destination.mkdir(name);
            counter.done().incrementAndGet();
        }
        return folder;
    }
}
