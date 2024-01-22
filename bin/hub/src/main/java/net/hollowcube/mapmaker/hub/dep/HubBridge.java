package net.hollowcube.mapmaker.hub.dep;

import net.hollowcube.mapmaker.bridge.HubToMapBridge;
import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.player.JoinMapRequest;
import net.hollowcube.mapmaker.player.SessionService;
import net.hollowcube.mapmaker.session.MapPresence;
import net.hollowcube.mapmaker.session.Presence;
import net.hollowcube.mapmaker.session.SessionManager;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class HubBridge implements HubToMapBridge {
    private static final Logger logger = LoggerFactory.getLogger(HubBridge.class);

    private final SessionService sessionService;
    private final SessionManager sessionManager;

    public HubBridge(@NotNull SessionService sessionService, @NotNull SessionManager sessionManager) {
        this.sessionService = sessionService;
        this.sessionManager = sessionManager;
    }

    @Override
    public void joinMap(@NotNull Player player, @NotNull String mapId, @NotNull JoinMapState joinMapState) {
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
    public @Nullable String getCurrentMap(@NotNull Player player) {
        var presence = sessionManager.getPresence(player.getUuid().toString());
        if (presence == null || !Presence.TYPE_MAPMAKER_MAP.equals(presence.type())) return null;
        return presence.mapId();
    }
}
