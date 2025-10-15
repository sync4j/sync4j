package com.fathzer.sync4j.sync;

import java.util.Objects;
import java.util.function.LongConsumer;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.File;
import com.fathzer.sync4j.FileProvider;
import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.sync.parameters.PerformanceParameters;

import jakarta.annotation.Nonnull;

/**
 * An event sent by the synchronizer.
 */
public class Event {
    /**
     * The status of an action.
     */
    public enum Status {
        /**
         * The action is planned.
         */
        PLANNED,
        /**
         * The action is started.
         */
        STARTED,
        /**
         * The action failed.
         */
        FAILED,
        /**
         * The action completed.
         */
        COMPLETED
    }

    /**
     * An action performed by the synchronizer.
     */
    public static interface Action {
    }

    private final Action action;
    private Status status;

    Event(Action action) {
        this.action = action;
        this.status = Status.PLANNED;
    }

    /**
     * Returns the action.
     * @return the action
     */
    public Action getAction() {
        return action;
    }

    /**
     * Returns the status.
     * @return the status
     */
    public Status getStatus() {
        return status;
    }

    void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Action to compare two files.
     */
    public static final class CompareFileAction implements Action {
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
    public static final class CopyFileAction implements Action {
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
    public static final class CreateFolderAction implements Action {
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
    public static final class DeleteEntryAction implements Action {
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

    /**
     * Action to list the content of a folder.
     */
    public static sealed class ListAction implements Action {
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

    /**
     * Action to preload a folder (i.e. preload the list of entries of a folder and its subfolders).
     * @see PerformanceParameters#fastList
     */
    public static final class PreloadAction extends ListAction {
        PreloadAction(@Nonnull Folder folder) {
            super(folder);
        }
    }
}
