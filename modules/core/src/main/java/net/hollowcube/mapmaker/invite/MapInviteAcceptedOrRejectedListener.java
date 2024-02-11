package net.hollowcube.mapmaker.invite;

import com.google.gson.Gson;
import net.hollowcube.mapmaker.bridge.ServerBridge;
import net.hollowcube.mapmaker.bridge.ServerBridge.JoinMapState;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(MapInviteAcceptedOrRejectedListener.class);

    private static final String INVITE_ACCEPT_REJECT_TOPIC = "invite-accept-reject";
    private static final Gson GSON = AbstractHttpService.GSON;

    private final MapService mapService;
    private final PlayerService playerService;
    private final SessionManager sessionManager;
    private final ServerBridge serverBridge;

    public MapInviteAcceptedOrRejectedListener(@NotNull MapService mapService, @NotNull PlayerService playerService,
                                               @NotNull SessionManager sessionManager,
                                               @NotNull ServerBridge serverBridge, @NotNull String kafkaBrokers) {
        super(INVITE_ACCEPT_REJECT_TOPIC, MapInviteAcceptedOrRejectedListener::fromJson, kafkaBrokers);
        this.mapService = mapService;
        this.playerService = playerService;
        this.sessionManager = sessionManager;
        this.serverBridge = serverBridge;
    }

    private static @NotNull MapInviteAcceptedOrRejectedMessage fromJson(@NotNull String json) {
        return GSON.fromJson(json, MapInviteAcceptedOrRejectedMessage.class);
    }

    @Override
    protected void onMessage(@NotNull ConsumerRecord<String, String> kafkaRecord,
                             @NotNull MapInviteAcceptedOrRejectedMessage message) {
        Thread.startVirtualThread(() -> {
            LOGGER.info("Received invite accepted or rejected message: {}", message);

            this.sendAcceptedOrRejectedMessageToRecipient(message);
            this.sendCorrectPlayerToServer(message);
        });
    }

    private void sendAcceptedOrRejectedMessageToRecipient(@NotNull MapInviteAcceptedOrRejectedMessage message) {
        var senderId = UUID.fromString(message.senderId());
        var sender = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(senderId);
        if (sender == null) {
            // Sender is not on this server - ignore
            LOGGER.debug("Invite sender or request recipient not on this server - ignoring");
            return;
        }

        var map = this.mapService.getMap(message.recipientId(), message.mapId());
        var mapName = Component.text(map.name());

        var targetId = message.recipientId();
        var targetSession = this.sessionManager.getSession(targetId);
        if (targetSession == null) {
            LOGGER.error("Received invite accept or reject message for unknown target: {}", targetId);
            return;
        }

        var targetName = Component.text(targetSession.username());
        var targetDisplayName = this.playerService.getPlayerDisplayName2(targetId);

        var playBuild = map.isPublished() ? "play" : "build";
        var inviteRequest = message.type() == InviteType.INVITE ? "invite" : "request";
        var acceptReject = message.accepted() ? "accepted" : "rejected";

        var translationString = "map." + playBuild + "." + inviteRequest + "." + acceptReject;
        sender.sendMessage(Component.translatable(translationString, targetDisplayName, mapName, targetName));
    }

    private void sendCorrectPlayerToServer(@NotNull MapInviteAcceptedOrRejectedMessage message) {
        var type = message.type();

        if (type == InviteType.INVITE) {
            // For invites, we move the recipient to the sender's map
            this.joinMap(UUID.fromString(message.recipientId()), message.senderId(), message.mapId());
        } else {
            // For requests, we move the sender to the recipient's map
            this.joinMap(UUID.fromString(message.senderId()), message.recipientId(), message.mapId());
        }
    }

    private void joinMap(@NotNull UUID playerId, @NotNull String targetPlayerId, @NotNull String mapId) {
        var player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(playerId);
        if (player == null) return;

        var map = this.mapService.getMap(targetPlayerId, mapId);
        this.serverBridge.joinMap(player, mapId, map.isPublished() ? JoinMapState.PLAYING : JoinMapState.EDITING);
    }
}
