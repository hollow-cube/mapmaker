package net.hollowcube.mapmaker.player;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.opentelemetry.api.OpenTelemetry;
import io.prometheus.client.Summary;
import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.player.requests.CreatePlayerNotificationRequest;
import net.hollowcube.mapmaker.player.responses.*;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.minestom.server.MinecraftServer;
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

    public PlayerServiceImpl(@Nullable OpenTelemetry otel, String url) {
        super(otel);
        this.url = String.format("%s/v2/internal", url);
    }

    @Override
    public void updatePlayerData(String id, PlayerDataUpdateRequest update) {
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
    public Set<String> getUnlockedCosmetics(String playerId) {
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
        String id, Cosmetic cosmetic, @Nullable Integer coins, @Nullable Integer cubits,
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
    public void buyUpgrade(String playerId, String upgradeId, int cubits, JsonObject meta) {
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
    public JsonObject getPlayerBackpack(String id) {
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
    public String getPlayerId(String idOrUsername) {
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
    public PlayerData getPlayerData(String id) {
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
    public DisplayName getPlayerDisplayName2(String id) {
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
    public TabCompleteResponse getUsernameTabCompletions(String query, int limit) {
        return this.getTabCompletions(TabCompleteBody.forUsernames(query, limit));
    }

    // This is designed so other tab completions could be added in future if wanted
    @RuntimeGson
    private record TabCompleteBody(String query, int limit) {
        static TabCompleteBody forUsernames(String query, int limit) {
            return new TabCompleteBody(query, limit);
        }
    }

    private TabCompleteResponse getTabCompletions(TabCompleteBody body) {
        if (body.query().isEmpty()) {
            return new TabCompleteResponse(List.of());
        }

        var reqBody = GSON.toJson(body);
        var req = HttpRequest.newBuilder()
            .method("POST", HttpRequest.BodyPublishers.ofString(reqBody))
            .uri(URI.create(url + "/tab_complete"));
        var res = doRequest("getUsernameTabCompletions", req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200)
            throw new InternalError("Failed to get tab completions (" + res.statusCode() + "): " + res.body());
        return GSON.fromJson(res.body(), TabCompleteResponse.class);
    }

    @Override
    public CreateCheckoutLinkResponse createCheckoutLink(String source, String username, String product) {
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
    public @Nullable HypercubeStatus getHypercubeStatus(String playerId) {
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
    public TotpResult checkTotp(String playerId, @Nullable String code) {
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
    public TotpResult removeTotp(String playerId) {
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
    public @Nullable TotpSetupResponse beginTotpSetup(String playerId) {
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
    public TotpResult completeTotpSetup(String playerId, String code) {
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
    public List<PlayerAlts.Alt> getAlts(String playerId) {
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
    public Page<PlayerFriend> getPlayerFriends(String playerId, @Nullable Boolean onlineState, Pageable pageable) {
        var builder = urlQueryBuilder()
            .add("page", String.valueOf(pageable.page()))
            .add("pageSize", String.valueOf(pageable.pageSize()));
        if (onlineState != null) builder.add("onlineState", onlineState.toString());

        var req = HttpRequest.newBuilder()
            .uri(URI.create(url + "/players/%s/friends%s".formatted(playerId, builder.build())))
            .GET();
        var res = doRequest("getPlayerFriends", req, HttpResponse.BodyHandlers.ofString());

        return switch (res.statusCode()) {
            case 200 -> Page.fromJson(res.body(), PlayerFriend.class);
            default -> throw new InternalError("Failed to get friends (" + res.statusCode() + "): " + res.body());
        };
    }

    @Override
    public void removeFriend(String playerId, String targetId) {
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
    public Page<FriendRequest> getFriendRequests(String playerId, boolean incoming, Pageable pageable) {
        String direction = incoming ? "incoming" : "outgoing";
        var req = HttpRequest.newBuilder()
            .uri(URI.create(
                url + "/players/%s/friendRequests?direction=%s&page=%s&pageSize=%s".formatted(playerId, direction,
                    pageable.page(),
                    pageable.pageSize())))
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
    public SendFriendRequestResult sendFriendRequest(String playerId, String targetId) {
        var req = HttpRequest.newBuilder()
            .uri(URI.create(url + "/players/" + playerId + "/friendRequests/" + targetId))
            .POST(HttpRequest.BodyPublishers.noBody());
        var res = doRequest("sendFriendRequest", req, HttpResponse.BodyHandlers.ofString());

        return switch (res.statusCode()) {
            case 201 ->
                new SendFriendRequestResult(GSON.fromJson(res.body(), SendFriendRequestResponse.class).isRequest(),
                    null, null);
            case 401 -> {
                SendFriendRequestResult.LimitError error = GSON.fromJson(res.body(),
                    SendFriendRequestResult.LimitError.class);
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
    public FriendRequest deleteFriendRequest(String playerId, String targetId, boolean bidirectional) {
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
    public void blockPlayer(String playerId, String targetId) {
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
    public Page<BlockedPlayer> getBlockedPlayers(String playerId, Pageable pageable) {
        var req = HttpRequest.newBuilder()
            .uri(URI.create(url + "/players/%S/blocks?page=%s&pageSize=%s".formatted(playerId, pageable.page(),
                pageable.pageSize())))
            .GET();
        var res = doRequest("getBlockedPlayers", req, HttpResponse.BodyHandlers.ofString());

        return switch (res.statusCode()) {
            case 200 -> Page.fromJson(res.body(), BlockedPlayer.class);
            default ->
                throw new InternalError("Failed to get blocked players (" + res.statusCode() + "): " + res.body());
        };
    }

    @Override
    public void unblockPlayer(String playerId, String targetId) {
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
    public List<BlockedPlayer> getBlocksBetween(String playerId, String targetId, boolean bidirectional) {

        var req = HttpRequest.newBuilder()
            .uri(URI.create(
                "%s/players/%s/blocks/player/%s?bidirectional=%s".formatted(url, playerId, targetId, bidirectional)))
            .GET();
        var res = doRequest("getBlocksBetween", req, HttpResponse.BodyHandlers.ofString());

        return switch (res.statusCode()) {
            case 200 ->
                GSON.fromJson(res.body(), TypeToken.getParameterized(List.class, BlockedPlayer.class).getType());
            default ->
                throw new InternalError("Failed to get blocks between (" + res.statusCode() + "): " + res.body());
        };
    }

    @Override
    public PlayerNotificationResponse getNotifications(String playerId, int page, boolean unread) {
        var req = setupGet(url("%s/players/%s/notifications?page=%d&unread=%s", url, playerId, page, unread));
        var res = doRequest("getNotifications", req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> GSON.fromJson(res.body(), PlayerNotificationResponse.class);
            case 400 -> throw new IllegalArgumentException("Invalid page number");
            default -> throw new SessionService.InternalError(
                "Failed to get notifications (" + res.statusCode() + "): " + res.body());
        };
    }

    @Override
    public void deleteNotification(String playerId, String notificationId) {
        var req = setupDelete(url("%s/players/%s/notifications/%s", url, playerId, notificationId));
        var res = doRequest("deleteNotification", req, HttpResponse.BodyHandlers.ofString());
        switch (res.statusCode()) {
            case 200 -> {
            }
            case 404 -> throw new NotFoundError();
            default -> throw new SessionService.InternalError(
                "Failed to delete notification (" + res.statusCode() + "): " + res.body());
        }
    }

    @Override
    public void markNotificationRead(String playerId, String notificationId, boolean read) {
        var data = Map.of("read", read);
        var req = setupPatch(url("%s/players/%s/notifications/%s", url, playerId, notificationId), GSON.toJson(data));
        var res = doRequest("markNotificationRead", req, HttpResponse.BodyHandlers.ofString());
        switch (res.statusCode()) {
            case 200 -> {
            }
            case 404 -> throw new NotFoundError();
            default -> throw new SessionService.InternalError(
                "Failed to mark notification read (" + res.statusCode() + "): " + res.body());
        }
    }

    @Override
    public void createNotification(
        String playerId,
        String type,
        String key,
        @Nullable JsonObject data,
        @Nullable Integer expiresInSeconds,
        boolean replaceUnread
    ) {
        var request = new CreatePlayerNotificationRequest(type, key, data, expiresInSeconds);
        var req = setupPost(url("%s/players/%s/notifications?replaceUnread=%s", url, playerId, replaceUnread),
            GSON.toJson(request));
        var res = doRequest("createNotification", req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 201) {
            throw new SessionService.InternalError(
                "Failed to create notification (" + res.statusCode() + "): " + res.body());
        }
    }

    @Override
    public @Nullable String getRecap(String playerId, int year) {
        var req = HttpRequest.newBuilder()
            .uri(URI.create(url + "/recap/" + playerId + "/" + year))
            .GET();
        var res = doRequest("getRecap", req, HttpResponse.BodyHandlers.ofString());

        return switch (res.statusCode()) {
            case 200 -> res.body();
            case 404 -> null;
            default -> throw new InternalError("Failed to get recap (" + res.statusCode() + "): " + res.body());
        };
    }

    @RuntimeGson
    public record PlayerServiceError(String code, String message) {
    }
}
