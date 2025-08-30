package net.hollowcube.mapmaker.map;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.common.util.ProtocolVersions;
import net.hollowcube.mapmaker.CoreFeatureFlags;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.misc.ProxySupport;
import net.hollowcube.mapmaker.player.JoinHubRequest;
import net.hollowcube.mapmaker.player.JoinMapRequest;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.SessionService;
import net.hollowcube.mapmaker.session.MapPresence;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapServerBridge implements ServerBridge {
    private static final Logger logger = LoggerFactory.getLogger(MapServerBridge.class);

    private final MapMapServer server;

    public MapServerBridge(@NotNull MapMapServer server) {
        this.server = server;
    }

    @Override
    public void joinMap(@NotNull Player player, @NotNull String mapId, @NotNull JoinMapState joinMapState, @NotNull String source) {
        if (CoreFeatureFlags.MAP_DISABLE_ALL.test()) {
            player.sendMessage(Component.translatable("ff.maps_disabled"));
            return;
        }

        try {
            var playerId = player.getUuid().toString();
            logger.debug("trying to join map {} with state {} for {}", mapId, joinMapState, playerId);

            var map = server.mapService().getMap(playerId, mapId);

            var playerProtocolVersion = ProtocolVersions.getProtocolVersion(player);
            if (playerProtocolVersion < map.protocolVersion()) {
                player.sendMessage(Component.translatable("map_join.wrongversion",
                        Component.text(map.name()), Component.text(ProtocolVersions.getProtocolName(map.protocolVersion()))));
                return;
            }

            var targetState = switch (joinMapState) {
                case EDITING -> MapPresence.STATE_EDITING;
                case PLAYING -> MapPresence.STATE_PLAYING;
                case SPECTATING -> MapPresence.STATE_SPECTATING;
            };
            var response = server.sessionService().joinMapV2(new JoinMapRequest(playerId, mapId, targetState, source));
            logger.info("join map result: {}", response);

            var currentServerId = AbstractHttpService.hostname;
            if (currentServerId.equals(response.server())) {
                logger.info("moving between maps on this server");
                this.moveBetweenMapsOnThisServer(player, mapId, targetState);
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
            var playerData = PlayerData.fromPlayer(player);
            var res = server.sessionService().joinHubV2(new JoinHubRequest(playerData.id()));
            logger.info("join hub result: {}", res);
            ProxySupport.transfer(player, res.serverClusterIp());
        } catch (SessionService.NoAvailableServerException ignored) {
            player.sendMessage("No hub server is available!");
        } catch (Exception e) {
            ExceptionReporter.reportException(e, player);
            player.sendMessage(Component.text("An error occurred while trying to return to the hub. Please try again later."));
        }
    }

    private void moveBetweenMapsOnThisServer(@NotNull Player player, @NotNull String mapId, @NotNull String state) {
        FutureUtil.assertThread();

        try {
            // Add a pending join and then send them to the config state where it will be acted upon.
            var playerId = PlayerData.fromPlayer(player).id();
            server.addPendingJoin(playerId, mapId, state);

            // We need to remove the player from the map before entering configuration, because by the time we get
            // remove from instance event, the player already had their position reset (ie they are at 0,0,0).
            var world = MapWorld.forPlayer(player);
            if (world == null) {
                MinecraftServer.getSchedulerManager().scheduleEndOfTick(player::startConfigurationPhase);
            } else {
                world.scheduleRemovePlayer(player).thenRun(player::startConfigurationPhase);
            }
        } catch (Exception exception) {
            logger.error("Failed to move player to map", exception);
            player.sendMessage("Failed to move to map");
            this.joinHub(player);
        }
    }
}
