package net.hollowcube.map.event;

import net.hollowcube.map.event.trait.MapWorldEvent;
import net.hollowcube.map.world.MapWorldNew;
import net.hollowcube.mapmaker.model.MapData;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Triggered when a player reaches a checkpoint
 */
public record MapWorldCheckpointReachedEvent(
        @NotNull MapWorldNew mapWorld,
        @NotNull Player player,
        @NotNull MapData.POI checkpoint
) implements MapWorldEvent {

    @Override
    public @NotNull MapWorldNew getMapWorld() {
        return mapWorld;
    }

    public @NotNull Player getPlayer() {
        return player;
    }

    public @NotNull MapData.POI getCheckpoint() {
        return checkpoint;
    }

}
