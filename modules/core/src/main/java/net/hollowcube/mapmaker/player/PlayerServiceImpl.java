package net.hollowcube.mapmaker.player;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.prometheus.client.Summary;
import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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

    public PlayerServiceImpl(String url) {
        this.url = String.format("%s/v1/internal", url);
    }

    @Override
    public void updatePlayerData(@NotNull String id, @NotNull PlayerDataUpdateRequest update) {
        logger.log(System.Logger.Level.INFO, "update playerdata for {0}", id);
        var reqBody = GSON.toJson(update);
        var req = HttpRequest.newBuilder()
                .method("PATCH", HttpRequest.BodyPublishers.ofString(reqBody))
                .uri(URI.create(url + "/players/" + id))
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200)
            throw new SessionService.InternalError("Failed to update session (" + res.statusCode() + "): " + res.body());
    }

    @Override
    public @NotNull Set<String> getUnlockedCosmetics(@NotNull String playerId) {
        var req = HttpRequest.newBuilder()
                .uri(URI.create(url + "/players/" + playerId + "/cosmetics"))
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> GSON.fromJson(res.body(), new TypeToken<Set<String>>() {
            }.getType());
            case 404 -> Set.of();
            default ->
                    throw new SessionService.InternalError("Failed to get unlocked cosmetics (" + res.statusCode() + "): " + res.body());
        };
    }

    @Override
    public void buyCosmetic(@NotNull String id, @NotNull Cosmetic cosmetic, @Nullable Integer coins, @Nullable Integer cubits, @Nullable JsonObject items) {
        logger.log(System.Logger.Level.INFO, "buy cosmetic for {0}: {1}", id, cosmetic.path());
        var reqBodyData = new JsonObject();
        reqBodyData.addProperty("cosmeticId", cosmetic.path());
        if (coins != null) reqBodyData.addProperty("coins", coins);
        if (cubits != null) reqBodyData.addProperty("cubits", cubits);
        if (items != null) reqBodyData.add("items", items);
        var req = HttpRequest.newBuilder()
                .method("POST", HttpRequest.BodyPublishers.ofString(GSON.toJson(reqBodyData)))
                .uri(URI.create(url + "/players/" + id + "/cosmetics"))
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200)
            throw new SessionService.InternalError("Failed to update session (" + res.statusCode() + "): " + res.body());
    }

    @Override
    public @NotNull JsonObject getPlayerBackpack(@NotNull String id) {
        var req = HttpRequest.newBuilder()
                .uri(URI.create(url + "/players/" + id + "/backpack"))
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> GSON.fromJson(res.body(), JsonObject.class);
            case 404 -> new JsonObject();
            default ->
                    throw new SessionService.InternalError("Failed to get player backpack (" + res.statusCode() + "): " + res.body());
        };
    }

    @Override
    public @NotNull String getPlayerId(@NotNull String idOrUsername) {
        var req = HttpRequest.newBuilder()
                .uri(URI.create(url + "/players/" + idOrUsername + "/id"))
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> res.body();
            case 404 -> throw new NotFoundError();
            default ->
                    throw new SessionService.InternalError("Failed to get player id (" + res.statusCode() + "): " + res.body());
        };
    }

    @Override
    public @NotNull DisplayName getPlayerDisplayName2(@NotNull String id) {
        // If the player is online we have an up-to-date display name anyway
        var player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(UUID.fromString(id));
        if (player != null) {
            return PlayerDataV2.fromPlayer(player).displayName2();
        }

        //todo probably should have some basic cache here

        try (var $ = remoteFetchDisplayNameTime.startTimer()) {
            var req = HttpRequest.newBuilder()
                    .uri(URI.create(url + "/players/" + id + "/displayname?v=2"))
                    .build();
            var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
            return switch (res.statusCode()) {
                case 200 -> GSON.fromJson(res.body(), DisplayName.class);
                case 404 -> new DisplayName(List.of());
                default ->
                        throw new SessionService.InternalError("Failed to get player display name (" + res.statusCode() + "): " + res.body());
            };
        }
    }

    @Override
    public @NotNull TabCompleteResponse getUsernameTabCompletions(@NotNull String query) {
        if (query.isEmpty()) return new TabCompleteResponse(List.of());

        var reqBody = GSON.toJson(Map.of("query", query));
        var req = HttpRequest.newBuilder()
                .method("POST", HttpRequest.BodyPublishers.ofString(reqBody))
                .uri(URI.create(url + "/tab_complete"))
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200)
            throw new InternalError("Failed to get tab completions (" + res.statusCode() + "): " + res.body());
        return GSON.fromJson(res.body(), TabCompleteResponse.class);
    }

    @Override
    public @NotNull CreateCheckoutLinkResponse createCheckoutLink(@NotNull String source, @NotNull String playerId, @NotNull String productId) {
        var reqBody = GSON.toJson(Map.of(
                "type", "product",
                "source", source,
                "player", playerId,
                "product", productId
        ));
        var req = HttpRequest.newBuilder()
                .method("POST", HttpRequest.BodyPublishers.ofString(reqBody))
                .uri(URI.create(url + "/payments/checkout"))
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 201)
            throw new InternalError("Failed to create checkout url (" + res.statusCode() + "): " + res.body());
        return GSON.fromJson(res.body(), CreateCheckoutLinkResponse.class);
    }

    @Override
    public @NotNull CreateCheckoutLinkResponse createCheckoutLink(@NotNull String source, @NotNull String playerId, int cubits) {
        var reqBody = GSON.toJson(Map.of(
                "type", "cubits",
                "source", source,
                "player", playerId,
                "cubits", cubits
        ));
        var req = HttpRequest.newBuilder()
                .method("POST", HttpRequest.BodyPublishers.ofString(reqBody))
                .uri(URI.create(url + "/payments/checkout"))
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 201)
            throw new InternalError("Failed to create checkout url (" + res.statusCode() + "): " + res.body());
        return GSON.fromJson(res.body(), CreateCheckoutLinkResponse.class);
    }
}
