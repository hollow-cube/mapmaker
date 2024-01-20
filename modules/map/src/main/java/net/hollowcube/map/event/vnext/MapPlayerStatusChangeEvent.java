package net.hollowcube.map.event.vnext;

import net.hollowcube.map.event.trait.MapWorldEvent;
import net.hollowcube.map.feature.play.effect.StatusEffectData;
import net.hollowcube.map.world.MapWorld;
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
