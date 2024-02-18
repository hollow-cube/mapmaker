package net.hollowcube.mapmaker.map.event.vnext;

import net.hollowcube.mapmaker.map.feature.play.effect.StatusEffectData;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.trait.MapWorldEvent;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public record MapPlayerStatusChangeEvent(
        @NotNull Player player,
        @NotNull MapWorld mapWorld,
        @NotNull String statusId,
        @NotNull StatusEffectData effectData
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
