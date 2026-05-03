package net.hollowcube.mapmaker.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.semconv.SemanticAttributes;
import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.api.interaction.Command;
import net.hollowcube.mapmaker.api.interaction.Interaction;
import net.hollowcube.mapmaker.api.interaction.InteractionResponse;
import net.hollowcube.mapmaker.api.maps.MapRating;
import net.hollowcube.mapmaker.api.maps.MapReport;
import net.hollowcube.mapmaker.api.maps.MapRole;
import net.hollowcube.mapmaker.api.maps.MapWorldMessage;
import net.hollowcube.mapmaker.backpack.BackpackItem;
import net.hollowcube.mapmaker.invite.types.InviteType;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.player.DisplayName;
import net.hollowcube.mapmaker.player.PlayerDataUpdateMessage;
import net.hollowcube.mapmaker.player.RewardType;
import net.hollowcube.mapmaker.punishments.types.PunishmentType;
import net.hollowcube.mapmaker.punishments.types.PunishmentUpdateMessage;
import net.hollowcube.mapmaker.session.SessionUpdateMessage;
import net.hollowcube.mapmaker.temp.ChatMessageData;
import net.hollowcube.mapmaker.temp.ClientChatMessageData;
import net.hollowcube.mapmaker.util.gson.*;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.item.Material;
import org.intellij.lang.annotations.PrintFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractHttpService {
    private static final System.Logger logger = System.getLogger(MapServiceImpl.class.getName());

    public static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(MapVariant.class, new EnumTypeAdapter<>(MapVariant.class))
        .registerTypeAdapter(SaveStateType.class, new EnumTypeAdapter<>(SaveStateType.class))
        .registerTypeAdapter(BackpackItem.class, new EnumTypeAdapter<>(BackpackItem.class))
        .registerTypeAdapter(RewardType.class, new EnumTypeAdapter<>(RewardType.class))
        .registerTypeAdapter(MapTags.Tag.class, new EnumTypeAdapter<>(MapTags.Tag.class))
        .registerTypeAdapter(InviteType.class, new EnumTypeAdapter<>(InviteType.class))
        .registerTypeAdapter(Leaderboard.Format.class, new EnumTypeAdapter<>(Leaderboard.Format.class))
        .registerTypeAdapter(MapVerification.class, new LenientEnumTypeAdapter<>(MapVerification.class))
        .registerTypeAdapter(MapSize.class, new LenientEnumTypeAdapter<>(MapSize.class))
        .registerTypeAdapter(MapWorldMessage.Action.class, new LenientEnumTypeAdapter<>(MapWorldMessage.Action.class))
        .registerTypeAdapter(Command.Argument.Type.class, new LenientEnumTypeAdapter<>(Command.Argument.Type.class))
        .registerTypeAdapter(Interaction.Type.class, new LenientEnumTypeAdapter<>(Interaction.Type.class))
        .registerTypeAdapter(InteractionResponse.Type.class, new LenientEnumTypeAdapter<>(InteractionResponse.Type.class))
        .registerTypeAdapter(MapRole.class, new LenientEnumTypeAdapter<>(MapRole.class))
        .registerTypeAdapter(PlayerMapProgress.Progress.class, new EnumTypeAdapter<>(PlayerMapProgress.Progress.class))
        .registerTypeAdapter(ClientChatMessageData.Type.class, new EnumOrdinalTypeAdapter<>(ClientChatMessageData.Type.class))
        .registerTypeAdapter(ChatMessageData.Part.Type.class, new EnumOrdinalTypeAdapter<>(ChatMessageData.Part.Type.class))
        .registerTypeAdapter(SessionUpdateMessage.Action.class, new EnumOrdinalTypeAdapter<>(SessionUpdateMessage.Action.class))
        .registerTypeAdapter(MapRating.State.class, new LenientEnumTypeAdapter<>(MapRating.State.class))
        .registerTypeAdapter(MapQuality.class, new LenientEnumTypeAdapter<>(MapQuality.class))
        .registerTypeAdapter(MapReport.Category.class, new EnumTypeAdapter<>(MapReport.Category.class))
        .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
        .registerTypeAdapter(Material.class, new MaterialTypeAdapter())
        .registerTypeAdapter(Component.class, new ComponentTypeAdapter())
        .registerTypeAdapter(Point.class, new PointTypeAdapter())
        .registerTypeAdapter(DisplayName.class, new DisplayNameTypeAdapter())
        .registerTypeAdapter(Optional.class, new OptionalTypeAdapter())
        .registerTypeAdapter(PlayerDataUpdateMessage.Action.class, new EnumOrdinalTypeAdapter<>(PlayerDataUpdateMessage.Action.class))
        .registerTypeAdapter(PlayerDataUpdateMessage.ReasonType.class, new EnumOrdinalTypeAdapter<>(PlayerDataUpdateMessage.ReasonType.class))
        .registerTypeAdapter(PunishmentType.class, new EnumTypeAdapter<>(PunishmentType.class))
        .registerTypeAdapter(PunishmentUpdateMessage.Action.class, new EnumOrdinalTypeAdapter<>(PunishmentUpdateMessage.Action.class))
        .registerTypeAdapter(UnsignedLongAdapter.class, new UnsignedLongAdapter.Creator())
        .disableJdkUnsafe()
        .create();
    public static final TextMapSetter<HttpRequest.Builder> CONTEXT_PROPAGATOR = (carrier, key, value) -> {
        if (carrier == null) return;
        carrier.header(key, value);
    };

    public static final String hostname; //todo replace me with ServerRuntime call
    public static final String userAgent = "github.com/hollow-cube/mapmaker@" + ServerRuntime.getRuntime().shortCommit();

    private final HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    private final OpenTelemetry otel;
    protected final Tracer tracer;

    protected AbstractHttpService() {
        this(null);
    }

    protected AbstractHttpService(@Nullable OpenTelemetry otel) {
        this.otel = otel;
        if (otel == null) {
            this.tracer = null;
            return;
        }
        this.tracer = otel.getTracer(getClass().getName(), ServerRuntime.getRuntime().version());
    }

    protected <T> HttpResponse<T> doRequest(@NotNull String name, @NotNull HttpRequest.Builder reqBuilder, HttpResponse.BodyHandler<T> handler) {
        FutureUtil.assertThreadWarn();
        var span = tracer.spanBuilder(name).setSpanKind(SpanKind.CLIENT).startSpan();
        var context = Context.root().with(span);
        try {
            // Append propagation headers to the request
            otel.getPropagators().getTextMapPropagator().inject(context, reqBuilder, CONTEXT_PROPAGATOR);
            var req = reqBuilder.build();
            span.setAttribute(SemanticAttributes.HTTP_REQUEST_METHOD, req.method());
            span.setAttribute(SemanticAttributes.URL_FULL, req.uri().toString());

            logger.log(System.Logger.Level.DEBUG, "{0} {1}", req.method(), req.uri());
            var res = httpClient.send(req, handler);
            if (res.statusCode() == 403) {
                // We simply convert auth issues to 404s
                logger.log(System.Logger.Level.ERROR, "auth failed for request: " + req.method() + " " + req.uri());
                throw new MapService.NotFoundError("???");
            }
            return res;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MapService.InternalError(e);
        } catch (IOException e) {
            throw new MapService.InternalError(e);
        } finally {
            span.end();
        }
    }

    protected <T> HttpResponse<T> doRequest(@NotNull HttpRequest req, HttpResponse.BodyHandler<T> handler) {
        FutureUtil.assertThreadWarn();
        try {
            logger.log(System.Logger.Level.INFO, "{0} {1}", req.method(), req.uri());
            return httpClient.send(req, handler);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MapService.InternalError(e);
        } catch (IOException e) {
            throw new MapService.InternalError(e);
        }
    }

    static {
        String hn;
        try {
            hn = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hn = "unknown";
        }
        hostname = hn;
    }

    protected static @NotNull UrlQueryBuilder urlQueryBuilder() {
        return new UrlQueryBuilder();
    }

    protected static final class UrlQueryBuilder {

        private final Map<String, String> parts = new HashMap<>();

        public UrlQueryBuilder add(@NotNull String key, @NotNull String value) {
            this.parts.put(key, value);
            return this;
        }

        public @NotNull String build() {
            var result = new StringBuilder();
            boolean first = true;

            for (var entry : this.parts.entrySet()) {
                if (first) {
                    first = false;
                    result.append('?');
                } else {
                    result.append('&');
                }
                result.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
                result.append('=');
                result.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            }

            return result.toString();
        }
    }

    protected static @NotNull URI url(@NotNull @PrintFormat String format, @NotNull Object... args) {
        return URI.create(String.format(format, args));
    }

    protected static HttpRequest.Builder setup(@NotNull URI uri) {
        // TODO setup common headers
        return HttpRequest.newBuilder(uri);
    }

    protected static HttpRequest.Builder setupGet(@NotNull URI uri) {
        return setup(uri).GET();
    }

    protected static HttpRequest.Builder setupDelete(@NotNull URI uri) {
        return setup(uri).DELETE();
    }

    protected static HttpRequest.Builder setupPost(@NotNull URI uri, @NotNull String body) {
        return setup(uri)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .header("Content-Type", "application/json");
    }

    protected static HttpRequest.Builder setupPatch(@NotNull URI uri, @NotNull String body) {
        return setup(uri)
            .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
            .header("Content-Type", "application/json");
    }
}
