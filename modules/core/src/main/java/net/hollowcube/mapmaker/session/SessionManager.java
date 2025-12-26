package net.hollowcube.mapmaker.session;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.kafka.BaseConsumer;
import net.hollowcube.mapmaker.kafka.KafkaConfig;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.perm.PlatformPerm;
import net.hollowcube.mapmaker.player.DisplayName;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.player.SessionService;
import net.hollowcube.mapmaker.to_be_refactored.SyntheticTabListManager;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.audience.Audiences;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.network.ConnectionManager;
import net.minestom.server.utils.validate.Check;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jctools.queues.MpscArrayQueue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Predicate;

public class SessionManager {
    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);

    @Deprecated
    public static SessionManager instance; // todo fixme this is garbage

    private static final ConnectionManager CONNECTION_MANAGER = MinecraftServer.getConnectionManager();

    private final SessionService sessionService;
    private final PlayerService playerService;
    private final ConsumerImpl consumer;

    private final Map<String, PlayerSession> sessions = new ConcurrentHashMap<>(); // All sessions, including local ones
    private final SyntheticTabListManager syntheticTab;

    private final Predicate<String> hasSeeVanishedPerm;

    public SessionManager(
        @NotNull SessionService sessionService,
        @NotNull PlayerService playerService,
        @NotNull PermManager permManager,
        @NotNull KafkaConfig kafkaConfig,
        boolean noop
    ) {
        instance = this;
        this.sessionService = sessionService;
        this.playerService = playerService;

        if (!noop) this.consumer = new ConsumerImpl(String.join(",", kafkaConfig.bootstrapServerList()));
        else this.consumer = null;

        this.syntheticTab = new SyntheticTabListManager(this, playerService);
        MinecraftServer.getGlobalEventHandler()
            .addListener(PlayerSpawnEvent.class, this::handlePlayerSpawn);

        this.hasSeeVanishedPerm = permManager.createPrefetchedCondition(PlatformPerm.SEE_VANISHED);
    }

    public void sync() {
        for (var newSession : sessionService.sync()) {
            sessions.put(newSession.playerId(), newSession);
            if (!newSession.hidden()) syntheticTab.addSession(newSession);
        }

        logger.info("synced session manager with {} sessions", sessions.size());
    }

    public void close() {
        if (consumer != null) consumer.close();
    }

    public boolean isHidden(@NotNull String playerId) {
        var session = getSession(playerId);
        return session != null && session.hidden();
    }

    public @Nullable PlayerSession getSession(@NotNull String playerId) {
        return sessions.get(playerId);
    }

    public @Nullable PlayerSession getSessionByName(@NotNull String playerName) {
        for (var session : sessions.values()) {
            if (session.username().equalsIgnoreCase(playerName)) {
                return session;
            }
        }
        return null;
    }

    public @Nullable Presence getPresence(@NotNull String playerId) {
        var session = getSession(playerId);
        return session == null ? null : session.presence();
    }

    public void updateState(@NotNull String playerId, @NotNull SessionStateUpdateRequest req) {
        Check.stateCondition(!sessions.containsKey(playerId), "session does not exist");
        updateSessionOptimistic(sessionService.updateSessionProperties(playerId, req), req.metadata());
    }

    public @NotNull Collection<PlayerSession> sessions(boolean showHidden) {
        if (showHidden) return sessions.values();
        return sessions.values().stream().filter(s -> !s.hidden()).toList();
    }

    /**
     * Updates the local state of the given session for the given player. This is used in cases where the local server
     * does a session update and needs to apply the result immediately.
     *
     * @param session The new session state
     */
    public void updateSessionOptimistic(@NotNull PlayerSession session, @NotNull SessionStateUpdateRequest.Metadata metadata) {
        var oldSession = sessions.get(session.playerId());
        if (oldSession == null) {
            handleSessionCreate(session);
        } else {
            handleSessionUpdate(session, metadata);
        }
    }

    private void handleSessionCreate(@NotNull PlayerSession session) {
        logger.debug("remote session created for {}", session.playerId());
        sessions.put(session.playerId(), session);

        // Do not send a join message/add to tab list if the player is hidden
        if (session.hidden()) return;

        var joinMessage = buildJoinMessage(session.playerId());
        for (var player : CONNECTION_MANAGER.getOnlinePlayers()) {
            // Do not send the join message to the player who joined, we send that to them immediately on join so that it feels better
            if (player.getUuid().toString().equals(session.playerId())) continue;
            player.sendMessage(joinMessage);
        }

        syntheticTab.addSession(session);
    }

    private void handleSessionDelete(@NotNull SessionUpdateMessage message) {
        logger.debug("remote session deleted for {}", message.playerId());
        var removed = sessions.remove(message.playerId());
        if (removed == null) return;

        // Only send a leave message if the player is not hidden
        if (!removed.hidden()) {
            broadcastLeaveMessage(message.playerId());
            syntheticTab.removeSession(message.playerId());
        }

        // If we have the player locally that is bad, kick them immediately.
        var player = CONNECTION_MANAGER.getOnlinePlayerByUuid(UUID.fromString(message.playerId()));
        if (player != null) player.kick(Component.text("An error has occurred, please try again.\n(session.kicked)"));
    }

    private void handleSessionUpdate(@NotNull PlayerSession session, @NotNull SessionStateUpdateRequest.Metadata metadata) {
        logger.debug("remote session updated for {}", session.playerId());

        //todo need to make this more complicated so that people who have extra perms to see invis players can see them. they still get the leave message probably
        var oldSession = sessions.put(session.playerId(), session);
        if (oldSession == null) return;
        if (oldSession.hidden() && !session.hidden()) {
            // Player became visible
            boolean isSilent = metadata.hideSilent() != null && metadata.hideSilent();
            if (!isSilent) broadcastJoinMessage(session.playerId());
            syntheticTab.addSession(session);

            var player = CONNECTION_MANAGER.getOnlinePlayerByUuid(UUID.fromString(session.playerId()));
            if (player != null) configureVisiblePlayer(player);
        } else if (!oldSession.hidden() && session.hidden()) {
            // Player became hidden
            boolean isSilent = metadata.hideSilent() != null && metadata.hideSilent();
            if (!isSilent) broadcastLeaveMessage(session.playerId());
            syntheticTab.removeSession(session.playerId());

            var player = CONNECTION_MANAGER.getOnlinePlayerByUuid(UUID.fromString(session.playerId()));
            if (player != null) configureVanishedPlayer(player);
        }
    }

    private void handlePlayerSpawn(@NotNull PlayerSpawnEvent event) {
        if (!event.isFirstSpawn()) return;

        syntheticTab.addLocalPlayer(event.getPlayer());
    }

    private @Nullable Component buildJoinMessage(@NotNull String playerId) {
        var displayName = playerService.getPlayerDisplayName2(playerId);
        if (!showJoinLeaveMessage(displayName)) return null;

        return Component.translatable("chat.player.join", displayName.build(DisplayName.Context.DEFAULT));
    }

    private @Nullable Component buildLeaveMessage(@NotNull String playerId) {
        var displayName = playerService.getPlayerDisplayName2(playerId);
        if (!showJoinLeaveMessage(displayName)) return null;

        return Component.translatable("chat.player.leave", displayName.build(DisplayName.Context.DEFAULT));
    }

    private boolean showJoinLeaveMessage(@NotNull DisplayName displayName) {
        return displayName.getBadgeName() != null; // Show anyone with a badge for now.
    }

    private void broadcastJoinMessage(@NotNull String playerId) {
        var joinMessage = buildJoinMessage(playerId);
        if (joinMessage == null) return;

        Audiences.players().sendMessage(joinMessage);
    }

    private void broadcastLeaveMessage(@NotNull String playerId) {
        var leaveMessage = buildLeaveMessage(playerId);
        if (leaveMessage == null) return;

        Audiences.players().sendMessage(leaveMessage);
    }

    public void configureVanishedPlayer(@NotNull Player player) {
        player.updateViewableRule(p -> hasSeeVanishedPerm.test(PlayerData.fromPlayer(p).id()));
    }

    private void configureVisiblePlayer(@NotNull Player player) {
        player.updateViewableRule(null);
    }

    private class ConsumerImpl extends BaseConsumer<SessionUpdateMessage> {
        private final MpscArrayQueue<SessionUpdateMessage> queue = new MpscArrayQueue<>(512);
        private final Thread thread = Thread.startVirtualThread(this::processQueue);

        protected ConsumerImpl(@NotNull String bootstrapServers) {
            super("session-updates", AbstractHttpService.hostname, s -> AbstractHttpService.GSON.fromJson(s, SessionUpdateMessage.class), bootstrapServers);
        }

        @Override
        public void close() {
            this.thread.interrupt();
            super.close();
        }

        @Override
        protected void onMessage(@NotNull ConsumerRecord<String, String> kafkaRecord, @NotNull SessionUpdateMessage message) {
            while (!queue.relaxedOffer(message))
                FutureUtil.sleep(1000);
            LockSupport.unpark(thread);
        }

        private void processQueue() {
            while (true) {
                LockSupport.park();
                if (this.thread.isInterrupted() || !this.thread.isAlive())
                    return;

                queue.drain(message -> {
                    switch (message.action()) {
                        case CREATE -> {
                            // We have to check if the session exists here because its possible we did an optimistic update on the session before the message arrived.
                            var oldSession = sessions.get(message.playerId());
                            if (oldSession == null) {
                                handleSessionCreate(message.session());
                            } else {
                                handleSessionUpdate(message.session(), message.metadata());
                            }
                        }
                        case DELETE -> handleSessionDelete(message);
                        case UPDATE -> handleSessionUpdate(message.session(), message.metadata());
                    }
                });
            }
        }
    }
}
