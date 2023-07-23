package dev.hollowcube.replay;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Replay {
    public static final int VERSION = 1;

    private final List<List<RecordedChange>> changes;

    public Replay(@NotNull List<List<RecordedChange>> changes) {
        this.changes = changes;
    }

    public List<List<RecordedChange>> getChanges() {
        return changes;
    }
}
