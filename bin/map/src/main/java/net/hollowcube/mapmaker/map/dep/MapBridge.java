package net.hollowcube.mapmaker.map.dep;

import net.hollowcube.map.world.InternalMapWorld;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.bridge.MapToHubBridge;
import net.hollowcube.mapmaker.map.runtime.MapRuntime;
import net.hollowcube.mapmaker.player.JoinHubRequest;
import net.hollowcube.mapmaker.player.JoinMapRequest;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.SessionService;
import net.hollowcube.mapmaker.session.MapPresence;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class MapBridge implements MapToHubBridge {
    private static final Logger logger = LoggerFactory.getLogger(MapBridge.class);

    private final SessionService sessionService;
    private final MapRuntime mapRuntime;

    public MapBridge(@NotNull SessionService sessionService, @NotNull MapRuntime mapRuntime) {
        this.sessionService = sessionService;
        this.mapRuntime = mapRuntime;
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
        try {
            var playerId = player.getUuid().toString();
            logger.debug("trying to join map {} with state {} for {}", mapId, joinMapState, playerId);

            var response = sessionService.joinMapV2(new JoinMapRequest(playerId, mapId, switch (joinMapState) {
                case EDITING -> MapPresence.STATE_EDITING;
                case PLAYING -> MapPresence.STATE_PLAYING;
                case SPECTATING -> MapPresence.STATE_SPECTATING;
            }));
            logger.info("join map result: {}", response);

            if (mapRuntime.hostname().equals(response.server())) {
                logger.info("moving between maps on this server");
                this.moveBetweenMapsOnThisServer(player);
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
    public @Nullable String getCurrentMap(@NotNull Player player) {
        return null;
    }

    private void moveBetweenMapsOnThisServer(@NotNull Player player) {
        try {
            var currentWorld = MapWorld.forPlayerOptional(player);
            if (currentWorld instanceof InternalMapWorld imw) {
                imw.removePlayer(player);
            }

            player.startConfigurationPhase();
        } catch (Exception exception) {
            logger.error("Failed to move player to map", exception);
            player.sendMessage("Failed to move to map");
            this.sendPlayerToHub(player);
        }
    }

    private void moveBetweenServers(@NotNull Player player, @NotNull String targetServerIp) {
        player.sendPluginMessage("mapmaker:transfer", targetServerIp.getBytes(StandardCharsets.UTF_8));
    }
}
