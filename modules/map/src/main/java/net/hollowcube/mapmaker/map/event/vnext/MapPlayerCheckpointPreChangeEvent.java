package net.hollowcube.mapmaker.map.event.vnext;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.trait.MapWorldEvent;
import net.hollowcube.mapmaker.map.feature.play.effect.CheckpointEffectDataV2;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public record MapPlayerCheckpointPreChangeEvent(
        @NotNull Player player,
        @NotNull MapWorld mapWorld,
        @NotNull String checkpointId,
        @NotNull CheckpointEffectDataV2 effectData
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
