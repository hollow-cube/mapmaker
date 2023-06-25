package net.hollowcube.mapmaker.player;

import net.hollowcube.mapmaker.util.AbstractHttpService;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class PlayerServiceImpl extends AbstractHttpService implements PlayerService {
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
}
