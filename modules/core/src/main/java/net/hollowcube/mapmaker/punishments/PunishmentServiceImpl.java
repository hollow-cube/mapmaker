package net.hollowcube.mapmaker.punishments;

import com.google.gson.reflect.TypeToken;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.mapmaker.punishments.types.Punishment;
import net.hollowcube.mapmaker.punishments.types.PunishmentLadder;
import net.hollowcube.mapmaker.punishments.types.PunishmentType;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class PunishmentServiceImpl extends AbstractHttpService implements PunishmentService {
    private final String url;

    public PunishmentServiceImpl(@NotNull String url) {
        this.url = String.format("%s/v2/internal", url);
    }

    @Override
    public @NotNull List<Punishment> getPunishments(@Nullable String playerId, @Nullable UUID executorId, @Nullable PunishmentType type) {
        var request = HttpRequest.newBuilder()
                .uri(url(
                        "%s/punishments?playerId=%s&executorId=%s&type=%s",
                        url,
                        Objects.requireNonNullElse(playerId, ""),
                        OpUtils.mapOr(executorId, Object::toString, ""),
                        OpUtils.mapOr(type, PunishmentType::name, "").toLowerCase(Locale.ROOT)
                ))
                .build();
        var response = doRequest(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new InternalError("Failed to get punishments (" + response.statusCode() + "): " + response.body());
        }

        return GSON.fromJson(response.body(), new TypeToken<List<Punishment>>() {
        }.getType());
    }

    @Override
    public @Nullable Punishment getActivePunishment(@NotNull String playerId, @NotNull PunishmentType type) {
        var request = HttpRequest.newBuilder()
                .uri(url("%s/punishments/%s/active?punishmentType=%s", url, playerId, type.name().toLowerCase(Locale.ROOT)))
                .GET()
                .build();

        var response = doRequest(request, HttpResponse.BodyHandlers.ofString());
        return switch (response.statusCode()) {
            case 200 -> GSON.fromJson(response.body(), Punishment.class);
            case 404 -> null;
            default ->
                    throw new InternalError("Failed to revoke punishment (" + response.statusCode() + "): " + response.body());
        };
    }

    @Override
    public @NotNull Punishment createPunishment(@NotNull UUID playerId, @NotNull UUID executorId,
                                                @NotNull PunishmentType type, @Nullable String comment,
                                                @Nullable String reason) {
        var body = new HashMap<String, String>();
        body.put("playerId", playerId.toString());
        body.put("executorId", executorId.toString());
        body.put("punishmentType", type.name().toLowerCase(Locale.ROOT));
        if (comment != null) body.put("comment", comment);

        if (reason != null) {
            body.put("reason", reason);
        }

        var request = HttpRequest.newBuilder()
                .uri(URI.create(url + "/punishments"))
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                .build();

        var response = doRequest(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new InternalError("Failed to create punishment (" + response.statusCode() + "): " + response.body());
        }

        return GSON.fromJson(response.body(), Punishment.class);
    }

    @Override
    public void revokePunishment(@NotNull UUID playerId, @NotNull PunishmentType type,
                                 @NotNull UUID revokedBy, @NotNull String revokedReason) {
        var body = Map.of(
                "playerId", playerId.toString(),
                "type", type.name().toLowerCase(Locale.ROOT),
                "revokedBy", revokedBy.toString(),
                "revokedReason", revokedReason
        );
        var request = HttpRequest.newBuilder()
                .uri(URI.create(url + "/punishments/revoke"))
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                .build();

        var response = doRequest(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new InternalError("Failed to revoke punishment (" + response.statusCode() + "): " + response.body());
        }
    }

    @Override
    public @NotNull List<PunishmentLadder> getAllLadders() {
        return this.getLadders("");
    }

    @Override
    public @NotNull List<PunishmentLadder> getLaddersByType(@NotNull PunishmentType type) {
        return this.getLadders("?punishmentType=" + type.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public @NotNull List<PunishmentLadder> searchLadders(@NotNull String idQuery, @NotNull PunishmentType type) {
        return this.getLadders("?id=" + idQuery + "&punishmentType=" + type.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public @NotNull PunishmentLadder getLadderById(@NotNull String id) {
        var ladders = this.getLadders("?id=" + id);
        if (ladders.size() != 1) {
            throw new InternalError("More than one punishment ladder returned for id: " + id);
        }

        return ladders.getFirst();
    }

    private @NotNull List<PunishmentLadder> getLadders(@NotNull String query) {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(url + "/punishments/ladders" + query))
                .build();

        var response = doRequest(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new InternalError("Failed to get punishment ladders (" + response.statusCode() + "): " + response.body());
        }

        return GSON.fromJson(response.body(), new TypeToken<List<PunishmentLadder>>() {
        }.getType());
    }
}
