package net.hollowcube.mapmaker.obungus;

import io.opentelemetry.api.OpenTelemetry;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static net.hollowcube.mapmaker.map.MapServiceImpl.AUTHORIZER_HEADER;

public class ObungusServiceImpl extends AbstractHttpService implements ObungusService {
    private static final Logger logger = LoggerFactory.getLogger(ObungusServiceImpl.class);

    private final String url;

    public ObungusServiceImpl(@Nullable OpenTelemetry otel, @NotNull String url) {
        super(otel);
        this.url = String.format("%s/v2/obungus", url);
    }

    @Override
    public @NotNull ObungusBoxData getBoxFromReviewQueue(@NotNull String playerId) {
        logger.info("getting review box for player {}", playerId);
        var req = HttpRequest.newBuilder()
                .uri(URI.create(url + "/review_queue?player=" + playerId))
                .header(AUTHORIZER_HEADER, playerId);
        var res = doRequest("getBoxFromReviewQueue", req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> GSON.fromJson(res.body(), ObungusBoxData.class);
            default ->
                    throw new MapService.InternalError("failed to get review box: " + res.statusCode() + " " + res.body());
        };
    }
}
