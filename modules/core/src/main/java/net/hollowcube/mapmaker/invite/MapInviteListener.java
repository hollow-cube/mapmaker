package net.hollowcube.mapmaker.invite;

import com.google.gson.Gson;
import net.hollowcube.mapmaker.invite.types.CreatedMapInviteMessage;
import net.hollowcube.mapmaker.invite.types.InviteType;
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

public final class MapInviteListener extends BaseConsumer<CreatedMapInviteMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MapInviteListener.class);

    private static final String INVITE_TOPIC = "invites";
    private static final Gson GSON = AbstractHttpService.GSON;

    private final MapService mapService;
    private final PlayerService playerService;
    private final SessionManager sessionManager;

    public MapInviteListener(@NotNull MapService mapService, @NotNull PlayerService playerService,
                             @NotNull SessionManager sessionManager, @NotNull String kafkaBrokers) {
        super(INVITE_TOPIC, MapInviteListener::fromJson, kafkaBrokers);
        this.mapService = mapService;
        this.playerService = playerService;
        this.sessionManager = sessionManager;
    }

    private static @NotNull CreatedMapInviteMessage fromJson(@NotNull String json) {
        return GSON.fromJson(json, CreatedMapInviteMessage.class);
    }

    @Override
    protected void onMessage(@NotNull ConsumerRecord<String, String> kafkaRecord, @NotNull CreatedMapInviteMessage message) {
        Thread.startVirtualThread(() -> {
            LOGGER.info("Received invite created message: {}", message);

            var recipientId = UUID.fromString(message.recipientId());
            var player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(recipientId);
            if (player == null) {
                // Recipient is not on this server - ignore
                return;
            }

            var map = this.mapService.getMap(message.senderId(), message.mapId());
            var mapName = Component.text(map.name());

            var senderSession = this.sessionManager.getSession(message.senderId());
            if (senderSession == null) {
                LOGGER.error("Received invite message from unknown sender: {}", message.senderId());
                return;
            }

            var senderName = Component.text(senderSession.username());
            var senderDisplayName = this.playerService.getPlayerDisplayName2(message.senderId());

            var playBuild = map.isPublished() ? "play" : "build";
            var inviteRequest = message.type() == InviteType.INVITE ? "invite" : "request";

            var translationString = "map." + playBuild + "." + inviteRequest + ".pending";
            player.sendMessage(Component.translatable(translationString, senderDisplayName, mapName, senderName));
        });
    }
}
