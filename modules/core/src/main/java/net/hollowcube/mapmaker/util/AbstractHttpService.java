package net.hollowcube.mapmaker.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.object.ObjectType;
import net.hollowcube.mapmaker.player.DisplayName;
import net.hollowcube.mapmaker.session.SessionUpdateMessage;
import net.hollowcube.mapmaker.temp.ChatMessageData;
import net.hollowcube.mapmaker.temp.ClientChatMessageData;
import net.hollowcube.mapmaker.util.dfu.DFU;
import net.hollowcube.mapmaker.util.gson.*;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Optional;

public abstract class AbstractHttpService {
    private static final System.Logger logger = System.getLogger(MapServiceImpl.class.getName());

    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(MapVariant.class, new EnumTypeAdapter<>(MapVariant.class))
            .registerTypeAdapter(SaveStateType.class, new EnumTypeAdapter<>(SaveStateType.class))
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
            .disableJdkUnsafe()
            .create();

    public static final String hostname; //todo replace me with ServerRuntime call

    private final HttpClient httpClient = HttpClient.newHttpClient();

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

}
