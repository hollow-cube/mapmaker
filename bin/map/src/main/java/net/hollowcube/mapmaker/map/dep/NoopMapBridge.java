package net.hollowcube.mapmaker.map.dep;

import net.hollowcube.mapmaker.bridge.MapToHubBridge;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NoopMapBridge implements MapToHubBridge {

    @Override
    public void sendPlayerToHub(@NotNull Player player) {
        player.sendMessage("sendPlayerToHub not implemented");
    }

    @Override
    public void joinMap(@NotNull Player player, @NotNull String mapId, @NotNull JoinMapState joinMapState) {
        player.sendMessage("joinMap not implemented");
    }

    @Override
    public @Nullable String getCurrentMap(@NotNull Player player) {
        return null;
    }
}
