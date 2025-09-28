package com.fathzer.sync4j.sync;

import com.fathzer.sync4j.Folder;

public record Event(Action action, Status status) {
    public enum Status {
        PLANNED,
        STARTED,
        ENDED
    }

    public sealed interface Action permits ListAction {
    }

    public record ListAction(Folder folder, boolean recursive) implements Action {
    }
}
