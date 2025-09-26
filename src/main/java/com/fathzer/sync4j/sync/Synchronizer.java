package com.fathzer.sync4j.sync;

import java.io.FileNotFoundException;
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
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.Event;
import com.fathzer.sync4j.File;
import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.Event.Action;
import com.fathzer.sync4j.Event.ListAction;
import com.fathzer.sync4j.Event.Type;
import com.fathzer.sync4j.sync.parameters.SyncParameters;

import jakarta.annotation.Nonnull;

public class Synchronizer {
    private final SyncParameters parameters;
    private final Folder source;
    private final Folder destination;
    private final ExecutorService tasksService;
    private final ExecutorService copyService;

    private final Consumer<Event> listener = System.out::println; //TODO

    public Synchronizer(@Nonnull Folder source, @Nonnull Folder destination, @Nonnull SyncParameters parameters) throws IOException {
        this.source = Objects.requireNonNull(source);
        this.destination = Objects.requireNonNull(destination);
        this.parameters = Objects.requireNonNull(parameters);
        this.tasksService = Executors.newFixedThreadPool(parameters.getPerformance().getMaxComparisonThreads(), new DaemonThreadFactory("tasks"));
        this.copyService = Executors.newFixedThreadPool(parameters.getPerformance().getMaxTransferThreads(), new DaemonThreadFactory("copy"));
    }

    private static class DaemonThreadFactory implements ThreadFactory {
        private static final AtomicInteger THREAD_NUMBER = new AtomicInteger(1);
        private final String namePrefix;
        
        DaemonThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }
        
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, namePrefix + "-" + THREAD_NUMBER.getAndIncrement());
            thread.setDaemon(true);
            thread.setName(namePrefix);
            return thread;
        };
    }

    public void run() throws IOException {
        Folders folders = new Folders(source, destination);
        System.out.println("Syncing " + source + " -> " + destination);
        if (parameters.getPerformance().isFastList()) {
            doPreload(folders);
        }
        synchronizeFolder(folders.source, folders.destination, null);
    }

    private void doPreload(Folders folders) throws IOException {
        List<Future<Folder>> futures = new LinkedList<>();
        if (folders.source.getFileProvider().isFastListSupported()) futures.add(doAction(new Event.ListAction(folders.source, true), () -> {
            folders.source = folders.source.preload();
            return folders.source;
        }, tasksService));
        if (folders.destination.getFileProvider().isFastListSupported()) futures.add(doAction(new Event.ListAction(folders.destination, true), () -> {
            folders.destination = folders.destination.preload();
            return folders.destination;
        }, tasksService));

        for (Future<Folder> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new IOException(e);
            }
        }
    }

    public static class Folders {
        private Folder source;
        private Folder destination;

        public Folders(Folder source, Folder destination) {
            this.source = source;
            this.destination = destination;
        }
    }

    private void synchronizeFolder(Folder sourceFolder, Folder destinationFolder, List<Entry> destinationList) throws IOException {
        List<Entry> sourceList = sourceFolder.list();
        System.out.println("Find " + sourceList.size() + " files to process");
        if (destinationList == null) destinationList = destinationFolder.list();
        Map<String, Entry> destinationMap = destinationList.stream().collect(Collectors.toMap(Entry::getName, Function.identity()));
        Set<String> destinationNames = new HashSet<>(destinationMap.keySet());
        for (Entry entry : sourceList) {
            if (!parameters.getFilter().test(entry)) {
                skip(entry);
                continue;
            }
            if (destinationNames.remove(entry.getName())) {
                // Destination entry exists
                Entry destinationEntry = destinationMap.get(entry.getName());
                if (entry.isFile() && !destinationEntry.isFile() || (entry.isFolder() && !destinationEntry.isFolder())) {
                    deleteAndCopy(destinationEntry, entry);
                } else if (entry.isFile()) {
                    if (!parameters.getFileComparator().areSame(entry.asFile(), destinationEntry.asFile())) {
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
            listener.accept(new Event(Event.Type.START, action));
            T result = call.call();
            listener.accept(new Event(Event.Type.END, action));
            return result;
        });
    }
}

