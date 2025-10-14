package com.fathzer.sync4j.sync;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongConsumer;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.File;
import com.fathzer.sync4j.FileProvider;
import com.fathzer.sync4j.Folder;

import jakarta.annotation.Nonnull;

public class Event {
    public enum Status {
        PLANNED,
        STARTED,
        FAILED,
        COMPLETED
    }

    public static sealed class Action {
        private static final AtomicLong COUNTER_LONG = new AtomicLong();
        private final long id;
        private Action() {
            this.id = COUNTER_LONG.getAndIncrement();
        }

        public long id() {
            return id;
        }
    }

    private final Action action;
    private Status status;

    Event(Action action) {
        this.action = action;
        this.status = Status.PLANNED;
    }

    public Action getAction() {
        return action;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Action to compare two files.
     */
    public static final class CompareFileAction extends Action {
        private final File source;
        private final File destination;
        CompareFileAction(@Nonnull File source, @Nonnull File destination) {
            this.source = Objects.requireNonNull(source);
            this.destination = Objects.requireNonNull(destination);
        }
        /**
         * Returns the source file.
         * @return the source file
         */
        @Nonnull
        public File source() {
            return source;
        }
        /**
         * Returns the destination file.
         * @return the destination file
         */
        @Nonnull
        public File destination() {
            return destination;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName()+"{source=" + source +", destination=" + destination +'}';
        }
    }

    /**
     * Action to copy a file to a destination folder.
     */
    public static final class CopyFileAction extends Action {
        private final File source;
        private final Folder destination;
        private LongConsumer progressListener;

        CopyFileAction(@Nonnull File source, @Nonnull Folder destination) {
            this.source = Objects.requireNonNull(source);
            this.destination = Objects.requireNonNull(destination);
            this.progressListener = l -> {};
        }
        /**
         * Returns the source file.
         * @return the source file
         */
        @Nonnull
        public File source() {
            return source;
        }
        /**
         * Returns the destination folder.
         * @return the destination folder
         */
        @Nonnull
        public Folder destination() {
            return destination;
        }
        /**
         * Sets the progress listener.
         * @param progressListener the progress listener
         * <br>
         * The progress listener is called during the copy with the number of bytes already copied.
         * The frequency of the calls depends on the {@link FileProvider} implementation.
         */
        public void setProgressListener(@Nonnull LongConsumer progressListener) {
            this.progressListener = Objects.requireNonNull(progressListener);
        }
        @Nonnull
        LongConsumer progressListener() {
            return progressListener;
        }
        @Override
        public String toString() {
            return getClass().getSimpleName()+"{source=" + source +", destination=" + destination +'}';
        }
    }

    /**
     * Action to create a folder.
     */
    public static final class CreateFolderAction extends Action {
        private final Folder folder;
        private final String name;
        CreateFolderAction(@Nonnull Folder folder, @Nonnull String name) {
            this.folder = Objects.requireNonNull(folder);
            this.name = Objects.requireNonNull(name);
        }
        /**
         * Returns the folder where the folder to create is located.
         * @return A Folder instance
         */
        @Nonnull
        public Folder folder() {
            return folder;
        }
        /**
         * Returns the name of the folder to create.
         * @return the name of the folder to create
         */
        @Nonnull
        public String name() {
            return name;
        }
        @Override
        public String toString() {
            return getClass().getSimpleName()+"{parent=" + folder +", name=" + name +'}';
        }
    }

    /**
     * Action to delete an entry.
     */
    public static final class DeleteEntryAction extends Action {
        private final Entry entry;
        DeleteEntryAction(@Nonnull Entry entry) {
            this.entry = Objects.requireNonNull(entry);
        }
        /**
         * Returns the entry to delete.
         * @return An Entry instance
         */
        @Nonnull
        public Entry entry() {
            return entry;
        }
        @Override
        public String toString() {
            return getClass().getSimpleName()+"{entry=" + entry +'}';
        }
    }

    public static sealed class ListAction extends Action {
        private final Folder folder;
        ListAction(@Nonnull Folder folder) {
            this.folder = Objects.requireNonNull(folder);
        }
        /**
         * Returns the folder to list.
         * @return A Folder instance
         */
        @Nonnull
        public Folder folder() {
            return folder;
        }
        @Override
        public String toString() {
            return getClass().getSimpleName()+"{folder=" + folder +'}';
        }
    }

    public static final class PreloadAction extends ListAction {
        PreloadAction(@Nonnull Folder folder) {
            super(folder);
        }
    }
}
