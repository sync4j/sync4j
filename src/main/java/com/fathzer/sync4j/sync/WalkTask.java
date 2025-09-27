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
        System.out.println("Find " + sourceList.size() + " files to process");
        if (destinationList == null) destinationList = destinationFolder.list();
        Map<String, Entry> destinationMap = destinationList.stream().collect(Collectors.toMap(Entry::getName, Function.identity()));
        Set<String> destinationNames = new HashSet<>(destinationMap.keySet());
        for (Entry srcEntry : sourceList) {
            if (!context.params().filter().test(srcEntry)) {
                skip(srcEntry);
                continue;
            }
            if (destinationNames.remove(srcEntry.getName())) {
                // Destination entry exists
                Entry destinationEntry = destinationMap.get(srcEntry.getName());
                if (srcEntry.isFile()) {
                    final File src = srcEntry.asFile();
                    if (!destinationEntry.exists()) {
                        context.doCopyTask(new CopyFileTask(context, src, destinationFolder));
                    } else if (destinationEntry.isFile()) {
                        context.doCheckTask(new CompareFileTask(context, src, destinationFolder, destinationEntry.asFile()));
                    } else {
                        // Destination entry is a folder
                        context.doCopyTask(new DeleteThenCopyTask(context, destinationEntry.asFile(), src, destinationFolder));
                    }
                } else {
                    final Folder src = srcEntry.asFolder();
                    if (destinationEntry.isFolder()) {
                        new WalkTask(context, src, destinationEntry.asFolder(), null).fork();
                    } else {
                        deleteAndCopy(destinationEntry, srcEntry);
                    }
                }
            } else {
                // Destination entry does not exist
                createAndCopyEntry(destinationFolder, srcEntry);
            }
        }
        // Remaining destination entries have to be deleted
        for (String name : destinationNames) {
           Entry entry = destinationMap.get(name);
           context.doCopyTask(new DeleteTask(context, entry));
        }
    }

    private void skip(Entry entry) throws IOException {
        System.out.println("Skipping " + entry.getParentPath()+ "/" + entry.getName());
    }
/*
    private void deleteFile(Entry entry) throws IOException {
        context.listener.accept(new Event(Event.Type.DELETE, entry));
    }
*/
    private void createAndCopyEntry(Folder destinationFolder, Entry source) throws IOException {
//        context.listener.accept(new Event(Event.Type.CREATE, source));
    }

    private void deleteAndCopy(Entry destinationEntry, Entry source) throws IOException {
        // context.listener.accept(new Event(Event.Type.DELETE, destinationEntry));
        // context.listener.accept(new Event(Event.Type.CREATE, source));
    }
/*
    private void compareFiles(File source, File destination) throws IOException {
        context.listener.accept(new Event(Event.Type.COMPARE, source));
    }
*/
}
