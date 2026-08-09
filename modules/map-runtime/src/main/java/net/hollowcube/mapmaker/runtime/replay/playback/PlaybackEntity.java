package net.hollowcube.mapmaker.runtime.replay.playback;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;

/// A non-player entity reconstructed from a replay.
///
/// Its position comes entirely from recorded events, so it must not simulate anything of its own.
public class PlaybackEntity extends Entity {

    public PlaybackEntity(EntityType entityType) {
        super(entityType);

        setNoGravity(true);
        hasPhysics = false;
    }

    @Override
    protected void movementTick() {
        // Nothing
    }
}
