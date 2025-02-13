package net.hollowcube.mapmaker.player;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.opentelemetry.api.OpenTelemetry;
import io.prometheus.client.Summary;
import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.player.responses.TotpSetupResponse;
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
            throw new SessionService.InternalError("Failed to update session (" + res.statusCode() + "): " + res.body());
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
            default ->
                    throw new SessionService.InternalError("Failed to get player backpack (" + res.statusCode() + "): " + res.body());
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
                    .uri(URI.create(url + "/players/" + id + "/displayname?v=2"));
            var res = doRequest("getPlayerDisplayName2", req, HttpResponse.BodyHandlers.ofString());
            return switch (res.statusCode()) {
                case 200 -> GSON.fromJson(res.body(), DisplayName.class);
                case 404 -> new DisplayName(List.of(new DisplayName.Part("username", "!error!", null)));
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
                .uri(URI.create(url + "/tab_complete"));
        var res = doRequest("getUsernameTabCompletions", req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200)
            throw new InternalError("Failed to get tab completions (" + res.statusCode() + "): " + res.body());
        return GSON.fromJson(res.body(), TabCompleteResponse.class);
    }

    @Override
    public @NotNull CreateCheckoutLinkResponse createCheckoutLink(@NotNull String source, @NotNull String username, @NotNull String product) {
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
            default ->
                    throw new InternalError("Verification attempt failed: (" + response.statusCode() + "): " + response.body());
        };
    }

    @Override
    public @NotNull TotpResult checkTotp(@NotNull String playerId, @NotNull String code) {
        var req = HttpRequest.newBuilder()
                .uri(URI.create(url + "/players/" + playerId + "/totp/" + code))
                .GET();

        var response = doRequest("checkTotp", req, HttpResponse.BodyHandlers.ofString());

        return switch (response.statusCode()) {
            case 200 -> TotpResult.VALID_CODE;
            case 400 -> TotpResult.INVALID_FORMAT;
            case 401 -> TotpResult.INVALID_CODE;
            case 404 -> TotpResult.NOT_ENABLED;
            default ->
                    throw new InternalError("Totp check failed: (" + response.statusCode() + "): " + response.body());
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
            default ->
                    throw new InternalError("Failed to begin totp setup: (" + response.statusCode() + "): " + response.body());
        };
    }

    @Override
    public @NotNull TotpSetupResult completeTotpSetup(@NotNull String playerId, @NotNull String code) {
        var body = GSON.toJson(Map.of(
                "code", code
        ));
        var req = HttpRequest.newBuilder()
                .uri(URI.create(url + "/players/" + playerId + "/totp/setup"))
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));

        var response = doRequest("completeTotpSetup", req, HttpResponse.BodyHandlers.ofString());

        return switch (response.statusCode()) {
            case 200 -> TotpSetupResult.COMPLETED;
            case 400 -> TotpSetupResult.INVALID_FORMAT;
            case 401 -> TotpSetupResult.INVALID_CODE;
            case 404 -> TotpSetupResult.NOT_STARTED;
            case 409 -> TotpSetupResult.ALREADY_ENABLED;
            default ->
                    throw new InternalError("Failed to complete totp setup: (" + response.statusCode() + "): " + response.body());
        };
    }
}
