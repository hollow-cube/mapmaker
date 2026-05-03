package net.hollowcube.mapmaker.isolate;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.common.util.ProtocolVersions;
import net.hollowcube.mapmaker.CoreFeatureFlags;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.api.ApiClient;
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

import java.util.concurrent.CompletableFuture;

import static net.hollowcube.common.util.PlayerUtil.onConfigOrDisconnect;

public class MapIsolateBridge implements ServerBridge {
    private static final Logger logger = LoggerFactory.getLogger(MapIsolateBridge.class);

    private final ApiClient api;
    private final SessionService sessionService;

    public MapIsolateBridge(ApiClient api, SessionService sessionService) {
        this.api = api;
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
            var map = api.maps.get(joinConfig.mapId());

            var playerProtocolVersion = ProtocolVersions.getProtocolVersion(player);
            if (playerProtocolVersion < map.protocolVersion()) {
                player.sendMessage(Component.translatable("map_join.wrongversion",
                    Component.text(map.name()), Component.text(ProtocolVersions.getProtocolName(map.protocolVersion()))));
                return;
            }

            var targetState = switch (joinConfig.joinMapState()) {
                case EDITING -> MapPresence.STATE_EDITING;
                case PLAYING -> MapPresence.STATE_PLAYING;
                case VERIFYING -> MapPresence.STATE_VERIFYING;
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
            var future = new CompletableFuture<Void>();
            onConfigOrDisconnect(player, () -> future.complete(null));

            var playerData = PlayerData.fromPlayer(player);
            var res = sessionService.joinHubV2(new JoinHubRequest(playerData.id()));
            logger.info("join hub result: {}", res);
            ProxySupport.transfer(player, res.serverClusterIp());

            FutureUtil.getUnchecked(future); // Wait until not in instance
        } catch (Exception e) {
            ExceptionReporter.reportException(e, player);
            player.sendMessage(Component.text("An error occurred while trying to return to the hub. Please try again later."));
        }

    }

}
