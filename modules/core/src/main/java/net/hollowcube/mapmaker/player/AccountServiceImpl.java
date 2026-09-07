package net.hollowcube.mapmaker.player;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.opentelemetry.api.OpenTelemetry;
import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.player.responses.TotpSetupResponse;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

public class AccountServiceImpl extends AbstractHttpService implements AccountService {
    private static final System.Logger logger = System.getLogger(AccountServiceImpl.class.getName());

    private final String url;

    public AccountServiceImpl(@Nullable OpenTelemetry otel, @NotNull String url) {
        super(otel);
        this.url = String.format("%s/v2/internal", url);
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

}
