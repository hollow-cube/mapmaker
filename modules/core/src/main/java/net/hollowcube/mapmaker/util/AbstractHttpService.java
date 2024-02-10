package net.hollowcube.mapmaker.util;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.semconv.SemanticAttributes;
import net.hollowcube.common.ServerRuntime;
import net.hollowcube.mapmaker.backpack.BackpackItem;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.object.ObjectType;
import net.hollowcube.mapmaker.player.AppliedRewards;
import net.hollowcube.mapmaker.player.DisplayName;
import net.hollowcube.mapmaker.player.PlayerDataUpdateMessage;
import net.hollowcube.mapmaker.player.RewardType;
import net.hollowcube.mapmaker.punishments.PunishmentType;
import net.hollowcube.mapmaker.session.SessionUpdateMessage;
import net.hollowcube.mapmaker.temp.ChatMessageData;
import net.hollowcube.mapmaker.temp.ClientChatMessageData;
import net.hollowcube.mapmaker.util.dfu.DFU;
import net.hollowcube.mapmaker.util.gson.*;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractHttpService {
    private static final System.Logger logger = System.getLogger(MapServiceImpl.class.getName());

    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(MapVariant.class, new EnumTypeAdapter<>(MapVariant.class))
            .registerTypeAdapter(SaveStateType.class, new EnumTypeAdapter<>(SaveStateType.class))
            .registerTypeAdapter(BackpackItem.class, new EnumTypeAdapter<>(BackpackItem.class))
            .registerTypeAdapter(RewardType.class, new EnumTypeAdapter<>(RewardType.class))
            .registerTypeAdapter(MapVerification.class, new EnumOrdinalTypeAdapter<>(MapVerification.class))
            .registerTypeAdapter(MapSize.class, new EnumOrdinalTypeAdapter<>(MapSize.class))
            .registerTypeAdapter(PersonalizedMapData.Progress.class, new EnumOrdinalTypeAdapter<>(PersonalizedMapData.Progress.class))
            .registerTypeAdapter(ClientChatMessageData.Type.class, new EnumOrdinalTypeAdapter<>(ClientChatMessageData.Type.class))
            .registerTypeAdapter(ChatMessageData.Part.Type.class, new EnumOrdinalTypeAdapter<>(ChatMessageData.Part.Type.class))
            .registerTypeAdapter(SessionUpdateMessage.Action.class, new EnumOrdinalTypeAdapter<>(SessionUpdateMessage.Action.class))
            .registerTypeAdapter(MapRating.State.class, new EnumOrdinalTypeAdapter<>(MapRating.State.class))
            .registerTypeAdapter(MapQuality.class, new EnumOrdinalTypeAdapter<>(MapQuality.class))
            .registerTypeAdapter(ReportCategory.class, new EnumOrdinalTypeAdapter<>(ReportCategory.class))
            .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
            .registerTypeAdapter(Material.class, new MaterialTypeAdapter())
            .registerTypeAdapter(Component.class, new ComponentTypeAdapter())
            .registerTypeAdapter(ObjectType.class, new ObjectTypeTypeAdapter())
            .registerTypeAdapter(Point.class, new PointTypeAdapter())
            .registerTypeAdapter(DisplayName.class, new DisplayNameTypeAdapter())
            .registerTypeAdapter(SaveState.PlayState.class, DFU.JsonSerializer(SaveState.PlayState.CODEC))
            .registerTypeAdapter(SaveState.BuildState.class, DFU.JsonSerializer(SaveState.BuildState.CODEC))
            .registerTypeAdapter(Optional.class, new OptionalTypeAdapter())
            .registerTypeAdapter(AppliedRewards.class, new FieldSerializer<>(AppliedRewards::new, AppliedRewards::entries, new TypeToken<List<AppliedRewards.Entry>>() {
            }.getType()))
            .registerTypeAdapter(PlayerDataUpdateMessage.Action.class, new EnumOrdinalTypeAdapter<>(PlayerDataUpdateMessage.Action.class))
            .registerTypeAdapter(PlayerDataUpdateMessage.ReasonType.class, new EnumOrdinalTypeAdapter<>(PlayerDataUpdateMessage.ReasonType.class))
            .registerTypeAdapter(PunishmentType.class, new EnumTypeAdapter<>(PunishmentType.class))
            .disableJdkUnsafe()
            .create();
    public static final TextMapSetter<HttpRequest.Builder> CONTEXT_PROPAGATOR = (carrier, key, value) -> {
        if (carrier == null) return;
        carrier.header(key, value);
    };

    public static final String hostname; //todo replace me with ServerRuntime call

    private final HttpClient httpClient = HttpClient.newHttpClient();
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
        try {
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
}
