package net.hollowcube.mapmaker.invite;

import com.google.gson.Gson;
import net.hollowcube.mapmaker.invite.types.InviteType;
import net.hollowcube.mapmaker.invite.types.MapInviteAcceptedOrRejectedMessage;
import net.hollowcube.mapmaker.kafka.BaseConsumer;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.session.SessionManager;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public final class MapInviteAcceptedOrRejectedListener extends BaseConsumer<MapInviteAcceptedOrRejectedMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MapInviteListener.class);

    private static final String INVITE_ACCEPT_REJECT_TOPIC = "invite-accept-reject";
    private static final Gson GSON = AbstractHttpService.GSON;

    private final MapService mapService;
    private final PlayerService playerService;
    private final SessionManager sessionManager;

    public MapInviteAcceptedOrRejectedListener(@NotNull MapService mapService, @NotNull PlayerService playerService,
                                               @NotNull SessionManager sessionManager, @NotNull String kafkaBrokers) {
        super(INVITE_ACCEPT_REJECT_TOPIC, "invites", MapInviteAcceptedOrRejectedListener::fromJson, kafkaBrokers);
        this.mapService = mapService;
        this.playerService = playerService;
        this.sessionManager = sessionManager;
    }

    private static @NotNull MapInviteAcceptedOrRejectedMessage fromJson(@NotNull String json) {
        return GSON.fromJson(json, MapInviteAcceptedOrRejectedMessage.class);
    }

    @Override
    protected void onMessage(@NotNull ConsumerRecord<String, String> kafkaRecord,
                             @NotNull MapInviteAcceptedOrRejectedMessage message) {
        Thread.startVirtualThread(() -> {
            LOGGER.info("Received invite accepted or rejected message: {}", message);

            var senderId = UUID.fromString(message.senderId());
            var player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(senderId);
            if (player == null) {
                // Recipient is not on this server - ignore
                return;
            }

            var map = this.mapService.getMap(message.recipientId(), message.mapId());
            var mapName = Component.text(map.name());

            var recipientSession = this.sessionManager.getSession(message.recipientId());
            if (recipientSession == null) {
                LOGGER.error("Received invite accept or reject message from unknown recipient: {}", message.recipientId());
                return;
            }

            var recipientName = Component.text(recipientSession.username());
            var recipientDisplayName = this.playerService.getPlayerDisplayName2(message.recipientId());

            var playBuild = map.isPublished() ? "play" : "build";
            var inviteRequest = message.type() == InviteType.INVITE ? "invite" : "request";
            var acceptReject = message.accepted() ? "accepted" : "rejected";

            var translationString = "map." + playBuild + "." + inviteRequest + "." + acceptReject;
            player.sendMessage(Component.translatable(translationString, recipientDisplayName, mapName, recipientName));
        });
    }
}
