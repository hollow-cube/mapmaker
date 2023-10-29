package net.hollowcube.map.event;

import net.hollowcube.map.event.trait.MapWorldEvent;
import net.hollowcube.map.world.MapWorld;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Triggered when a player reaches a checkpoint
 */
public record MapWorldCheckpointReachedEvent(
        @NotNull MapWorld mapWorld,
        @NotNull Player player,
        @NotNull String checkpointId
) implements MapWorldEvent {

    @Override
    public @NotNull MapWorld getMapWorld() {
        return mapWorld;
    }

    public @NotNull Player getPlayer() {
        return player;
    }

    public @NotNull String getCheckpoint() {
        return checkpointId;
    }

}
