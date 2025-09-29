package com.fathzer.sync4j.sync;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.File;
import com.fathzer.sync4j.Folder;

public record Event(Action action, Status status) {
    public enum Status {
        PLANNED,
        STARTED,
        ENDED
    }

    public sealed interface Action permits ListAction, CompareFileAction, CopyFileAction, CreateFolderAction, DeleteEntryAction {
    }

    public record CompareFileAction(File source, File destination) implements Action {
    }

    public record CopyFileAction(File source, Folder destination) implements Action {
    }

    public record CreateFolderAction(Folder folder, String name) implements Action {
    }

    public record DeleteEntryAction(Entry entry) implements Action {
    }

    public record ListAction(Folder folder, boolean recursive) implements Action {
    }
}
