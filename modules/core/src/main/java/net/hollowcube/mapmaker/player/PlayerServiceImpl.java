package net.hollowcube.mapmaker.player;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.opentelemetry.api.OpenTelemetry;
import io.prometheus.client.Summary;
import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.player.requests.CreatePlayerNotificationRequest;
import net.hollowcube.mapmaker.player.responses.*;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerServiceImpl extends AbstractHttpService implements PlayerService {
    private static final Summary remoteFetchDisplayNameTime = Summary.build()
        .namespace("mapmaker").name("remote_fetch_display_name_time_seconds")
        .help("Summary of the time it takes to fetch a player's display name from the remote service")
        .register();

    private static final System.Logger logger = System.getLogger(PlayerServiceImpl.class.getName());

    private final String url;

    public PlayerServiceImpl(@Nullable OpenTelemetry otel, @NotNull String url) {
        super(otel);
        this.url = String.format("%s/v2/internal", url);
    }

    @Override
    public void updatePlayerData(@NotNull String id, @NotNull PlayerDataUpdateRequest update) {
        logger.log(System.Logger.Level.INFO, "update playerdata for {0}", id);
        var reqBody = GSON.toJson(update);
        var req = HttpRequest.newBuilder()
            .method("PATCH", HttpRequest.BodyPublishers.ofString(reqBody))
            .uri(URI.create(url + "/players/" + id));
        var res = doRequest("updatePlayerData", req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200)
            throw new SessionService.InternalError(
                "Failed to update session (" + res.statusCode() + "): " + res.body());
    }

    @Override
    public @NotNull Set<String> getUnlockedCosmetics(@NotNull String playerId) {
        var req = HttpRequest.newBuilder()
            .uri(URI.create(url + "/players/" + playerId + "/cosmetics"));
        var res = doRequest("getUnlockedCosmetics", req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> GSON.fromJson(res.body(), new TypeToken<Set<String>>() {
            }.getType());
            case 404 -> Set.of();
            default -> throw new SessionService.InternalError(
                "Failed to get unlocked cosmetics (" + res.statusCode() + "): " + res.body());
        };
    }

    @Override
    public void buyCosmetic(
        @NotNull String id, @NotNull Cosmetic cosmetic, @Nullable Integer coins, @Nullable Integer cubits,
        @Nullable JsonObject items
    ) {
        logger.log(System.Logger.Level.INFO, "buy cosmetic for {0}: {1}", id, cosmetic.path());
        var reqBodyData = new JsonObject();
        reqBodyData.addProperty("cosmeticId", cosmetic.path());
        if (coins != null) reqBodyData.addProperty("coins", coins);
        if (cubits != null) reqBodyData.addProperty("cubits", cubits);
        if (items != null) reqBodyData.add("items", items);
        var req = HttpRequest.newBuilder()
            .method("POST", HttpRequest.BodyPublishers.ofString(GSON.toJson(reqBodyData)))
            .uri(URI.create(url + "/players/" + id + "/cosmetics"));
        var res = doRequest("buyCosmetic", req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200)
            throw new SessionService.InternalError("Failed to buy cosmetic (" + res.statusCode() + "): " + res.body());
    }

    @Override
    public void buyUpgrade(@NotNull String playerId, @NotNull String upgradeId, int cubits, @NotNull JsonObject meta) {
        logger.log(System.Logger.Level.INFO, "buy upgrade for {0}: {1}", playerId, upgradeId);
        var reqBodyData = new JsonObject();
        reqBodyData.addProperty("upgradeId", upgradeId);
        reqBodyData.addProperty("cubits", cubits);
        reqBodyData.add("meta", meta);
        var req = HttpRequest.newBuilder()
            .method("POST", HttpRequest.BodyPublishers.ofString(GSON.toJson(reqBodyData)))
            .uri(URI.create(url + "/players/" + playerId + "/upgrades"));
        var res = doRequest("buyUpgrade", req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200)
            throw new SessionService.InternalError("Failed to buy upgrade (" + res.statusCode() + "): " + res.body());
    }

    @Override
    public @NotNull JsonObject getPlayerBackpack(@NotNull String id) {
        var req = HttpRequest.newBuilder()
            .uri(URI.create(url + "/players/" + id + "/backpack"));
        var res = doRequest("getPlayerBackpack", req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> GSON.fromJson(res.body(), JsonObject.class);
            case 404 -> new JsonObject();
            default -> throw new SessionService.InternalError(
                "Failed to get player backpack (" + res.statusCode() + "): " + res.body());
        };
    }

    @Override
    public @NotNull String getPlayerId(@NotNull String idOrUsername) {
        var req = HttpRequest.newBuilder()
            .uri(URI.create(url + "/players/" + idOrUsername + "/id"));
        var res = doRequest("getPlayerId", req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> res.body();
            case 404 -> throw new NotFoundError();
            default -> throw new SessionService.InternalError(
                "Failed to get player id (" + res.statusCode() + "): " + res.body());
        };
    }

    @Override
    public @NotNull PlayerData getPlayerData(@NotNull String id) {
        var req = HttpRequest.newBuilder()
            .uri(URI.create(url + "/players/" + id));
        var res = doRequest("getPlayerData", req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> GSON.fromJson(res.body(), PlayerData.class);
            case 404 -> throw new NotFoundError();
            default -> throw new SessionService.InternalError(
                "Failed to get player data (" + res.statusCode() + "): " + res.body());
        };
    }

    @Override
    public @NotNull DisplayName getPlayerDisplayName2(@NotNull String id) {
        // If the player is online we have an up-to-date display name anyway
        var player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(UUID.fromString(id));
        if (player != null) {
            return PlayerData.fromPlayer(player).displayName2();
        }

        //todo probably should have some basic cache here

        try (var $ = remoteFetchDisplayNameTime.startTimer()) {
            var req = HttpRequest.newBuilder()
                .uri(URI.create(url + "/players/" + id + "/displayname?v=2"));
            var res = doRequest("getPlayerDisplayName2", req, HttpResponse.BodyHandlers.ofString());
            return switch (res.statusCode()) {
                case 200 -> GSON.fromJson(res.body(), DisplayName.class);
                case 404 -> new DisplayName(List.of(new DisplayName.Part("username", "!error!", null)));
                default -> throw new SessionService.InternalError(
                    "Failed to get player display name (" + res.statusCode() + "): " + res.body());
            };
        }
    }

    @Override
    public @NotNull TabCompleteResponse getUsernameTabCompletions(@NotNull String query) {
        if (query.isEmpty()) return new TabCompleteResponse(List.of());

        var reqBody = GSON.toJson(Map.of("query", query));
        var req = HttpRequest.newBuilder()
            .method("POST", HttpRequest.BodyPublishers.ofString(reqBody))
            .uri(URI.create(url + "/tab_complete"));
        var res = doRequest("getUsernameTabCompletions", req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200)
            throw new InternalError("Failed to get tab completions (" + res.statusCode() + "): " + res.body());
        return GSON.fromJson(res.body(), TabCompleteResponse.class);
    }

    @Override
    public @NotNull CreateCheckoutLinkResponse createCheckoutLink(
        @NotNull String source, @NotNull String username, @NotNull String product) {
        var reqBody = GSON.toJson(Map.of(
            "username", username,
            "package", product
        ));
        var req = HttpRequest.newBuilder()
            .method("POST", HttpRequest.BodyPublishers.ofString(reqBody))
            .uri(URI.create(url + "/tebex/checkout"));
        var res = doRequest("createCheckoutLink", req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200)
            throw new InternalError("Failed to create checkout url (" + res.statusCode() + "): " + res.body());
        return GSON.fromJson(res.body(), CreateCheckoutLinkResponse.class);
    }

    @Override
    public @Nullable HypercubeStatus getHypercubeStatus(@NotNull String playerId) {
        var req = HttpRequest.newBuilder()
            .method("GET", HttpRequest.BodyPublishers.noBody())
            .uri(URI.create(url + "/players/" + playerId + "/hypercube"));
        var res = doRequest("getHypercubeStatus", req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> GSON.fromJson(res.body(), HypercubeStatus.class);
            case 404 -> null;
            default ->
                throw new InternalError("Failed to get hypercube status url (" + res.statusCode() + "): " + res.body());
        };
    }

    @Override
    public @NotNull LinkResult attemptVerify(@NotNull String playerId, @NotNull String secret) {
        var body = GSON.toJson(Map.of(
            "verificationType", "discord",
            "playerId", playerId,
            "userSecret", secret
        ));
        var req = HttpRequest.newBuilder()
            .uri(URI.create(url + "/players/verify/attempt"))
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        var response = doRequest("attemptVerify", req, HttpResponse.BodyHandlers.ofString());

        return switch (response.statusCode()) {
            case 200 -> LinkResult.SUCCESS;
            case 404 -> LinkResult.INVALID_SECRET;
            case 204 -> LinkResult.EXPIRED_SECRET;
            case 409 -> LinkResult.ALREADY_LINKED;
            default -> throw new InternalError(
                "Verification attempt failed: (" + response.statusCode() + "): " + response.body());
        };
    }

    @Override
    public @NotNull TotpResult checkTotp(@NotNull String playerId, @Nullable String code) {
        var req = HttpRequest.newBuilder()
            .uri(URI.create(url + "/players/" + playerId + "/totp" + (code != null ? "?code=" + code : "")))
            .GET();

        var response = doRequest("checkTotp", req, HttpResponse.BodyHandlers.ofString());

        return switch (response.statusCode()) {
            case 200 -> TotpResult.SUCCESS;
            case 400 -> TotpResult.INVALID_FORMAT;
            case 401 -> TotpResult.INVALID_CODE;
            case 404 -> TotpResult.NOT_ENABLED;
            default ->
                throw new InternalError("Totp check failed: (" + response.statusCode() + "): " + response.body());
        };
    }

    @Override
    public @NotNull TotpResult removeTotp(@NotNull String playerId) {
        var req = HttpRequest.newBuilder()
            .uri(URI.create(url + "/players/" + playerId + "/totp"))
            .DELETE();

        var response = doRequest("removeTotp", req, HttpResponse.BodyHandlers.ofString());

        return switch (response.statusCode()) {
            case 204 -> TotpResult.SUCCESS;
            case 404 -> TotpResult.NOT_ENABLED;
            default ->
                throw new InternalError("Failed to remove totp: (" + response.statusCode() + "): " + response.body());
        };
    }

    @Override
    public @Nullable TotpSetupResponse beginTotpSetup(@NotNull String playerId) {
        var req = HttpRequest.newBuilder()
            .uri(URI.create(url + "/players/" + playerId + "/totp/setup"))
            .POST(HttpRequest.BodyPublishers.noBody());

        var response = doRequest("beginTotpSetup", req, HttpResponse.BodyHandlers.ofString());

        return switch (response.statusCode()) {
            case 201 -> GSON.fromJson(response.body(), TotpSetupResponse.class);
            case 404 -> throw new NotFoundError();
            case 409 -> null;
            default -> throw new InternalError(
                "Failed to begin totp setup: (" + response.statusCode() + "): " + response.body());
        };
    }

    @Override
    public @NotNull TotpResult completeTotpSetup(@NotNull String playerId, @NotNull String code) {
        var body = GSON.toJson(Map.of(
            "code", code
        ));
        var req = HttpRequest.newBuilder()
            .uri(URI.create(url + "/players/" + playerId + "/totp/setup"))
            .method("PATCH", HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));

        var response = doRequest("completeTotpSetup", req, HttpResponse.BodyHandlers.ofString());

        return switch (response.statusCode()) {
            case 200 -> TotpResult.SUCCESS;
            case 400 -> TotpResult.INVALID_FORMAT;
            case 401 -> TotpResult.INVALID_CODE;
            case 404 -> TotpResult.NOT_ENABLED;
            case 409 -> TotpResult.ALREADY_ENABLED;
            default -> throw new InternalError(
                "Failed to complete totp setup: (" + response.statusCode() + "): " + response.body());
        };
    }

    @Override
    public @NotNull List<PlayerAlts.Alt> getAlts(@NotNull String playerId) {
        var req = HttpRequest.newBuilder().uri(URI.create(url + "/players/" + playerId + "/alts")).GET();
        var res = doRequest("getAlts", req, HttpResponse.BodyHandlers.ofString());

        return switch (res.statusCode()) {
            case 200 -> GSON.fromJson(res.body(), PlayerAlts.class).results();
            case 404 -> List.of();
            default ->
                throw new SessionService.InternalError("Failed to get alts (" + res.statusCode() + "): " + res.body());
        };
    }

    @Override
    public @NotNull Page<PlayerFriend> getPlayerFriends(@NotNull String playerId, @NotNull Pageable pageable) {
        var req = HttpRequest.newBuilder().uri(URI.create(url + "/players/%s/friends?page=%s&pageSize=%s".formatted(playerId, pageable.page(), pageable.pageSize()))).GET();
        var res = doRequest("getPlayerFriends", req, HttpResponse.BodyHandlers.ofString());

        // todo correct type
        return switch (res.statusCode()) {
            case 200 -> Page.fromJson(res.body(), PlayerFriend.class);
            default -> throw new InternalError("Failed to get friends (" + res.statusCode() + "): " + res.body());
        };
    }

    @Override
    public void removeFriend(@NotNull String playerId, @NotNull String targetId) {
        var req = HttpRequest.newBuilder()
            .uri(URI.create(url + "/players/" + playerId + "/friends/" + targetId))
            .DELETE();
        var res = doRequest("removeFriend", req, HttpResponse.BodyHandlers.discarding());

        switch (res.statusCode()) {
            case 204 -> {
            } // successful
            case 404 -> throw new NotFoundError();
            default -> throw new InternalError("Failed to remove friend (" + res.statusCode() + "): " + res.body());
        }
    }

    @Override
    public @NotNull Page<FriendRequest> getFriendRequests(@NotNull String playerId, boolean incoming, @NotNull Pageable pageable) {
        String direction = incoming ? "incoming" : "outgoing";
        var req = HttpRequest.newBuilder()
            .uri(URI.create(url + "/players/%s/friendRequests?direction=%s&page=%s&pageSize=%s".formatted(playerId, direction, pageable.page(), pageable.pageSize())))
            .GET();
        var res = doRequest("getFriendRequests", req, HttpResponse.BodyHandlers.ofString());

        // todo correct type
        return switch (res.statusCode()) {
            case 200 -> Page.fromJson(res.body(), FriendRequest.class);
            default ->
                throw new InternalError("Failed to get friend requests (" + res.statusCode() + "): " + res.body());
        };
    }

    @Override
    public @NotNull SendFriendRequestResult sendFriendRequest(@NotNull String playerId, @NotNull String targetId) {
        var req = HttpRequest.newBuilder()
            .uri(URI.create(url + "/players/" + playerId + "/friendRequests/" + targetId))
            .POST(HttpRequest.BodyPublishers.noBody());
        var res = doRequest("sendFriendRequest", req, HttpResponse.BodyHandlers.ofString());

        return switch (res.statusCode()) {
            case 201 ->
                new SendFriendRequestResult(GSON.fromJson(res.body(), SendFriendRequestResponse.class).isRequest(),
                                            null, null);
            case 401 -> {
                SendFriendRequestResult.LimitError error = GSON.fromJson(res.body(), SendFriendRequestResult.LimitError.class);
                yield new SendFriendRequestResult(false, null, error);
            }
            case 409 -> {
                PlayerServiceError error = GSON.fromJson(res.body(), PlayerServiceError.class);
                yield new SendFriendRequestResult(false, error, null);
            }
            default ->
                throw new InternalError("Failed to send friend request (" + res.statusCode() + "): " + res.body());
        };
    }

    @Override
    public @NotNull FriendRequest deleteFriendRequest(
        @NotNull String playerId, @NotNull String targetId, boolean bidirectional) {
        var req = HttpRequest.newBuilder()
            .uri(URI.create(
                url + "/players/" + playerId + "/friendRequests/" + targetId + "?bidirectional=" + bidirectional))
            .DELETE();
        var res = doRequest("deleteFriendRequest", req, HttpResponse.BodyHandlers.ofString());

        return switch (res.statusCode()) {
            case 200 -> GSON.fromJson(res.body(), FriendRequest.class);
            case 404 -> throw new NotFoundError();
            default ->
                throw new InternalError("Failed to delete friend request (" + res.statusCode() + "): " + res.body());
        };
    }

    @Override
    public void blockPlayer(@NotNull String playerId, @NotNull String targetId) {
        var req = HttpRequest.newBuilder()
            .uri(URI.create(url + "/players/" + playerId + "/blocks/" + targetId))
            .POST(HttpRequest.BodyPublishers.noBody());
        var res = doRequest("blockPlayer", req, HttpResponse.BodyHandlers.ofString());

        switch (res.statusCode()) {
            case 201 -> {
            } // do nothing, successful
            case 400 -> throw new BadRequestError();
            case 409 -> throw new AlreadyExistsError();
            default -> throw new InternalError("Failed to block player (" + res.statusCode() + "): " + res.body());
        }
    }

    @Override
    public @NotNull Page<BlockedPlayer> getBlockedPlayers(@NotNull String playerId, @NotNull Pageable pageable) {
        var req = HttpRequest.newBuilder().uri(URI.create(url + "/players/%S/blocks?page=%s&pageSize=%s".formatted(playerId, pageable.page(), pageable.pageSize()))).GET();
        var res = doRequest("getBlockedPlayers", req, HttpResponse.BodyHandlers.ofString());

        return switch (res.statusCode()) {
            case 200 -> Page.fromJson(res.body(), BlockedPlayer.class);
            default ->
                throw new InternalError("Failed to get blocked players (" + res.statusCode() + "): " + res.body());
        };
    }

    @Override
    public void unblockPlayer(@NotNull String playerId, @NotNull String targetId) {
        var req = HttpRequest.newBuilder()
            .uri(URI.create(url + "/players/" + playerId + "/blocks/" + targetId))
            .DELETE();
        var res = doRequest("unblockPlayer", req, HttpResponse.BodyHandlers.discarding());

        switch (res.statusCode()) {
            case 204 -> {
            } // do nothing, successful
            case 404 -> throw new NotFoundError();
            default -> throw new InternalError("Failed to unblock player (" + res.statusCode() + "): " + res.body());
        }
    }

    @Override
    public @NotNull PlayerNotificationResponse getNotifications(@NotNull String playerId, int page, boolean unread) {
        var req = setupGet(url("%s/players/%s/notifications?page=%d&unread=%s", url, playerId, page, unread));
        var res = doRequest("getNotifications", req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> GSON.fromJson(res.body(), PlayerNotificationResponse.class);
            case 400 -> throw new IllegalArgumentException("Invalid page number");
            default ->
                    throw new SessionService.InternalError("Failed to get notifications (" + res.statusCode() + "): " + res.body());
        };
    }

    @Override
    public void deleteNotification(@NotNull String playerId, @NotNull String notificationId) {
        var req = setupDelete(url("%s/players/%s/notifications?notificationId=%s", url, playerId, notificationId));
        var res = doRequest("deleteNotification", req, HttpResponse.BodyHandlers.ofString());
        switch (res.statusCode()) {
            case 200 -> {}
            case 404 -> throw new NotFoundError();
            default ->
                    throw new SessionService.InternalError("Failed to delete notification (" + res.statusCode() + "): " + res.body());
        }
    }

    @Override
    public void markNotificationRead(@NotNull String playerId, @NotNull String notificationId, boolean read) {
        var data = Map.of("read", read);
        var req = setupPatch(url("%s/players/%s/notifications?notificationId=%s", url, playerId, notificationId), GSON.toJson(data));
        var res = doRequest("markNotificationRead", req, HttpResponse.BodyHandlers.ofString());
        switch (res.statusCode()) {
            case 200 -> {}
            case 404 -> throw new NotFoundError();
            default ->
                    throw new SessionService.InternalError("Failed to mark notification read (" + res.statusCode() + "): " + res.body());
        }
    }

    @Override
    public void createNotification(
        @NotNull String playerId,
        @NotNull String type,
        @NotNull String key,
        @Nullable JsonObject data,
        @Nullable Integer expiresInSeconds,
        boolean replaceUnread
    ) {
        var request = new CreatePlayerNotificationRequest(type, key, data, expiresInSeconds);
        var req = setupPost(url("%s/players/%s/notifications?replaceUnread=%s", url, playerId, replaceUnread), GSON.toJson(request));
        var res = doRequest("createNotification", req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 201) {
            throw new SessionService.InternalError("Failed to create notification (" + res.statusCode() + "): " + res.body());
        }
    }

    public record PlayerServiceError(@NotNull String code, @NotNull String message) {
    }
}
