package net.hollowcube.anticheat.state;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.hollowcube.anticheat.protocol.*;
import org.jetbrains.annotations.Nullable;

/// Where every entity the client knows about is, decoded rather than merely kept, so the trim
/// policy can ask which entities came near the player.
///
/// Entries are immutable and the map is copy-on-write against [#snapshot()], which makes a snapshot
/// constant time and costs one map copy per snapshot that is followed by a write.
public final class EntityTable {

    /// `VecDeltaCodec`: the `move_entity_*` family sends 1/4096 block units.
    public static final double DELTA_UNIT = 1.0 / 4096.0;

    /// Primitive-keyed because the `move_entity_*` family hits it for every tracked entity on
    /// every tick, where a boxed key is an allocation per lookup.
    private Int2ObjectMap<TrackedEntity> entities = new Int2ObjectOpenHashMap<>();
    private boolean shared;
    private TrackedEntity player = new TrackedEntity(-1, null, EntityTypes776.PLAYER, 0, 0, 0, 0, 0, false, false);

    public int size() {
        return entities.size();
    }

    public TrackedEntity player() {
        return player;
    }

    public @Nullable TrackedEntity get(int entityId) {
        return entities.get(entityId);
    }

    /// Whether nothing about this entity is recorded, because it is a display entity that has not
    /// been promoted. Unknown ids are never dropped.
    public boolean isDropped(int entityId) {
        var entity = entities.get(entityId);
        return entity != null && entity.dropped();
    }

    public EntityTableView snapshot() {
        shared = true;
        return new EntityTableView(Int2ObjectMaps.unmodifiable(entities), player);
    }

    void apply(S2CAddEntity packet) {
        entities().put(packet.entityId(), new TrackedEntity(
            packet.entityId(), packet.uuid(), packet.entityTypeId(),
            packet.x(), packet.y(), packet.z(),
            TrackedEntity.rotation(packet.yRot()), TrackedEntity.rotation(packet.xRot()),
            false, EntityTypes776.isDisplay(packet.entityTypeId())));
    }

    void apply(MoveEntity packet) {
        var entity = entities.get(packet.entityId());
        if (entity == null) return;
        var moved = entity;
        if (packet.hasPosition())
            moved = moved.withPosition(
                entity.x() + packet.deltaX() * DELTA_UNIT,
                entity.y() + packet.deltaY() * DELTA_UNIT,
                entity.z() + packet.deltaZ() * DELTA_UNIT,
                packet.onGround());
        else moved = moved.withPosition(entity.x(), entity.y(), entity.z(), packet.onGround());
        if (packet.hasRotation())
            moved = moved.withRotation(TrackedEntity.rotation(packet.yRot()), TrackedEntity.rotation(packet.xRot()));
        entities().put(entity.entityId(), moved);
    }

    void apply(S2CTeleportEntity packet) {
        var entity = entities.get(packet.entityId());
        if (entity == null) return;
        entities().put(entity.entityId(), absolute(entity, packet.change(), packet.relatives(), packet.onGround()));
    }

    /// `entity_position_sync` carries no relative flags: the values are always absolute.
    void apply(S2CEntityPositionSync packet) {
        var entity = entities.get(packet.entityId());
        if (entity == null) return;
        var values = packet.values();
        entities().put(entity.entityId(), entity
            .withPosition(values.x(), values.y(), values.z(), packet.onGround())
            .withRotation(values.yRot(), values.xRot()));
    }

    /// Metadata merges last-wins per index, on the player too: their pose and flags arrive the
    /// same way. An id nothing tracks is left alone rather than invented.
    void apply(S2CSetEntityData packet) {
        var entries = Metadata776.entries(packet.rest());
        if (entries.isEmpty()) return;
        if (packet.entityId() == player.entityId()) {
            player = player.withMetadata(entries);
            return;
        }
        var entity = entities.get(packet.entityId());
        if (entity == null) return;
        entities().put(entity.entityId(), entity.withMetadata(entries));
    }

    void apply(S2CRemoveEntities packet) {
        for (int entityId : packet.entityIds())
            if (entities.containsKey(entityId)) entities().remove(entityId);
    }

    void apply(S2CLogin packet) {
        clear();
        player = new TrackedEntity(packet.playerId(), null, EntityTypes776.PLAYER, 0, 0, 0, 0, 0, false, false);
    }

    /// The server's own-player teleport, applied with the same [Relative] rules the client uses in
    /// `setValuesFromPositionPacket`.
    void apply(S2CPlayerPosition packet) {
        player = absolute(player, packet.change(), packet.relatives(), player.onGround());
    }

    /// `player_rotation`, whose two axes are independently absolute or relative.
    void apply(S2CPlayerRotation packet) {
        player = player.withRotation(
            packet.relativeYRot() ? player.yRot() + packet.yRot() : packet.yRot(),
            packet.relativeXRot() ? player.xRot() + packet.xRot() : packet.xRot());
    }

    /// The client's own movement, which is always absolute.
    void apply(MovePlayer packet) {
        var moved = player;
        if (packet.hasPosition()) moved = moved.withPosition(packet.x(), packet.y(), packet.z(), packet.onGround());
        else moved = moved.withPosition(moved.x(), moved.y(), moved.z(), packet.onGround());
        if (packet.hasRotation()) moved = moved.withRotation(packet.yRot(), packet.xRot());
        player = moved;
    }

    /// Marks a dropped entity kept from now on, and hands back the entry the caller needs to build
    /// the synthesized `add_entity` with.
    @Nullable TrackedEntity promote(int entityId) {
        var entity = entities.get(entityId);
        if (entity == null || !entity.dropped()) return null;
        var promoted = entity.promoted();
        entities().put(entityId, promoted);
        return promoted;
    }

    void clear() {
        if (entities.isEmpty()) return;
        entities = new Int2ObjectOpenHashMap<>();
        shared = false;
    }

    private static TrackedEntity absolute(TrackedEntity base, PositionMoveRotation change, int relatives, boolean onGround) {
        return base
            .withPosition(
                Relative.isSet(relatives, Relative.X) ? base.x() + change.x() : change.x(),
                Relative.isSet(relatives, Relative.Y) ? base.y() + change.y() : change.y(),
                Relative.isSet(relatives, Relative.Z) ? base.z() + change.z() : change.z(),
                onGround)
            .withRotation(
                Relative.isSet(relatives, Relative.Y_ROT) ? base.yRot() + change.yRot() : change.yRot(),
                Relative.isSet(relatives, Relative.X_ROT) ? base.xRot() + change.xRot() : change.xRot());
    }

    private Int2ObjectMap<TrackedEntity> entities() {
        if (shared) {
            entities = new Int2ObjectOpenHashMap<>(entities);
            shared = false;
        }
        return entities;
    }
}
