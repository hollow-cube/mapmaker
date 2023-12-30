package net.hollowcube.mapmaker.session;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.kafka.BaseConsumer;
import net.hollowcube.mapmaker.kafka.KafkaConfig;
import net.hollowcube.mapmaker.player.DisplayName;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.player.SessionService;
import net.hollowcube.mapmaker.to_be_refactored.SyntheticTabListManager;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.audience.Audiences;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.network.ConnectionManager;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);

    private static final ConnectionManager CONNECTION_MANAGER = MinecraftServer.getConnectionManager();

    private final SessionService sessionService;
    private final PlayerService playerService;
    private final ConsumerImpl consumer;

    private final Map<String, PlayerSession> sessions = new ConcurrentHashMap<>(); // All sessions, including local ones
    private final SyntheticTabListManager syntheticTab;

    public SessionManager(@NotNull SessionService sessionService, @NotNull PlayerService playerService, @NotNull KafkaConfig kafkaConfig, boolean noop) {
        this.sessionService = sessionService;
        this.playerService = playerService;

        if (!noop) this.consumer = new ConsumerImpl(String.join(",", kafkaConfig.bootstrapServers()));
        else this.consumer = null;

        this.syntheticTab = new SyntheticTabListManager(this, playerService);
        MinecraftServer.getGlobalEventHandler()
                .addListener(PlayerSpawnEvent.class, this::handlePlayerSpawn)
                .addListener(PlayerDisconnectEvent.class, this::handlePlayerDisconnect);
    }

    public void sync() {
        for (var newSession : sessionService.sync()) {
            sessions.put(newSession.playerId(), newSession);
            syntheticTab.addSession(newSession);
        }

        logger.info("synced session manager with {} sessions", sessions.size());
    }

    public void close() {
        consumer.close();
    }

    public @Nullable PlayerSession getSession(@NotNull String playerId) {
        return sessions.get(playerId);
    }

    public @Nullable Presence getPresence(@NotNull String playerId) {
        var session = getSession(playerId);
        return session == null ? null : session.presence();
    }

    public @NotNull Collection<PlayerSession> sessions() {
        return sessions.values();
    }

    public int networkPlayerCount() {
        return sessions.size();
    }

    private void handleSessionCreate(@NotNull SessionUpdateMessage message) {
        logger.info("remote session created for {}", message.playerId());
        sessions.put(message.playerId(), message.session());

        var displayName = playerService.getPlayerDisplayName2(message.playerId());
        var joinMessage = Component.translatable("chat.player.join", displayName.build(DisplayName.Context.DEFAULT));

        for (var player : CONNECTION_MANAGER.getOnlinePlayers()) {
            // Do not send the join message to the player who joined, we send that to them immediately on join so that it feels better
            if (player.getUuid().toString().equals(message.playerId())) continue;

            player.sendMessage(joinMessage);
        }

        syntheticTab.addSession(message.session());
    }

    private void handleSessionDelete(@NotNull SessionUpdateMessage message) {
        logger.info("remote session deleted for {}", message.playerId());
        var removed = sessions.remove(message.playerId());
        if (removed == null) return;

        var displayName = playerService.getPlayerDisplayName2(message.playerId());
        var leaveMessage = Component.translatable("chat.player.leave", displayName.build(DisplayName.Context.DEFAULT));

        var allPlayers = Audiences.players();
        allPlayers.sendMessage(leaveMessage);

        syntheticTab.removeSession(message.playerId());
    }

    private void handleSessionUpdate(@NotNull SessionUpdateMessage message) {
        logger.info("remote session updated for {}", message.playerId());

        logger.info("UPDATE CONTENT: {}", message.session());
        sessions.put(message.playerId(), message.session());
    }

    private void handlePlayerSpawn(@NotNull PlayerSpawnEvent event) {
        if (!event.isFirstSpawn()) return;

        syntheticTab.addLocalPlayer(event.getPlayer());
    }

    private void handlePlayerDisconnect(@NotNull PlayerDisconnectEvent event) {
        syntheticTab.removeLocalPlayer(event.getPlayer());
    }

    private class ConsumerImpl extends BaseConsumer<SessionUpdateMessage> {

        protected ConsumerImpl(@NotNull String bootstrapServers) {
            super("session-updates", AbstractHttpService.hostname, s -> AbstractHttpService.GSON.fromJson(s, SessionUpdateMessage.class), bootstrapServers);
        }

        @Override
        protected void onMessage(@NotNull ConsumerRecord<String, String> kafkaRecord, @NotNull SessionUpdateMessage message) {
            switch (message.action()) {
                case CREATE -> FutureUtil.submitVirtual(() -> handleSessionCreate(message));
                case DELETE -> FutureUtil.submitVirtual(() -> handleSessionDelete(message));
                case UPDATE -> FutureUtil.submitVirtual(() -> handleSessionUpdate(message));
            }
        }
    }
}
