package net.hollowcube.mapmaker.map;

import net.hollowcube.mapmaker.CoreFeatureFlags;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.misc.ProxySupport;
import net.hollowcube.mapmaker.player.JoinHubRequest;
import net.hollowcube.mapmaker.player.JoinMapRequest;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.SessionService;
import net.hollowcube.mapmaker.session.MapPresence;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapIsolateBridge implements ServerBridge {
    private static final Logger logger = LoggerFactory.getLogger(MapIsolateBridge.class);

    private final SessionService sessionService;

    public MapIsolateBridge(@NotNull SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public void joinMap(@NotNull Player player, @NotNull String mapId, @NotNull JoinMapState joinMapState, @NotNull String source) {
        if (CoreFeatureFlags.MAP_DISABLE_ALL.test()) {
            player.sendMessage(Component.translatable("ff.maps_disabled"));
            return;
        }

        try {
            var playerId = player.getUuid().toString();

            var targetState = switch (joinMapState) {
                case EDITING -> MapPresence.STATE_EDITING;
                case PLAYING -> MapPresence.STATE_PLAYING;
                case SPECTATING -> MapPresence.STATE_SPECTATING;
            };
            var response = sessionService.joinMapV2(new JoinMapRequest(playerId, mapId, targetState, source));
            logger.info("join map result: {}", response);

            var currentServerId = AbstractHttpService.hostname;
            if (currentServerId.equals(response.server())) {
                logger.info("moving between maps on this server");
                this.moveBetweenMapsOnThisServer(player);
            } else {
                logger.info("moving to other server");
                ProxySupport.transfer(player, response.serverClusterIp());
            }
        } catch (Exception e) {
            ExceptionReporter.reportException(e, player);
            player.sendMessage(Component.text("An error occurred while trying to join the map. Please try again later."));
        }
    }

    @Override
    public void joinHub(@NotNull Player player) {
        try {
            var playerData = PlayerDataV2.fromPlayer(player);
            var res = sessionService.joinHubV2(new JoinHubRequest(playerData.id()));
            logger.info("join hub result: {}", res);
            ProxySupport.transfer(player, res.serverClusterIp());
        } catch (Exception e) {
            ExceptionReporter.reportException(e, player);
            player.sendMessage(Component.text("An error occurred while trying to return to the hub. Please try again later."));
        }
    }

    private void moveBetweenMapsOnThisServer(@NotNull Player player) {
        try {
            // We need to remove the player from the map before entering configuration, because by the time we get
            // remove from instance event, the player already had their position reset (ie they are at 0,0,0).
            // todo: this seems like a minestom bug that should be fixed.
            var world = MapWorld.forPlayerOptional(player);
            if (world != null) world.removePlayer(player);

            player.startConfigurationPhase();
        } catch (Exception exception) {
            logger.error("Failed to move player to map", exception);
            player.sendMessage("Failed to move to map");
            this.joinHub(player);
        }
    }
}
