package net.hollowcube.mapmaker.isolate;

import net.hollowcube.common.util.ProtocolVersions;
import net.hollowcube.mapmaker.CoreFeatureFlags;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.misc.ProxySupport;
import net.hollowcube.mapmaker.player.JoinHubRequest;
import net.hollowcube.mapmaker.player.JoinMapRequest;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.SessionService;
import net.hollowcube.mapmaker.session.MapPresence;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapIsolateBridge implements ServerBridge {
    private static final Logger logger = LoggerFactory.getLogger(MapIsolateBridge.class);

    private final MapService mapService;
    private final SessionService sessionService;

    public MapIsolateBridge(MapService mapService, SessionService sessionService) {
        this.mapService = mapService;
        this.sessionService = sessionService;
    }

    @Override
    public void joinMap(Player player, JoinConfig joinConfig) {
        if (CoreFeatureFlags.MAP_DISABLE_ALL.test()) {
            player.sendMessage(Component.translatable("ff.maps_disabled"));
            return;
        }

        try {
            var playerId = player.getUuid().toString();
            var map = mapService.getMap(playerId, joinConfig.mapId());

            var playerProtocolVersion = ProtocolVersions.getProtocolVersion(player);
            if (playerProtocolVersion < map.protocolVersion()) {
                player.sendMessage(Component.translatable("map_join.wrongversion",
                        Component.text(map.name()), Component.text(ProtocolVersions.getProtocolName(map.protocolVersion()))));
                return;
            }

            var targetState = switch (joinConfig.joinMapState()) {
                case EDITING -> MapPresence.STATE_EDITING;
                case PLAYING -> MapPresence.STATE_PLAYING;
                case SPECTATING -> MapPresence.STATE_SPECTATING;
            };
            var response = sessionService.joinMapV2(new JoinMapRequest(playerId, joinConfig.mapId(), targetState, joinConfig.source(), joinConfig.isolateOverride()));
            logger.info("join map result: {}", response);

            ProxySupport.transfer(player, response.serverClusterIp());
        } catch (Exception e) {
            ExceptionReporter.reportException(e, player);
            player.sendMessage(Component.text("An error occurred while trying to join the map. Please try again later."));
        }
    }

    @Override
    public void joinHub(Player player) {
        try {
            var playerData = PlayerData.fromPlayer(player);
            var res = sessionService.joinHubV2(new JoinHubRequest(playerData.id()));
            logger.info("join hub result: {}", res);
            ProxySupport.transfer(player, res.serverClusterIp());
        } catch (Exception e) {
            ExceptionReporter.reportException(e, player);
            player.sendMessage(Component.text("An error occurred while trying to return to the hub. Please try again later."));
        }
    }

}
