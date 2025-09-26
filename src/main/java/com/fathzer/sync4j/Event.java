package com.fathzer.sync4j;

public record Event(Type type, Action action) {
    public enum Type {
        START,
        END
    }

    public sealed interface Action permits ListAction {
    }

    public record ListAction(Folder folder, boolean recursive) implements Action {
    }
}
