package net.hollowcube.map.event.vnext;

import net.hollowcube.map.feature.play.effect.CheckpointEffectData;
import net.hollowcube.map2.MapWorld;
import net.hollowcube.map2.event.trait.MapWorldEvent;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public record MapPlayerCheckpointChangeEvent(
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
