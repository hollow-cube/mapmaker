package net.hollowcube.mapmaker.map.event;

import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.BlockEvent;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

public class BlockItemPlaceEvent implements PlayerInstanceEvent, BlockEvent, CancellableEvent {
    private final Player player;
    private final Point blockPosition;
    private final Block block;
    private boolean cancelled;

    public BlockItemPlaceEvent(@NotNull Player player, @NotNull Point blockPosition, @NotNull Block block) {
        this.player = player;
        this.blockPosition = blockPosition;
        this.block = block;
    }

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }

    @Override
    public @NotNull Block getBlock() {
        return block;
    }

    public Point getBlockPosition() {
        return blockPosition;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
