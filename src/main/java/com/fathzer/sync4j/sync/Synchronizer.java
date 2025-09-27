package com.fathzer.sync4j.sync;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.Event;
import com.fathzer.sync4j.File;
import com.fathzer.sync4j.Folder;

import com.fathzer.sync4j.sync.parameters.SyncParameters;

import jakarta.annotation.Nonnull;

public class Synchronizer {
    private final Folder source;
    private final Folder destination;
    private Context context;

    public Synchronizer(@Nonnull Folder source, @Nonnull Folder destination, @Nonnull SyncParameters parameters) throws IOException {
        this.source = Objects.requireNonNull(source);
        this.destination = Objects.requireNonNull(destination);
        this.context = new Context(parameters);
    }


    public void run() throws IOException {
        Folders folders = new Folders(source, destination);
        System.out.println("Syncing " + source + " -> " + destination);
        if (context.params().performance().fastList()) {
            doPreload(folders);
        }
        ForkJoinTask<Void> task = context.walkService().submit(new WalkTask(context, source, destination, null));
        try {
            task.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(e);
        }
    }

    private void doPreload(Folders folders) throws IOException {
        List<Future<?>> futures = new LinkedList<>();
        if (folders.source.getFileProvider().isFastListSupported()) {
            futures.add(context.walkService().submit(new PreloadTask(context, folders, true)));
        }
        if (folders.destination.getFileProvider().isFastListSupported()) futures.add(context.walkService().submit(() -> {
            futures.add(context.walkService().submit(new PreloadTask(context, folders, false)));
        }));

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new IOException(e);
            }
        }
    }
/*
    private void synchronizeFolder(Folder sourceFolder, Folder destinationFolder, List<Entry> destinationList) throws IOException {
        List<Entry> sourceList = sourceFolder.list();
        System.out.println("Find " + sourceList.size() + " files to process");
        if (destinationList == null) destinationList = destinationFolder.list();
        Map<String, Entry> destinationMap = destinationList.stream().collect(Collectors.toMap(Entry::getName, Function.identity()));
        Set<String> destinationNames = new HashSet<>(destinationMap.keySet());
        for (Entry entry : sourceList) {
            if (!context.params().filter().test(entry)) {
                skip(entry);
                continue;
            }
            if (destinationNames.remove(entry.getName())) {
                // Destination entry exists
                Entry destinationEntry = destinationMap.get(entry.getName());
                if (entry.isFile() && !destinationEntry.isFile() || (entry.isFolder() && !destinationEntry.isFolder())) {
                    deleteAndCopy(destinationEntry, entry);
                } else if (entry.isFile()) {
                    if (!context.params().fileComparator().areSame(entry.asFile(), destinationEntry.asFile())) {
                        compareFiles(entry.asFile(), destinationEntry.asFile());
                    } else {
//                        System.out.println("File " + destinationEntry.getParentPath()+ "/" + destinationEntry.getName() + " is ok");
                    }
                } else if (entry.isFolder()) {
                    System.out.println("Folder " + destinationEntry.getParentPath()+ "/" + destinationEntry.getName() + " is not processed ... will be in the near future");
                }
            } else {
                createAndCopyEntry(destinationFolder, entry);
            }
        }
        // Remaining destination names have to be deleted
        for (String name : destinationNames) {
           Entry entry = destinationMap.get(name);
           deleteFile(entry); 
        }
    }

    private void skip(Entry entry) throws IOException {
        System.out.println("Skipping " + entry.getParentPath()+ "/" + entry.getName());
    }

    private void compareFiles(File source, File destination) throws IOException {
        System.out.println("Compare " + destination.getParentPath()+ "/" + destination.getName());
    }

    private void createAndCopyEntry(Folder destinationFolder, Entry source) throws IOException {
        System.out.println("Create and copy "+(source.isFile() ? "file" : "folder") + " " + destinationFolder.getParentPath()+ "/" + source.getName());
    }

    private void deleteAndCopy(Entry destinationEntry, Entry source) throws IOException {
        System.out.println("Delete and copy " + destinationEntry.getParentPath()+ "/" + source.getName());
    }

    private void deleteFile(Entry entry) throws IOException {
        System.out.println("Delete " + entry.getParentPath()+ "/" + entry.getName());
    }

    private <T> Future<T> doAction(Event.Action action, Callable<T> call, ExecutorService executorService) throws IOException {
        return executorService.submit(() -> {
//            listener.accept(new Event(action, Status.STARTED));
            T result = call.call();
//            listener.accept(new Event(action, Status.ENDED));
            return result;
        });
    }*/
}

