package net.hollowcube.map2.block.handler;

import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

/**
 * A no-op block handler does nothing, and is used for blocks where we do not actually care about any of the NBT data.
 *
 * <p>In those cases, we still must have a block handler registered so that the block does not turn invisible.</p>
 */
public class NoopBlockHandler implements BlockHandler {
    private final NamespaceID id;

    NoopBlockHandler(@NotNull String id) {
        this.id = NamespaceID.from(id);
    }

    @Override
    public @NotNull NamespaceID getNamespaceId() {
        return id;
    }
}
