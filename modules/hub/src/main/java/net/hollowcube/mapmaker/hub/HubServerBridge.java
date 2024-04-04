package net.hollowcube.mapmaker.hub;

import net.hollowcube.mapmaker.CoreFeatureFlags;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.player.JoinMapRequest;
import net.hollowcube.mapmaker.player.SessionService;
import net.hollowcube.mapmaker.session.MapPresence;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class HubServerBridge implements ServerBridge {
    private static final Logger logger = LoggerFactory.getLogger(HubServerBridge.class);

    private final SessionService sessionService;

    public HubServerBridge(@NotNull SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public void joinMap(@NotNull Player player, @NotNull String mapId, @NotNull JoinMapState joinMapState) {
        if (CoreFeatureFlags.MAP_DISABLE_ALL.test()) {
            player.sendMessage(Component.translatable("ff.maps_disabled"));
            return;
        }

        MiscFunctionality.sendFadeout(player);
        try {
            var playerId = player.getUuid().toString();
            logger.debug("trying to join map {} with state {} for {}", mapId, joinMapState, playerId);
            var res = sessionService.joinMapV2(new JoinMapRequest(playerId, mapId, switch (joinMapState) {
                case EDITING -> MapPresence.STATE_EDITING;
                case PLAYING -> MapPresence.STATE_PLAYING;
                case SPECTATING -> MapPresence.STATE_SPECTATING;
            }));
            logger.info("join map result: {}", res);
            player.sendPluginMessage("mapmaker:transfer", res.serverClusterIp().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            MinecraftServer.getExceptionManager().handleException(e);
            player.sendMessage(Component.text("An error occurred while trying to join the map. Please try again later."));
            player.clearTitle();
        }
    }

    @Override
    public void joinHub(@NotNull Player player) {
        // This is a noop for the standalone hub bridge.
    }
}
