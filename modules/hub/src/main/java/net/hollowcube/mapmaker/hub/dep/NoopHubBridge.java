package net.hollowcube.mapmaker.hub.dep;

import net.hollowcube.mapmaker.bridge.HubToMapBridge;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NoopHubBridge implements HubToMapBridge {
    @Override
    public void joinMap(@NotNull Player player, @NotNull String mapId, @NotNull JoinMapState joinMapState) {
        player.sendMessage("joinMap not implemented");
    }

    @Override
    public @Nullable String getCurrentMap(@NotNull Player player) {
        return null;
    }
}
