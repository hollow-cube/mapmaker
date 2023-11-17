package net.hollowcube.map.block.handler;

import net.hollowcube.map.MapHooks;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.BlockHandler;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public interface PressurePlateBlockMixin extends BlockHandler {
    BoundingBox BOUNDING_BOX = new BoundingBox(14.0 / 16.0, 1.0 / 16.0, 14.0 / 16.0);

    @Override
    default boolean isTickable() {
        return true;
    }

    @Override
    default void tick(@NotNull Tick tick) {
        var instance = tick.getInstance();
        var pos = tick.getBlockPosition();
        var centerPos = new Vec(pos.blockX() + 0.5, pos.blockY(), pos.blockZ() + 0.5);

        Set<Player> newPlayers = new HashSet<>(), currentPlayers = getPlayersOnPlate();

        // Check for collision with all players in instance
        var entities = instance.getNearbyEntities(pos, 2);
        if (entities.isEmpty()) {
            currentPlayers.clear();
            return;
        }
        for (var entity : entities) {
            Player player;
            if (entity instanceof Player p) {
                if (!MapHooks.isPlayerPlaying(p)) continue;
                player = p;
            } else if (entity.hasTag(MapHooks.ASSOCIATED_PLAYER)) {
                player = entity.getTag(MapHooks.ASSOCIATED_PLAYER);
            } else continue;

            if (!BOUNDING_BOX.intersectBox(centerPos.sub(entity.getPosition()), entity.getBoundingBox()))
                continue;

            // Player is on the plate
            newPlayers.add(player);
        }

        // Diff the new players with the old players
        for (var player : newPlayers) {
            if (!currentPlayers.contains(player)) {
                onPlatePressed(tick, player);
            }
        }
        currentPlayers.clear();
        currentPlayers.addAll(newPlayers);
    }

    void onPlatePressed(@NotNull Tick tick, @NotNull Player player);

    /**
     * MUST return a mutable set used for internal statekeeping.
     */
    @ApiStatus.Internal
    @NotNull Set<Player> getPlayersOnPlate();
}
