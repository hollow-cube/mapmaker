package net.hollowcube.mapmaker.session;

import io.nats.client.Message;
import io.nats.client.MessageConsumer;
import io.nats.client.api.AckPolicy;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.DeliverPolicy;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.player.*;
import net.hollowcube.mapmaker.to_be_refactored.SyntheticTabListManager;
import net.hollowcube.mapmaker.util.nats.JetStreamWrapper;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.audience.Audiences;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.network.ConnectionManager;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);

    @Deprecated
    public static SessionManager instance; // todo fixme this is garbage

    private static final ConnectionManager CONNECTION_MANAGER = MinecraftServer.getConnectionManager();

    private static final String STREAM = "SESSIONS";
    private static final ConsumerConfiguration CONSUMER_CONFIG = ConsumerConfiguration.builder()
        .filterSubjects("session.>")
        .deliverPolicy(DeliverPolicy.New)
        .ackPolicy(AckPolicy.None)
        .inactiveThreshold(Duration.ofMinutes(5))
        .build();

    private final SessionService sessionService;
    private final PlayerService playerService;

    private final Map<String, PlayerSession> sessions = new ConcurrentHashMap<>(); // All sessions, including local ones
    private final SyntheticTabListManager syntheticTab;

    private final MessageConsumer consumer;

    public SessionManager(SessionService sessionService, PlayerService playerService, JetStreamWrapper jetStream) {
        instance = this;
        this.sessionService = sessionService;
        this.playerService = playerService;

        this.syntheticTab = new SyntheticTabListManager(this, playerService);
        MinecraftServer.getGlobalEventHandler()
            .addListener(PlayerSpawnEvent.class, this::handlePlayerSpawn);

        this.consumer = jetStream.subscribe(STREAM, CONSUMER_CONFIG, SessionUpdateMessage.class, this::handleSessionUpdateMessage);
        Thread.startVirtualThread(this::incrementalSyncLoop); // begin doing incremental sync every few minutes to ensure we stay in sync.
    }

    public void sync() {
        for (var newSession : sessionService.sync()) {
            sessions.put(newSession.playerId(), newSession);
            if (!newSession.hidden()) syntheticTab.addSession(newSession);
        }

        logger.info("synced session manager with {} sessions", sessions.size());
    }

    public void syncIncremental() {
        var oldSessions = new HashSet<>(sessions.keySet());
        for (var session : sessionService.sync()) {
            if (oldSessions.remove(session.playerId())) {
                handleSessionCreate(session);
            } else {
                handleSessionUpdate(session, new SessionStateUpdateRequest.Metadata());
            }
        }

        for (var deleted : oldSessions) {
            handleSessionDelete(new SessionUpdateMessage(SessionUpdateMessage.Action.DELETE, deleted, null, null));
        }
    }

    public void close() {
        try {
            consumer.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isHidden(String playerId) {
        var session = getSession(playerId);
        return session != null && session.hidden();
    }

    public @Nullable PlayerSession getSession(String playerId) {
        return sessions.get(playerId);
    }

    public @Nullable PlayerSession getSessionByName(String playerName) {
        for (var session : sessions.values()) {
            if (session.username().equalsIgnoreCase(playerName)) {
                return session;
            }
        }
        return null;
    }

    public @Nullable Presence getPresence(String playerId) {
        var session = getSession(playerId);
        return session == null ? null : session.presence();
    }

    public void updateState(String playerId, SessionStateUpdateRequest req) {
        Check.stateCondition(!sessions.containsKey(playerId), "session does not exist");
        updateSessionOptimistic(sessionService.updateSessionProperties(playerId, req), req.metadata());
    }

    public Collection<PlayerSession> sessions(boolean showHidden) {
        if (showHidden) return sessions.values();
        return sessions.values().stream().filter(s -> !s.hidden()).toList();
    }

    /**
     * Updates the local state of the given session for the given player. This is used in cases where the local server
     * does a session update and needs to apply the result immediately.
     *
     * @param session The new session state
     */
    public void updateSessionOptimistic(PlayerSession session, SessionStateUpdateRequest.Metadata metadata) {
        var oldSession = sessions.get(session.playerId());
        if (oldSession == null) {
            handleSessionCreate(session);
        } else {
            handleSessionUpdate(session, metadata);
        }
    }

    private void handleSessionCreate(PlayerSession session) {
        if (sessions.containsKey(session.playerId())) {
            handleSessionUpdate(session, new SessionStateUpdateRequest.Metadata());
            return;
        }

        logger.debug("remote session created for {}", session.playerId());
        sessions.put(session.playerId(), session);

        // Do not send a join message/add to tab list if the player is hidden
        if (session.hidden()) return;

        this.broadcastJoinMessage(session.playerId());
        syntheticTab.addSession(session);
    }

    private void handleSessionDelete(SessionUpdateMessage message) {
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

    private void handleSessionUpdate(PlayerSession session, SessionStateUpdateRequest.Metadata metadata) {
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

    private void handlePlayerSpawn(PlayerSpawnEvent event) {
        if (!event.isFirstSpawn()) return;

        syntheticTab.addLocalPlayer(event.getPlayer());
    }

    private boolean showJoinLeaveMessage(DisplayName displayName) {
        return displayName.getBadgeName() != null; // Show anyone with a badge for now.
    }

    private void broadcastJoinMessage(String playerId) {
        List<String> friends = this.playerService.getPlayerFriends(playerId, true, new PlayerService.Pageable(1, 10_000)).items()
            .stream().map(PlayerFriend::playerId).toList();
        var displayName = this.playerService.getPlayerDisplayName2(playerId);

        if (showJoinLeaveMessage(displayName)) {
            // only send to non-friends
            Audiences.players(lPlayer -> !friends.contains(lPlayer.getUuid().toString()))
                .sendMessage(Component.translatable("chat.player.join", displayName.build(DisplayName.Context.DEFAULT)));
        }

        Audiences.players(lPlayer -> friends.contains(lPlayer.getUuid().toString()))
            .sendMessage(Component.translatable("chat.friend.join", displayName.build(DisplayName.Context.DEFAULT)));
    }

    private void broadcastLeaveMessage(String playerId) {
        List<String> friends = this.playerService.getPlayerFriends(playerId, true, new PlayerService.Pageable(1, 10_000)).items()
            .stream().map(PlayerFriend::playerId).toList();
        var displayName = this.playerService.getPlayerDisplayName2(playerId);

        if (showJoinLeaveMessage(displayName)) {
            // only send to non-friends
            Audiences.players(player -> !friends.contains(player.getUuid().toString()))
                .sendMessage(Component.translatable("chat.player.leave", displayName.build(DisplayName.Context.DEFAULT)));
        }

        Audiences.players(player -> friends.contains(player.getUuid().toString()))
            .sendMessage(Component.translatable("chat.friend.leave", displayName.build(DisplayName.Context.DEFAULT)));
    }

    public void configureVanishedPlayer(Player player) {
        player.scheduleNextTick(e -> e.updateViewableRule(p -> PlayerData.fromPlayer(p).has(Permission.GENERIC_STAFF)));
    }

    private void configureVisiblePlayer(Player player) {
        player.updateViewableRule(null);
    }

    private void handleSessionUpdateMessage(Message msg, SessionUpdateMessage message) {
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
    }

    @Blocking
    private void incrementalSyncLoop() {
        FutureUtil.sleep(30_000);
        while (true) {
            try {
                syncIncremental();
            } catch (Exception e) {
                ExceptionReporter.reportException(e);
            }

            FutureUtil.sleep(5 * 60 * 1000);
        }
    }
}
