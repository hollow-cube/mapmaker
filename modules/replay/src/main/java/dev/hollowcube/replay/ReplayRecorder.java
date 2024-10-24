package dev.hollowcube.replay;

import dev.hollowcube.replay.event.InstanceEndTickEvent;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.event.EventListener;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static net.minestom.server.network.NetworkBuffer.VAR_INT;
import static net.minestom.server.network.NetworkBuffer.VECTOR3D;

@SuppressWarnings("UnstableApiUsage")
public class ReplayRecorder implements Closeable {
    private final ReplayFactory factory;

    private final List<List<RecordedChange>> changes;
    private List<RecordedChange> currentChanges;

    private boolean active = true;

    private final Instance instance;
    private final Point origin;

    private int userVersion = 0;

    public ReplayRecorder(@NotNull ReplayFactory factory, @NotNull Instance instance, @NotNull Point origin) {
        this.factory = factory;
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

    public void setUserVersion(int userVersion) {
        this.userVersion = userVersion;
    }

    public int getUserVersion() {
        return userVersion;
    }

    public void record(@NotNull RecordedChange change) {
        currentChanges.add(change);
    }

    public @NotNull InputStream toStream() {
        return new ByteArrayInputStream(NetworkBuffer.makeArray(buffer -> {
            var metadata = new ReplayMetadata(origin);
            buffer.write(VAR_INT, factory.version());
            buffer.write(VAR_INT, userVersion);
            buffer.write(VAR_INT, MinecraftServer.TICK_PER_SECOND);
            buffer.write(VAR_INT, changes.size());
            buffer.write(VECTOR3D, origin);

            // TODO: 1.21.2 broke this, need to use proper network buffer types here
//            buffer.writeCollection(changes, ($, change) -> buffer.writeCollection(change, ($$, entry) -> {
//                buffer.write(VAR_INT, entry.id());
//                entry.write(metadata, buffer);
//            }));
        }));
    }

    @Deprecated
    public @NotNull Replay complete() {
        endTick();
        active = false;
        return new Replay(changes);
    }

    private void endTick() {
        changes.add(currentChanges);
        currentChanges = new ArrayList<>();
    }

    @Override
    public void close() {
        active = false;
    }
}
