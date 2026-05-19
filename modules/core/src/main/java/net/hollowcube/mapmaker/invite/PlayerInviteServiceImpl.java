package net.hollowcube.mapmaker.invite;

import io.opentelemetry.api.OpenTelemetry;
import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.invite.types.InviteType;
import net.hollowcube.mapmaker.invite.types.MapInvite;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapVerification;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.player.DisplayName;
import net.hollowcube.mapmaker.player.Permission;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.session.Presence;
import net.hollowcube.mapmaker.session.SessionManager;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Blocking;
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
    private final ApiClient api;
    private final SessionManager sessionManager;
    private final ServerBridge bridge;

    public PlayerInviteServiceImpl(
        @NotNull OpenTelemetry otel,
        @NotNull String url,
        @NotNull ApiClient api,
        @NotNull SessionManager sessionManager,
        @NotNull ServerBridge bridge
    ) {
        super(otel);
        this.url = String.format("%s/v3/internal/invites", url);
        this.api = api;
        this.sessionManager = sessionManager;
        this.bridge = bridge;
    }

    @Override
    @Blocking
    public void join(@NotNull Player sender, @NotNull String targetId) {
        var senderSession = sessionManager.getSession(sender.getUuid().toString());
        if (senderSession == null) {
            // Yeah this is very bad
            sender.sendMessage(Component.translatable("command.generic.sanity_check_failed"));
            return;
        }

        var targetDisplayName = api.players.getDisplayName(targetId);
        var targetSession = sessionManager.getSession(targetId);
        if (targetSession == null) {
            sender.sendMessage(Component.translatable("map.join.target_offline", targetDisplayName));
            return;
        }

        var targetPresence = targetSession.presence();
        if (targetPresence.type().equals(Presence.TYPE_MAPMAKER_HUB)) {
            sender.sendMessage(Component.translatable("map.join.target_not_in_map", targetDisplayName));
            return;
        }

        if (senderSession.presence().mapId().equals(targetPresence.mapId())) {
            sender.sendMessage(Component.translatable("map.join.already_on_map", targetDisplayName));
            return;
        }

        var targetMap = api.maps.get(targetPresence.mapId());
        var senderData = PlayerData.fromPlayer(sender);
        // TODO: When trusted members exist for maps, check if the player is a trusted member
        if (!targetMap.isPublished() && !senderData.has(Permission.GENERIC_STAFF)) {
            sender.sendMessage(Component.translatable("map.join.no_permission", targetDisplayName));
            return;
        }

        var joinState = targetMap.isPublished() || targetPresence.state().equals("playing") ? ServerBridge.JoinMapState.PLAYING : ServerBridge.JoinMapState.EDITING;
        bridge.joinMap(sender, targetMap.id(), joinState, "join_command");
    }

    @Override
    public void registerInvite(@NotNull Player sender, @NotNull String targetId) {
        var senderMap = this.getCurrentMap(sender);
        if (senderMap == null) {
            sender.sendMessage(Component.translatable("map.invite.no_map"));
            return;
        }
        if (!senderMap.isPublished() && senderMap.verification() != MapVerification.UNVERIFIED) {
            sender.sendMessage(Component.translatable("map.invite.verifying"));
            return;
        }

        var senderMapName = Component.text(senderMap.name());
        var targetDisplayName = api.players.getDisplayName(targetId);
        if (!doesPlayerOwnMap(sender, senderMap) && !senderMap.isPublished()) {
            sender.sendMessage(Component.translatable("map.invite.no_permission", targetDisplayName, senderMapName));
            return;
        }

        var targetSession = sessionManager.getSession(targetId);
        if (targetSession == null) {
            // This should've been checked outside this method
            return;
        }

        if (senderMap.id().equals(targetSession.presence().mapId())) {
            sender.sendMessage(Component.translatable("map.invite.same_map"));
            return;
        }

        var body = GSON.toJson(new MapInvite(InviteType.INVITE, sender, targetId, senderMap));
        var request = HttpRequest.newBuilder()
            .method("POST", HttpRequest.BodyPublishers.ofString(body))
            .uri(URI.create(this.url + "/map/invite"));
        var response = doRequest("register_invite", request, HttpResponse.BodyHandlers.ofString());

        switch (response.statusCode()) {
            case 200 -> {
                String translateString = "map." + (senderMap.isPublished() ? "play" : "build") + ".invite.sent";
                sender.sendMessage(Component.translatable(translateString, targetDisplayName, senderMapName));
            }
            case 409 -> {
                SessionError error = GSON.fromJson(response.body(), SessionError.class);
                processRegisterError(sender, targetDisplayName, error, true);
            }
            default -> throw new InternalError("Failed to register invite: " + response.body());
        }
    }

    @Override
    public void registerRequest(@NotNull Player sender, @NotNull String targetId) {
        var targetDisplayName = api.players.getDisplayName(targetId);

        var targetSession = sessionManager.getSession(targetId);
        if (targetSession == null) {
            // This should've been checked outside this method
            return;
        }

        var targetPresence = targetSession.presence();
        if (targetPresence == null || targetPresence.type().equals(Presence.TYPE_MAPMAKER_HUB)) {
            sender.sendMessage(Component.translatable("map.play.request.cant_send", targetDisplayName));
            return;
        }

        var senderPresence = sessionManager.getSession(PlayerData.fromPlayer(sender).id()).presence();
        if (senderPresence.mapId().equals(targetPresence.mapId())) {
            sender.sendMessage(Component.translatable("map.request.same_map", targetDisplayName));
            return;
        }

        var targetMap = api.maps.get(targetPresence.mapId());

        var body = GSON.toJson(new MapInvite(InviteType.REQUEST, sender, targetId, targetMap));
        var request = HttpRequest.newBuilder()
            .method("POST", HttpRequest.BodyPublishers.ofString(body))
            .uri(URI.create(this.url + "/map/request"));
        var response = doRequest("register_request", request, HttpResponse.BodyHandlers.ofString());

        switch (response.statusCode()) {
            case 200 -> {
                String translateString = "map." + (targetMap.isPublished() ? "play" : "build") + ".request.sent";
                sender.sendMessage(Component.translatable(translateString, targetDisplayName, Component.text(targetMap.name())));
            }
            case 409 -> {
                SessionError error = GSON.fromJson(response.body(), SessionError.class);
                processRegisterError(sender, targetDisplayName, error, false);
            }
            default -> throw new InternalError("Failed to register request: " + response.body());
        }
    }

    private static void processRegisterError(@NotNull Player sender, @NotNull DisplayName targetDisplayName,
                                             @NotNull SessionError error, boolean invite) {
        var translationString = switch (error.code()) {
            case "invite_exists" -> "map.invite.already_present";
            case "request_exists" -> "map.request.already_present";
            case "already_on_map" -> invite ? "map.invite.same_map" : "map.request.same_map";
            default -> throw new IllegalStateException("Unexpected error (" + error.code() + "): " + error.message());
        };
        sender.sendMessage(Component.translatable(translationString, targetDisplayName));
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
            body.put("targetId", targetId);
        }

        var request = HttpRequest.newBuilder()
            .method("POST", HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
            .uri(URI.create(this.url + "/map/" + acceptReject));
        var response = doRequest("accept_or_reject", request, HttpResponse.BodyHandlers.ofString());

        var statusCode = response.statusCode();
        switch (statusCode) {
            case 200 -> {
                var invite = GSON.fromJson(response.body(), MapInvite.class);
                var inviteMap = api.maps.get(invite.mapId());

                boolean isInvite = invite.inviteType() == InviteType.INVITE;
                String inviteRequest = isInvite ? "invite" : "request";
                String playBuild = inviteMap.isPublished() ? "play" : "build";

                var senderDisplayName = api.players.getDisplayName(invite.senderId());

                String translateString = "map." + playBuild + "." + inviteRequest + "." + acceptReject;
                sender.sendMessage(Component.translatable(translateString, senderDisplayName, Component.text(inviteMap.name())));
            }
            case 400 -> {
                InviteError error = GSON.fromJson(response.body(), InviteError.class);
                processAcceptOrRejectError(error, sender, acceptReject);
            }
            default -> throw new InternalError("Failed to " + acceptReject + ": " + response.body());
        }
    }

    private @Nullable MapData getCurrentMap(@NotNull Player player) {
        return MiscFunctionality.getCurrentMap(sessionManager, api.maps, player);
    }

    private static void processAcceptOrRejectError(@NotNull InviteError error, @NotNull Player sender, String acceptReject) {
        String translationKey = switch (error.errorCode()) {
            case ErrorCodes.INVITE_NOT_FOUND, ErrorCodes.REQUEST_NOT_FOUND, ErrorCodes.NO_INVITES_OR_REQUESTS ->
                "map.invite_and_request.cant_" + acceptReject;
            case ErrorCodes.INVITE_SENDER_LEFT_MAP -> "map.invite.left_map";
            case ErrorCodes.INVITE_SENDER_OFFLINE -> "map.invite.offline";
            case ErrorCodes.REQUEST_TARGET_LEFT_MAP -> "map.request.left_map";
            case ErrorCodes.REQUEST_TARGET_OFFLINE -> "map.request.offline";
            default ->
                throw new IllegalStateException("Unexpected error code: " + error.errorCode() + " (message: " + error.errorText() + ")");
        };
        sender.sendMessage(Component.translatable(translationKey));
    }

    private static boolean doesPlayerOwnMap(@NotNull Player player, @NotNull MapData map) {
        return player.getUuid().equals(UUID.fromString(map.owner()));
    }

    @RuntimeGson
    public record InviteError(int errorCode, @NotNull String errorText) {
    }

    @RuntimeGson
    public record SessionError(@NotNull String code, @NotNull String message) {
    }

    private static final class ErrorCodes {

        static final int INVITE_NOT_FOUND = 0;
        static final int REQUEST_NOT_FOUND = 1;
        static final int INVITE_ALREADY_EXISTS = 2;
        static final int REQUEST_ALREADY_EXISTS = 3;
        static final int ALREADY_ON_MAP = 4;
        static final int INVITE_SENDER_LEFT_MAP = 5;
        static final int INVITE_SENDER_OFFLINE = 6;
        static final int REQUEST_TARGET_LEFT_MAP = 7;
        static final int REQUEST_TARGET_OFFLINE = 8;
        static final int NO_INVITES_OR_REQUESTS = 9;
    }
}
