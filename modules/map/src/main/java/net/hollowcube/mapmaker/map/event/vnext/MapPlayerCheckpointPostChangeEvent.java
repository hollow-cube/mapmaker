package net.hollowcube.mapmaker.map.event.vnext;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.trait.MapWorldEvent;
import net.hollowcube.mapmaker.map.feature.play.effect.CheckpointEffectData;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a player steps off a checkpoint
 *
 * Note that this is also called when entering spectator mode while standing on a checkpoint. Callers should check for
 * the presence of a save state if they care.
 */
public record MapPlayerCheckpointPostChangeEvent(
        @NotNull Player player,
        @NotNull MapWorld mapWorld,
        @NotNull String checkpointId,
        @NotNull CheckpointEffectData effectData
) implements PlayerEvent, MapWorldEvent {

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }

    @Override
    public @NotNull MapWorld getMapWorld() {
        return mapWorld;
    }

}
