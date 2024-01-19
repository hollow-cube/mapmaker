package net.hollowcube.mapmaker.invite;

import net.hollowcube.mapmaker.bridge.ServerBridge;
import net.hollowcube.mapmaker.bridge.ServerBridge.JoinMapState;
import net.hollowcube.mapmaker.invite.types.InviteType;
import net.hollowcube.mapmaker.invite.types.MapInvite;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.player.JoinMapRequest;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.player.SessionService;
import net.hollowcube.mapmaker.session.MapPresence;
import net.hollowcube.mapmaker.session.SessionManager;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerInviteServiceImpl extends AbstractHttpService implements PlayerInviteService {

    private final String url;
    private final MapService mapService;
    private final PlayerService playerService;
    private final SessionService sessionService;
    private final SessionManager sessionManager;
    private final ServerBridge serverBridge;

    public PlayerInviteServiceImpl(String url, MapService mapService, PlayerService playerService,
                                   SessionService sessionService, SessionManager sessionManager,
                                   ServerBridge serverBridge) {
        this.url = String.format("%s/v2/internal/invites", url);
        this.mapService = mapService;
        this.playerService = playerService;
        this.sessionService = sessionService;
        this.sessionManager = sessionManager;
        this.serverBridge = serverBridge;
    }

    @Override
    public void join(@NotNull Player sender, @NotNull Player target) {
        // TODO: Figure out if this is even used and if so, what it's for
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void registerInvite(@NotNull Player sender, @NotNull String targetId) {
        var senderMap = this.getCurrentMap(sender);
        if (senderMap == null) {
            sender.sendMessage(Component.translatable("map.invite.no_map"));
            return;
        }

        var senderMapName = Component.text(senderMap.name());
        var targetDisplayName = this.playerService.getPlayerDisplayName2(targetId);
        if (!doesPlayerOwnMap(sender, senderMap) && !senderMap.isPublished()) {
            sender.sendMessage(Component.translatable("map.invite.no_permission", targetDisplayName, senderMapName));
            return;
        }

        var targetMap = MiscFunctionality.getCurrentMap(this.sessionManager, this.mapService, targetId);
        if (targetMap == senderMap) {
            sender.sendMessage(Component.translatable("map.invite.same_map"));
            return;
        }

        var body = GSON.toJson(new MapInvite(InviteType.INVITE, sender, targetId, senderMap));
        var request = HttpRequest.newBuilder()
                .method("POST", HttpRequest.BodyPublishers.ofString(body))
                .uri(URI.create(this.url + "/map/invite"))
                .build();
        var response = doRequest(request, HttpResponse.BodyHandlers.ofString());

        switch (response.statusCode()) {
            case 200 -> {
                String translateString = "map." + (senderMap.isPublished() ? "play" : "build") + ".invite.sent";
                sender.sendMessage(Component.translatable(translateString, targetDisplayName, senderMapName));
            }
            case 409 -> sender.sendMessage(Component.translatable("map.invite.already_present", targetDisplayName));
            default -> throw new InternalError("Failed to register invite: " + response.body());
        }
    }

    @Override
    public void registerRequest(@NotNull Player sender, @NotNull String targetId) {
        var targetDisplayName = this.playerService.getPlayerDisplayName2(targetId);

        var targetMap = MiscFunctionality.getCurrentMap(this.sessionManager, this.mapService, targetId);
        if (targetMap == null) {
            sender.sendMessage(Component.translatable("map.play.request.cant_send", targetDisplayName));
            return;
        }

        var senderMap = MiscFunctionality.getCurrentMap(this.sessionManager, this.mapService, sender);
        if (senderMap == targetMap) {
            sender.sendMessage(Component.translatable("map.request.same_map", targetDisplayName));
            return;
        }

        var body = GSON.toJson(new MapInvite(InviteType.REQUEST, sender, targetId, targetMap));
        var request = HttpRequest.newBuilder()
                .method("POST", HttpRequest.BodyPublishers.ofString(body))
                .uri(URI.create(this.url + "/map/request"))
                .build();
        var response = doRequest(request, HttpResponse.BodyHandlers.ofString());

        switch (response.statusCode()) {
            case 200 -> {
                String translateString = "map." + (targetMap.isPublished() ? "play" : "build") + ".request.sent";
                sender.sendMessage(Component.translatable(translateString, targetDisplayName, Component.text(targetMap.name())));
            }
            case 409 -> sender.sendMessage(Component.translatable("map.request.already_present", targetDisplayName));
            default -> throw new InternalError("Failed to register request: " + response.body());
        }
    }

    @Override
    public void accept(@NotNull Player sender, @Nullable String targetId) {
        this.acceptOrReject(sender, targetId, true);
    }

    @Override
    public void reject(@NotNull Player sender, @Nullable String targetId) {
        this.acceptOrReject(sender, targetId, false);
    }

    private void acceptOrReject(@NotNull Player sender, @Nullable String targetId, boolean accept) {
        String acceptReject = accept ? "accept" : "reject";

        Map<Object, Object> body = new HashMap<>();
        body.put("senderId", sender.getUuid().toString());
        if (targetId != null) {
            body.put("recipientId", targetId);
        }

        var request = HttpRequest.newBuilder()
                .method("POST", HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                .uri(URI.create(this.url + "/map/" + acceptReject))
                .build();
        var response = doRequest(request, HttpResponse.BodyHandlers.ofString());

        var statusCode = response.statusCode();
        switch (statusCode) {
            case 200 -> {
                var invite = GSON.fromJson(response.body(), MapInvite.class);
                var inviteMap = this.mapService.getMap(invite.senderId(), invite.mapId());

                boolean isInvite = invite.inviteType() == InviteType.INVITE;
                String inviteRequest = isInvite ? "invite" : "request";
                String playBuild = inviteMap.isPublished() ? "play" : "build";

                var targetDisplayName = this.playerService.getPlayerDisplayName2(invite.recipientId());
                String translateString = "map." + playBuild + "." + inviteRequest + "." + acceptReject;
                sender.sendMessage(Component.translatable(translateString, targetDisplayName, Component.text(inviteMap.name())));

                if (accept) {
                    this.joinMap(sender, invite.senderId(), inviteMap, isInvite);
                }
            }
            case 400 -> {
                InviteError error = GSON.fromJson(response.body(), InviteError.class);
                processAcceptOrRejectError(error, sender, acceptReject);
            }
            default -> throw new InternalError("Failed to " + acceptReject + ": " + response.body());
        }
    }

    private MapData getCurrentMap(@NotNull Player player) {
        return MiscFunctionality.getCurrentMap(this.sessionManager, this.mapService, player);
    }

    private void joinMap(@NotNull Player sender, @NotNull String requestSenderId, @NotNull MapData map, boolean isInvite) {
        if (isInvite) {
            this.joinSenderToTargetMap(sender, map);
        } else {
            // For requests, we use the invite sender's ID, different from the sender of the accept's ID
            this.joinTargetToSenderMap(sender, requestSenderId, map);
        }
    }

    private void joinSenderToTargetMap(@NotNull Player sender, @NotNull MapData map) {
        this.serverBridge.joinMap(sender, map.id(), map.isPublished() ? JoinMapState.PLAYING : JoinMapState.EDITING);
    }

    private void joinTargetToSenderMap(@NotNull Player sender, @NotNull String requestSenderId, @NotNull MapData map) {
        var presence = map.isPublished() ? MapPresence.STATE_PLAYING : MapPresence.STATE_EDITING;
        var response = this.sessionService.joinMapV2(new JoinMapRequest(requestSenderId, map.id(), presence));

        TransferOtherRequest request = new TransferOtherRequest(UUID.fromString(requestSenderId), response.serverClusterIp());
        sender.sendPluginMessage("mapmaker:transfer_other", GSON.toJson(request).getBytes());
    }

    private record TransferOtherRequest(@NotNull UUID targetPlayerId, @NotNull String serverName) {
    }

    private static void processAcceptOrRejectError(@NotNull InviteError error, @NotNull Player sender, String acceptReject) {
        String translationKey = switch (error.errorCode()) {
            case ErrorCodes.INVITE_NOT_FOUND, ErrorCodes.REQUEST_NOT_FOUND, ErrorCodes.NO_INVITES_OR_REQUESTS ->
                    "map.invite_and_request.cant_" + acceptReject;
            case ErrorCodes.MULTIPLE_INVITES -> "map.invite.multiple";
            case ErrorCodes.MULTIPLE_REQUESTS -> "map.request.multiple";
            case ErrorCodes.INVITE_AND_REQUEST -> "map.invite_and_request.both";
            case ErrorCodes.INVITE_SENDER_LEFT_MAP -> "map.invite.sender_left_map";
            case ErrorCodes.INVITE_SENDER_OFFLINE -> "map.invite.sender_offline";
            case ErrorCodes.REQUEST_TARGET_LEFT_MAP -> "map.request.target_left_map";
            case ErrorCodes.REQUEST_TARGET_OFFLINE -> "map.request.target_offline";
            default -> throw new IllegalStateException("Unexpected error code: " + error.errorCode() + " (message: " + error.errorText() + ")");
        };
        sender.sendMessage(Component.translatable(translationKey));
    }

    private static boolean doesPlayerOwnMap(@NotNull Player player, @NotNull MapData map) {
        return player.getUuid().equals(UUID.fromString(map.owner()));
    }

    private record InviteError(int errorCode, String errorText) {
    }

    private static final class ErrorCodes {

        static final int INVITE_NOT_FOUND = 0;
        static final int REQUEST_NOT_FOUND = 1;
        static final int MULTIPLE_INVITES = 2;
        static final int MULTIPLE_REQUESTS = 3;
        static final int INVITE_AND_REQUEST = 4;
        static final int INVITE_SENDER_LEFT_MAP = 5;
        static final int INVITE_SENDER_OFFLINE = 6;
        static final int REQUEST_TARGET_LEFT_MAP = 7;
        static final int REQUEST_TARGET_OFFLINE = 8;
        static final int NO_INVITES_OR_REQUESTS = 9;
    }
}
