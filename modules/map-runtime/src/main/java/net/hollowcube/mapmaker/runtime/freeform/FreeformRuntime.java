package net.hollowcube.mapmaker.runtime.freeform;

import net.hollowcube.common.hud.PlayerHud;
import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.misc.TitleHud;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

/// Built-in behavior for a map. Just the hub for now.
public interface FreeformRuntime {
    FreeformRuntime NONE = new FreeformRuntime() {};

    default void load(FreeformMapWorld world) {}

    default void configurePlayer(FreeformMapWorld world, Player player) {
        player.setRespawnPoint(MapSettings.getSpawnPoint(world.map().settings()));
    }

    default void configurePlaying(FreeformMapWorld world, Player player) {}

    default void resetPlaying(FreeformMapWorld world, Player player) {}

    default PlayerHud.@Nullable Module titleHud(FreeformMapWorld world) {
        return TitleHud.playing(world.server().api().players, world.map());
    }
}
