package dev.hollowcube.replay;

import dev.hollowcube.replay.event.InstanceEndTickEvent;
import net.minestom.server.coordinate.Point;
import net.minestom.server.event.EventListener;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ReplayRecorder {
    private final List<List<RecordedChange>> changes;
    private List<RecordedChange> currentChanges;

    private boolean active = true;

    private final Instance instance;
    private final Point origin;

    public ReplayRecorder(@NotNull Instance instance, @NotNull Point origin) {
        this.changes = new ArrayList<>();
        this.currentChanges = new ArrayList<>();

        this.instance = instance;
        this.origin = origin;

        instance.eventNode().addListener(EventListener.builder(InstanceEndTickEvent.class)
                .filter(event -> event.instance() == instance)
                .handler(event -> endTick())
                .expireWhen(event -> !active)
                .build());
    }

    public @NotNull Instance instance() {
        return instance;
    }

    public @NotNull Point origin() {
        return origin;
    }

    public void record(@NotNull RecordedChange change) {
        currentChanges.add(change);
    }

    public @NotNull Replay complete() {
        endTick();
        active = false;
        return new Replay(changes);
    }

    private void endTick() {
        changes.add(currentChanges);
        currentChanges = new ArrayList<>();
    }
}
