package net.hollowcube.mapmaker.map.dep;

import net.hollowcube.mapmaker.bridge.MapToHubBridge;
import net.hollowcube.mapmaker.player.JoinHubRequest;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.SessionService;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class MapBridge implements MapToHubBridge {
    private static final Logger logger = LoggerFactory.getLogger(MapBridge.class);

    private final SessionService sessionService;

    public MapBridge(@NotNull SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public void sendPlayerToHub(@NotNull Player player) {
        var playerData = PlayerDataV2.fromPlayer(player);
        var res = sessionService.joinHubV2(new JoinHubRequest(playerData.id()));
        logger.info("join map result: {}", res);
        player.sendPluginMessage("mapmaker:transfer", res.serverClusterIp().getBytes(StandardCharsets.UTF_8));
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
