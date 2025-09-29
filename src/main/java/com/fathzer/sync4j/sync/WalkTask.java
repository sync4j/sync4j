package com.fathzer.sync4j.sync;

import java.io.IOException;
import java.io.UncheckedIOException;
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

class WalkTask extends RecursiveAction {
	private static final long serialVersionUID = 1L;
	
    private final Context context;
    private final Folder sourceFolder;
    private final Folder destinationFolder;
    private List<Entry> destinationList;

    WalkTask(Context context, Folder sourceFolder, Folder destinationFolder, List<Entry> destinationList) {
        this.context = context;
        this.sourceFolder = sourceFolder;
        this.destinationFolder = destinationFolder;
        this.destinationList = destinationList;
    }

    @Override
    protected void compute() {
        try {
            process();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void process() throws IOException {
        List<Entry> sourceList = sourceFolder.list();
        System.out.println("Find " + sourceList.size() + " files to process " + sourceFolder);
        if (destinationList == null) destinationList = destinationFolder.list();
        Map<String, Entry> destinationMap = destinationList.stream().collect(Collectors.toMap(Entry::getName, Function.identity()));
        Set<String> destinationNames = new HashSet<>(destinationMap.keySet());
        for (Entry srcEntry : sourceList) {
            if (!context.params().filter().test(srcEntry)) {
                context.skip(srcEntry);
                continue;
            }
            if (context.isCancelled()) return;
            if (destinationNames.remove(srcEntry.getName())) {
                // Destination entry exists
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
                        // Spawn a new task to process the folder with empty destination folder (to not call list for nothing)
                        new WalkTask(context, src, destinationEntry.asFolder(), List.of()).fork();
                    }
                }
            } else {
                if (srcEntry.isFile()) {
                    context.asyncCopy(srcEntry.asFile(), destinationFolder);
                } else {
                    Folder destinationEntry = context.createFolder(destinationFolder, srcEntry.getName());
                    // Spawn a new task to process the folder with empty destination folder (to not call list for nothing)
                    new WalkTask(context, srcEntry.asFolder(), destinationEntry, List.of()).fork();
                }
            }
        }
        if (context.isCancelled()) return;
        // Remaining destination entries have to be deleted
        for (String name : destinationNames) {
           Entry entry = destinationMap.get(name);
           context.asyncDelete(entry);
        }
        System.out.println("End walking through " + sourceFolder);
    }
}
