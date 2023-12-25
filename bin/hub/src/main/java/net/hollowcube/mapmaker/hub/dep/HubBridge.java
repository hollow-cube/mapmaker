package net.hollowcube.mapmaker.hub.dep;

import net.hollowcube.mapmaker.bridge.HubToMapBridge;
import net.hollowcube.mapmaker.player.JoinMapRequest;
import net.hollowcube.mapmaker.player.SessionService;
import net.hollowcube.mapmaker.session.MapPresence;
import net.hollowcube.mapmaker.session.Presence;
import net.hollowcube.mapmaker.session.SessionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

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
        player.showTitle(Title.title(Component.text("Joining map..."), Component.empty(), Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(10_000), Duration.ofMillis(500))));

        var playerId = player.getUuid().toString();
        logger.debug("trying to join map {} with state {} for {}", mapId, joinMapState, playerId);
        var res = sessionService.joinMapV2(new JoinMapRequest(playerId, mapId, switch (joinMapState) {
            case EDITING -> MapPresence.STATE_EDITING;
            case PLAYING -> MapPresence.STATE_PLAYING;
            case SPECTATING -> MapPresence.STATE_SPECTATING;
        }));
        logger.info("join map result: {}", res);
        player.sendPluginMessage("mapmaker:transfer", res.serverClusterIp().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public @Nullable String getCurrentMap(@NotNull Player player) {
        var presence = sessionManager.getPresence(player.getUuid().toString());
        if (presence == null || !Presence.TYPE_MAPMAKER_MAP.equals(presence.type())) return null;
        return presence.mapId();
    }
}
