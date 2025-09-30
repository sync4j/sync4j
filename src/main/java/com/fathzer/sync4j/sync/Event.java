package com.fathzer.sync4j.sync;

import java.util.concurrent.atomic.AtomicLong;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.File;
import com.fathzer.sync4j.Folder;

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

    public static final class CompareFileAction extends Action {
        private final File source;
        private final File destination;
        CompareFileAction(File source, File destination) {
            this.source = source;
            this.destination = destination;
        }
        public File source() {
            return source;
        }
        public File destination() {
            return destination;
        }
    }

    public static final class CopyFileAction extends Action {
        private final File source;
        private final Folder destination;
        CopyFileAction(File source, Folder destination) {
            this.source = source;
            this.destination = destination;
        }
        public File source() {
            return source;
        }
        public Folder destination() {
            return destination;
        }
    }

    public static final class CreateFolderAction extends Action {
        private final Folder folder;
        private final String name;
        CreateFolderAction(Folder folder, String name) {
            this.folder = folder;
            this.name = name;
        }
        public Folder folder() {
            return folder;
        }
        public String name() {
            return name;
        }
    }

    public static final class DeleteEntryAction extends Action {
        private final Entry entry;
        DeleteEntryAction(Entry entry) {
            this.entry = entry;
        }
        public Entry entry() {
            return entry;
        }
    }

    public static final class ListAction extends Action {
        private final Folder folder;
        private final boolean recursive;
        ListAction(Folder folder, boolean recursive) {
            this.folder = folder;
            this.recursive = recursive;
        }
        public Folder folder() {
            return folder;
        }
        public boolean recursive() {
            return recursive;
        }
    }
}
