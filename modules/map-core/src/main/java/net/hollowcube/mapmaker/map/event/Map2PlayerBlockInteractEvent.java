package net.hollowcube.mapmaker.map.event;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.trait.Map2PlayerEvent;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

/// This is a somewhat weird special case to allow right clicking a checkpoint with a checkpoint. But oh well.
public final class Map2PlayerBlockInteractEvent implements Map2PlayerEvent, CancellableEvent {
    private final MapWorld world;
    private final Player player;
    private final Block block;
    private final BlockVec blockPosition;
    private final PlayerHand hand;

    private boolean cancelled;

    public Map2PlayerBlockInteractEvent(
            MapWorld world, Player player,
            Block block, BlockVec blockPosition,
            PlayerHand hand
    ) {
        this.world = world;
        this.player = player;
        this.block = block;
        this.blockPosition = blockPosition;
        this.hand = hand;
    }

    @Override
    public @NotNull MapWorld world() {
        return world;
    }

    @Override
    public @NotNull Player player() {
        return player;
    }

    public @NotNull Block block() {
        return block;
    }

    public @NotNull BlockVec blockPosition() {
        return blockPosition;
    }

    public @NotNull PlayerHand hand() {
        return hand;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
