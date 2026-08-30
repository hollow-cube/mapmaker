package net.hollowcube.anticheat.capture;

import net.hollowcube.anticheat.state.EntityTableView;
import net.hollowcube.anticheat.state.StateCache;
import net.hollowcube.anticheat.state.StateCacheView;
import net.hollowcube.anticheat.state.TrackedEntity;
import net.hollowcube.anticheat.world.ChunkMap;
import net.hollowcube.anticheat.world.WorldView;

/// Everything a trace needs to start from: the client's world, its cached state and where every
/// entity was, all as of one instant.
///
/// Taking one is constant time — both views hand out the model's own storage and leave it to copy
/// on its next write — so a snapshot every thirty seconds costs nothing but the copies the frames
/// after it force. Once taken it is immutable, which is what lets it cross to the writer thread.
public record Snapshot(long tNs, int pingId, WorldView world, StateCacheView state) {

    public static Snapshot of(long tNs, int pingId, ChunkMap world, StateCache state) {
        return new Snapshot(tNs, pingId, world.snapshot(), state.snapshot());
    }

    public EntityTableView entities() {
        return state.entities();
    }

    /// Where the connection's own player was, which is the position a trace is trimmed around.
    public TrackedEntity player() {
        return state.entities().player();
    }
}
