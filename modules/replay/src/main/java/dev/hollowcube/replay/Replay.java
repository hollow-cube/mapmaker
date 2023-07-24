package dev.hollowcube.replay;

import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

import static net.minestom.server.network.NetworkBuffer.VAR_INT;
import static net.minestom.server.network.NetworkBuffer.VECTOR3D;

@SuppressWarnings("UnstableApiUsage")
public class Replay {
    public static final int VERSION = 1;

    public static @NotNull Replay read(@NotNull ReplayFactory factory, InputStream data) {
        try {
            return read(factory, data.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static @NotNull Replay read(@NotNull ReplayFactory factory, byte[] data) {
        var buffer = new NetworkBuffer(ByteBuffer.wrap(data));
        var version = buffer.read(VAR_INT);
        var userVersion = buffer.read(VAR_INT);
        var tickRate = buffer.read(VAR_INT);
        var length = buffer.read(VAR_INT);
        var origin = buffer.read(VECTOR3D);

        var metadata = new ReplayMetadata(origin);
        var changes = buffer.readCollection($ -> buffer.readCollection($$ -> {
            var entryId = buffer.read(VAR_INT);
            return factory.readEntry(entryId, metadata, buffer);
        }));

        return new Replay(changes);
    }

    private final List<List<RecordedChange>> changes;

    public Replay(@NotNull List<List<RecordedChange>> changes) {
        this.changes = changes;
    }

    public List<List<RecordedChange>> getChanges() {
        return changes;
    }
}
