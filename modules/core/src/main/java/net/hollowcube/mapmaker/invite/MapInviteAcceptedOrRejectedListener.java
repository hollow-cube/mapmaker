package net.hollowcube.mapmaker.invite;

import io.nats.client.Message;
import io.nats.client.MessageConsumer;
import io.nats.client.api.AckPolicy;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.DeliverPolicy;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.invite.types.InviteType;
import net.hollowcube.mapmaker.invite.types.MapInviteAcceptedOrRejectedMessage;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.map.runtime.ServerBridge.JoinMapState;
import net.hollowcube.mapmaker.session.SessionManager;
import net.hollowcube.mapmaker.util.nats.JetStreamWrapper;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.time.Duration;
import java.util.UUID;

public final class MapInviteAcceptedOrRejectedListener implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MapInviteAcceptedOrRejectedListener.class);

    // TODO: this consumer can just be merged with InviteConsumer they listen to basically the same
    private static final String STREAM = "INVITES";
    private static final ConsumerConfiguration CONSUMER_CONFIG = ConsumerConfiguration.builder()
        .filterSubjects("invite.accepted", "invite.rejected")
        .deliverPolicy(DeliverPolicy.New)
        .ackPolicy(AckPolicy.None)
        .inactiveThreshold(Duration.ofMinutes(5))
        .build();

    private final ApiClient api;
    private final SessionManager sessionManager;
    private final ServerBridge serverBridge;

    private final MessageConsumer consumer;

    public MapInviteAcceptedOrRejectedListener(
        @NotNull ApiClient api,
        @NotNull SessionManager sessionManager, @NotNull ServerBridge serverBridge,
        @NotNull JetStreamWrapper jetStream
    ) {
        this.api = api;
        this.sessionManager = sessionManager;
        this.serverBridge = serverBridge;

        this.consumer = jetStream.subscribe(STREAM, CONSUMER_CONFIG, MapInviteAcceptedOrRejectedMessage.class, this::handleInviteMessage);
    }

    @Override
    public void close() {
        try {
            consumer.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Blocking
    private void handleInviteMessage(@NotNull Message msg, @NotNull MapInviteAcceptedOrRejectedMessage message) {
        LOGGER.info("Received invite accepted or rejected message: {}", message);

        this.sendAcceptedOrRejectedMessageToRecipient(message);
        this.sendCorrectPlayerToServer(message);
    }

    private void sendAcceptedOrRejectedMessageToRecipient(@NotNull MapInviteAcceptedOrRejectedMessage message) {
        var senderId = UUID.fromString(message.senderId());
        var sender = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(senderId);
        if (sender == null) {
            // Sender is not on this server - ignore
            LOGGER.debug("Invite sender or request recipient not on this server - ignoring");
            return;
        }

        var map = api.maps.get(message.mapId());
        var mapName = Component.text(map.name());

        var targetId = message.recipientId();
        var targetSession = this.sessionManager.getSession(targetId);
        if (targetSession == null) {
            LOGGER.error("Received invite accept or reject message for unknown target: {}", targetId);
            return;
        }

        var targetName = Component.text(targetSession.username());
        var targetDisplayName = api.players.getDisplayName(targetId);

        var playBuild = map.isPublished() ? "play" : "build";
        var inviteRequest = message.type() == InviteType.INVITE ? "invite" : "request";
        var acceptReject = message.accepted() ? "accepted" : "rejected";

        var translationString = "map." + playBuild + "." + inviteRequest + "." + acceptReject;
        sender.sendMessage(Component.translatable(translationString, targetDisplayName, mapName, targetName));
    }

    @Blocking
    private void sendCorrectPlayerToServer(@NotNull MapInviteAcceptedOrRejectedMessage message) {
        var type = message.type();

        if (type == InviteType.INVITE) {
            // For invites, we move the recipient to the sender's map
            this.joinMap(UUID.fromString(message.recipientId()), message.senderId(), message.mapId(), "invite");
        } else {
            // For requests, we move the sender to the recipient's map
            this.joinMap(UUID.fromString(message.senderId()), message.recipientId(), message.mapId(), "request");
        }
    }

    @Blocking
    private void joinMap(@NotNull UUID playerId, @NotNull String targetPlayerId, @NotNull String mapId, @NotNull String source) {
        var player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(playerId);
        if (player == null) return;

        var map = api.maps.get(mapId);
        this.serverBridge.joinMap(player, mapId, map.isPublished() ? JoinMapState.PLAYING : JoinMapState.EDITING, source);
    }
}
