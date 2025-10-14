package com.fathzer.sync4j.sync;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.RecursiveAction;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.File;
import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.sync.Event.ListAction;

class WalkTask extends RecursiveAction {
	private static final long serialVersionUID = 1L;
	
    private final transient Context context;
    private final transient Folder sourceFolder;
    private final transient Folder destinationFolder;
    private transient List<Entry> destinationList;

    WalkTask(Context context, Folder sourceFolder, Folder destinationFolder, List<Entry> destinationList) {
        this.context = context;
        this.sourceFolder = sourceFolder;
        this.destinationFolder = destinationFolder;
        this.destinationList = destinationList;
    }

    @Override
    protected void compute() {
        List<Entry> sourceList = list(sourceFolder);
        if (destinationList == null && sourceList != null) {
            destinationList = list(destinationFolder);
        }
        if (sourceList == null || destinationList == null) {
            // An error occurred during folder listing, do not process the folders
            return;
        }
        Map<String, Entry> destinationMap = destinationList.stream().collect(Collectors.toMap(Entry::getName, Function.identity()));
        Set<String> destinationNames = new HashSet<>(destinationMap.keySet());
        for (Entry srcEntry : sourceList) {
            if (context.isCancelled()) return;
            if (!context.params().filter().test(srcEntry)) {
                context.skip(srcEntry);
                continue;
            }
            processEntry(destinationMap, destinationNames, srcEntry);
        }
        if (context.isCancelled()) return;
        // Remaining destination entries have to be deleted
        for (String name : destinationNames) {
        Entry entry = destinationMap.get(name);
        context.asyncDelete(entry);
        }
    }

    @SuppressWarnings("java:S1168")
    private List<Entry> list(Folder folder) {
        final ListTask task = new ListTask(context, new ListAction(folder));
        try {
            return task.executeSync();
        } catch (IOException e) {
            context.processError(e, task.action);
            return null;
        }
    }

    private void processEntry(Map<String, Entry> destinationMap, Set<String> destinationNames, Entry srcEntry) {
        if (destinationNames.remove(srcEntry.getName())) {
            // Destination entry exists
            processExistingDestinationEntry(destinationMap, srcEntry);
        } else {
            if (srcEntry.isFile()) {
                context.asyncCopy(srcEntry.asFile(), destinationFolder);
            } else {
                Folder destinationEntry = context.createFolder(destinationFolder, srcEntry.getName());
                spawnEmptyFolderTask(srcEntry.asFolder(), destinationEntry);
            }
        }
    }

    private void processExistingDestinationEntry(Map<String, Entry> destinationMap, Entry srcEntry) {
        Entry destinationEntry = destinationMap.get(srcEntry.getName());
        if (srcEntry.isFile()) {
            final File src = srcEntry.asFile();
            if (destinationEntry.isFile()) {
                context.asyncCheckAndCopy(src, destinationFolder, destinationEntry.asFile());
            } else {
                // Destination entry is a folder
                context.deleteThenAsyncCopy(destinationEntry.asFolder(), destinationFolder, src);
            }
        } else {
            final Folder src = srcEntry.asFolder();
            if (destinationEntry.isFolder()) {
                new WalkTask(context, src, destinationEntry.asFolder(), null).fork();
            } else {
                // Destination entry is a file
                // Delete the file and create the folder
                destinationEntry = context.deleteThenCreate(destinationEntry.asFile(), destinationFolder, src);
                spawnEmptyFolderTask(src, destinationEntry.asFolder());
            }
        }
    }

    /**
     * Spawns a new task to process the folder with empty destination folder (to not call list for nothing)
     * @param src the source folder
     * @param destination the destination folder, if null, the folder will be skipped
     */
    void spawnEmptyFolderTask(Folder src, Folder destination) {
        if (destination!=null) {
            new WalkTask(context, src, destination, List.of()).fork();
        }
    }
}
