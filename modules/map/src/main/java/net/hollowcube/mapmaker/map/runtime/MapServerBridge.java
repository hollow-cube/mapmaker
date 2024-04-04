package net.hollowcube.mapmaker.map.runtime;

import net.hollowcube.mapmaker.CoreFeatureFlags;
import net.hollowcube.mapmaker.map.MapServerRunner;
import net.hollowcube.mapmaker.player.JoinHubRequest;
import net.hollowcube.mapmaker.player.JoinMapRequest;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.session.MapPresence;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class MapServerBridge implements ServerBridge {
    private static final Logger logger = LoggerFactory.getLogger(MapServerBridge.class);

    private final MapServerRunner server;

    public MapServerBridge(@NotNull MapServerRunner server) {
        this.server = server;
    }

    @Override
    public void joinMap(@NotNull Player player, @NotNull String mapId, @NotNull JoinMapState joinMapState) {
        if (CoreFeatureFlags.MAP_DISABLE_ALL.test()) {
            player.sendMessage(Component.translatable("ff.maps_disabled"));
            return;
        }

        try {
            var playerId = player.getUuid().toString();
            logger.debug("trying to join map {} with state {} for {}", mapId, joinMapState, playerId);

            var targetState = switch (joinMapState) {
                case EDITING -> MapPresence.STATE_EDITING;
                case PLAYING -> MapPresence.STATE_PLAYING;
                case SPECTATING -> MapPresence.STATE_SPECTATING;
            };
            var response = server.sessionService().joinMapV2(new JoinMapRequest(playerId, mapId, targetState));
            logger.info("join map result: {}", response);

            var currentServerId = AbstractHttpService.hostname;
            if (currentServerId.equals(response.server())) {
                logger.info("moving between maps on this server");
                this.moveBetweenMapsOnThisServer(player, mapId, targetState);
            } else {
                logger.info("moving to other server");
                this.moveBetweenServers(player, response.serverClusterIp());
            }
        } catch (Exception e) {
            MinecraftServer.getExceptionManager().handleException(e);
            player.sendMessage(Component.text("An error occurred while trying to join the map. Please try again later."));
        }
    }

    @Override
    public void joinHub(@NotNull Player player) {
        try {
            var playerData = PlayerDataV2.fromPlayer(player);
            var res = server.sessionService().joinHubV2(new JoinHubRequest(playerData.id()));
            logger.info("join hub result: {}", res);
            player.sendPluginMessage("mapmaker:transfer", res.serverClusterIp().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            MinecraftServer.getExceptionManager().handleException(e);
            player.sendMessage(Component.text("An error occurred while trying to return to the hub. Please try again later."));
        }

    }

    private void moveBetweenMapsOnThisServer(@NotNull Player player, @NotNull String mapId, @NotNull String state) {
        try {
            // Add a pending join and then send them to the config state where it will be acted upon.
            var playerId = PlayerDataV2.fromPlayer(player).id();
            server.addPendingJoin(playerId, mapId, state);

            player.startConfigurationPhase();
        } catch (Exception exception) {
            logger.error("Failed to move player to map", exception);
            player.sendMessage("Failed to move to map");
            this.joinHub(player);
        }
    }

    private void moveBetweenServers(@NotNull Player player, @NotNull String targetServerIp) {
        player.sendPluginMessage("mapmaker:transfer", targetServerIp.getBytes(StandardCharsets.UTF_8));
    }
}
