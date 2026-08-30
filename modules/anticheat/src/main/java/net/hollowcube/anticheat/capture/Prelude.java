package net.hollowcube.anticheat.capture;

import net.hollowcube.anticheat.log.Frame;
import net.hollowcube.anticheat.protocol.*;
import net.hollowcube.anticheat.state.TrackedEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/// Turns a [Snapshot] into the frames a reader replays to reach it.
///
/// The state cache's frames come first, in the order the connection saw them, because that is the
/// order that reproduces the client's state. They are not enough on their own: positions are
/// decoded rather than cached (a relative move replayed out of context would be wrong), so every
/// tracked entity gets a synthesized `entity_position_sync` carrying where it actually was, and the
/// own player an absolute `player_position`. Dropped display entities are left out — nothing about
/// them is kept — as is the own player's entry in the entity table, which the player frame covers.
///
/// Every frame is timestamped 0: the prelude *is* the trace's start, and [Frame#tNs()] is
/// relative to it.
final class Prelude {

    private static final int ENTITY_POSITION_SYNC =
        Protocol776.packetId(ProtocolState.PLAY, Direction.S2C, "entity_position_sync");
    private static final int PLAYER_POSITION =
        Protocol776.packetId(ProtocolState.PLAY, Direction.S2C, "player_position");

    static List<Frame> frames(Snapshot snapshot) {
        var cached = snapshot.state().frames();
        var frames = new ArrayList<Frame>(cached.size() + snapshot.entities().size() + 1);
        for (var frame : cached)
            frames.add(new Frame(0, frame.direction(), frame.state(), frame.packetId(), snapshot.pingId(), frame.body()));

        var player = snapshot.player();
        var entities = new ArrayList<TrackedEntity>(snapshot.entities().all());
        entities.sort(Comparator.comparingInt(TrackedEntity::entityId));
        for (var entity : entities) {
            if (entity.dropped() || entity.entityId() == player.entityId()) continue;
            frames.add(frame(snapshot, ENTITY_POSITION_SYNC,
                new S2CEntityPositionSync.V776(entity.entityId(), position(entity), entity.onGround())));
        }

        // Before the login there is no player to place, and nothing above it either.
        if (player.entityId() >= 0)
            frames.add(frame(snapshot, PLAYER_POSITION, new S2CPlayerPosition.V776(0, position(player), 0)));

        return List.copyOf(frames);
    }

    /// Deltas are zero rather than the entity's real velocity: the model does not track it, and a
    /// wrong one would be read as movement the client never saw.
    private static PositionMoveRotation position(TrackedEntity entity) {
        return new PositionMoveRotation(entity.x(), entity.y(), entity.z(), 0, 0, 0, entity.yRot(), entity.xRot());
    }

    private static Frame frame(Snapshot snapshot, int packetId, Packet packet) {
        return new Frame(0, Direction.S2C, ProtocolState.PLAY, packetId, snapshot.pingId(), packet.toByteArray());
    }

    private Prelude() {}
}
