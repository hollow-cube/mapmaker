package net.hollowcube.anticheat.state;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.hollowcube.anticheat.protocol.Metadata776;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/// One entity's position as the client would have it, plus what the state cache needs to decide
/// whether to keep its packets.
///
/// Positions are tracked even for [#dropped] entities — three doubles each — because a dropped
/// display entity that later turns up as a vehicle has to be promoted with a real position.
///
/// [#metadata] is the merged `set_entity_data` state, index to whole raw entry, because metadata
/// is where real physics lives for some entities — an interaction entity's width and height, the
/// pose, the shared flags. Best effort: entries [Metadata776] cannot walk are absent (their raw
/// frames are still in the state cache).
public record TrackedEntity(
    int entityId,
    @Nullable UUID uuid,
    int typeId,
    double x, double y, double z,
    float yRot, float xRot,
    boolean onGround,
    boolean dropped,
    Int2ObjectMap<byte[]> metadata
) {

    public TrackedEntity(int entityId, @Nullable UUID uuid, int typeId, double x, double y, double z,
                         float yRot, float xRot, boolean onGround, boolean dropped) {
        this(entityId, uuid, typeId, x, y, z, yRot, xRot, onGround, dropped, Int2ObjectMaps.emptyMap());
    }

    /// The byte rotations the entity packets carry: `value * 360 / 256`.
    public static float rotation(byte packed) {
        return packed * 360.0f / 256.0f;
    }

    public static byte packRotation(float degrees) {
        return (byte) Math.round(degrees * 256.0f / 360.0f);
    }

    TrackedEntity withPosition(double x, double y, double z, boolean onGround) {
        return new TrackedEntity(entityId, uuid, typeId, x, y, z, yRot, xRot, onGround, dropped, metadata);
    }

    TrackedEntity withRotation(float yRot, float xRot) {
        return new TrackedEntity(entityId, uuid, typeId, x, y, z, yRot, xRot, onGround, dropped, metadata);
    }

    TrackedEntity withMetadata(List<Metadata776.Entry> entries) {
        if (entries.isEmpty()) return this;
        var merged = new Int2ObjectOpenHashMap<>(metadata);
        for (var entry : entries) merged.put(entry.index(), entry.bytes());
        return new TrackedEntity(entityId, uuid, typeId, x, y, z, yRot, xRot, onGround, dropped, merged);
    }

    TrackedEntity promoted() {
        return new TrackedEntity(entityId, uuid, typeId, x, y, z, yRot, xRot, onGround, false, metadata);
    }
}
