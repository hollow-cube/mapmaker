package net.hollowcube.mapmaker.player;

import io.prometheus.client.Summary;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class PlayerServiceImpl extends AbstractHttpService implements PlayerService {
    private static final Summary remoteFetchDisplayNameTime = Summary.build()
            .namespace("mapmaker").name("remote_fetch_display_name_time_seconds")
            .help("Summary of the time it takes to fetch a player's display name from the remote service")
            .register();

    private static final System.Logger logger = System.getLogger(PlayerServiceImpl.class.getName());

    private final String url;

    public PlayerServiceImpl(String url) {
        this.url = String.format("%s/v1/internal/players", url);
    }

    @Override
    public void updatePlayerData(@NotNull String id, @NotNull PlayerDataUpdateRequest update) {
        logger.log(System.Logger.Level.INFO, "update playerdata for {0}", id);
        var reqBody = GSON.toJson(update);
        var req = HttpRequest.newBuilder()
                .method("PATCH", HttpRequest.BodyPublishers.ofString(reqBody))
                .uri(URI.create(url + "/" + id))
                .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200)
            throw new SessionService.InternalError("Failed to update session (" + res.statusCode() + "): " + res.body());
    }

    @Override
    public @NotNull Component getPlayerDisplayName(@NotNull String id) {
        // If the player is online we have an up-to-date display name anyway
        var player = MinecraftServer.getConnectionManager().getPlayer(id);
        if (player != null) {
            return PlayerDataV2.fromPlayer(player).displayName();
        }

        //todo probably should have some basic cache here

        try (var $ = remoteFetchDisplayNameTime.startTimer()) {
            var req = HttpRequest.newBuilder()
                    .uri(URI.create(url + "/" + id + "/displayname"))
                    .build();
            var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
            return switch (res.statusCode()) {
                case 200 -> GSON.fromJson(res.body(), Component.class);
                case 404 -> Component.text("Unknown Player");
                default -> throw new SessionService.InternalError("Failed to get player display name (" + res.statusCode() + "): " + res.body());
            };
        }

    }
}
