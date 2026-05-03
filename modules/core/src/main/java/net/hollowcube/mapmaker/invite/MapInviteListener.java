package net.hollowcube.mapmaker.invite;

import io.nats.client.Message;
import io.nats.client.MessageConsumer;
import io.nats.client.api.AckPolicy;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.DeliverPolicy;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.invite.types.CreatedMapInviteMessage;
import net.hollowcube.mapmaker.invite.types.InviteType;
import net.hollowcube.mapmaker.session.SessionManager;
import net.hollowcube.mapmaker.util.nats.JetStreamWrapper;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.time.Duration;
import java.util.UUID;

public final class MapInviteListener implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MapInviteListener.class);

    private static final String STREAM = "INVITES";
    private static final ConsumerConfiguration CONSUMER_CONFIG = ConsumerConfiguration.builder()
        .filterSubjects("invite.created")
        .deliverPolicy(DeliverPolicy.New)
        .ackPolicy(AckPolicy.None)
        .inactiveThreshold(Duration.ofMinutes(5))
        .build();

    private final ApiClient api;
    private final SessionManager sessionManager;

    private final MessageConsumer consumer;

    public MapInviteListener(
        @NotNull ApiClient api,
        @NotNull SessionManager sessionManager, @NotNull JetStreamWrapper jetStream
    ) {
        this.api = api;
        this.sessionManager = sessionManager;

        this.consumer = jetStream.subscribe(STREAM, CONSUMER_CONFIG, CreatedMapInviteMessage.class, this::handleInviteMessage);
    }

    @Override
    public void close() {
        try {
            consumer.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handleInviteMessage(@NotNull Message msg, @NotNull CreatedMapInviteMessage message) {
        LOGGER.info("Received invite created message: {}", message);

        var recipientId = UUID.fromString(message.recipientId());
        var player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(recipientId);
        if (player == null) {
            // Recipient is not on this server - ignore
            return;
        }

        var map = api.maps.get(message.mapId());
        var mapName = Component.text(map.name());

        var senderSession = this.sessionManager.getSession(message.senderId());
        if (senderSession == null) {
            LOGGER.error("Received invite message from unknown sender: {}", message.senderId());
            return;
        }

        var senderName = Component.text(senderSession.username());
        var senderDisplayName = api.players.getDisplayName(message.senderId());

        var playBuild = map.isPublished() ? "play" : "build";
        var inviteRequest = message.type() == InviteType.INVITE ? "invite" : "request";

        var translationString = "map." + playBuild + "." + inviteRequest + ".pending";
        player.sendMessage(Component.translatable(translationString, senderDisplayName, mapName, senderName));
    }
}
