package net.hollowcube.mapmaker.map.block.handler;

import net.kyori.adventure.key.Key;
import net.minestom.server.instance.block.BlockHandler;

/**
 * A no-op block handler does nothing, and is used for blocks where we do not actually care about any of the NBT data.
 *
 * <p>In those cases, we still must have a block handler registered so that the block does not turn invisible.</p>
 */
public class NoopBlockHandler implements BlockHandler {
    private final Key id;

    NoopBlockHandler(String id) {
        this.id = Key.key(id);
    }

    @Override
    public Key getKey() {
        return id;
    }
}
