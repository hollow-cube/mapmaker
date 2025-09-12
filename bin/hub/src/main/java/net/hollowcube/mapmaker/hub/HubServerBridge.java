package net.hollowcube.mapmaker.hub;

import net.hollowcube.common.util.ProtocolVersions;
import net.hollowcube.mapmaker.CoreFeatureFlags;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.misc.ProxySupport;
import net.hollowcube.mapmaker.player.JoinMapRequest;
import net.hollowcube.mapmaker.player.SessionService;
import net.hollowcube.mapmaker.session.MapPresence;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HubServerBridge implements ServerBridge {
    private static final Logger logger = LoggerFactory.getLogger(HubServerBridge.class);

    private final MapService mapService;
    private final SessionService sessionService;

    public HubServerBridge(MapService mapService, SessionService sessionService) {
        this.mapService = mapService;
        this.sessionService = sessionService;
    }

    @Override
    public void joinMap(Player player, JoinConfig joinConfig) {
        if (CoreFeatureFlags.MAP_DISABLE_ALL.test()) {
            player.sendMessage(Component.translatable("ff.maps_disabled"));
            return;
        }

        var playerId = player.getUuid().toString();
        var map = mapService.getMap(playerId, joinConfig.mapId());

        var playerProtocolVersion = ProtocolVersions.getProtocolVersion(player);
        if (playerProtocolVersion < map.protocolVersion()) {
            player.sendMessage(Component.translatable("map_join.wrongversion",
                    Component.text(map.name()), Component.text(ProtocolVersions.getProtocolName(map.protocolVersion()))));
            return;
        }

        MiscFunctionality.sendFadeout(player);
        try {
            logger.debug("trying to join map {} with state {} for {}", joinConfig.mapId(), joinConfig.joinMapState(), playerId);
            var res = sessionService.joinMapV2(new JoinMapRequest(playerId, joinConfig.mapId(), switch (joinConfig.joinMapState()) {
                case EDITING -> MapPresence.STATE_EDITING;
                case PLAYING -> MapPresence.STATE_PLAYING;
                case SPECTATING -> MapPresence.STATE_SPECTATING;
            }, joinConfig.source(), joinConfig.isolateOverride()));
            logger.info("join map result: {}", res);
            ProxySupport.transfer(player, res.serverClusterIp());
        } catch (Exception e) {
            if (!(e instanceof SessionService.NoAvailableServerException))
                ExceptionReporter.reportException(e, player);
            player.sendMessage(Component.translatable("map.join.fail"));
            player.clearTitle();
        }
    }

    @Override
    public void joinHub(Player player) {
        // This is a noop for the standalone hub bridge.
    }
}
