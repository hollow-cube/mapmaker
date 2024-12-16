package net.hollowcube.mapmaker.perm;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.JsonObject;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerPreLoginEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import org.apache.kafka.common.utils.CopyOnWriteMap;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

@Blocking
public class PermManagerImpl extends AbstractHttpService implements PermManager {
    private static final Logger logger = LoggerFactory.getLogger(PermManagerImpl.class);

    private static final SimpleDateFormat TIME_FORMAT;

    static {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        TIME_FORMAT.setTimeZone(tz);
    }

    // language=JSON
    private static final String PLATFORM_QUERY_TEMPLATE = """
            {
              "consistency": {
                "minimizeLatency": true
              },
              "resource": {
                "objectType": "mapmaker/platform",
                "objectId": "0"
              },
              "permission": "%s",
              "subject": {
                "object": {
                  "objectType": "mapmaker/player",
                  "objectId": "%s"
                }
              },
              "context": {
                "never_set": true,
                "current_time": "%s"
              }
            }
            """;

    private final HttpClient client = HttpClient.newHttpClient();
    private final String baseUrl;
    private final String token;

    private final Map<String, Cache<String, Boolean>> platformPermissions = new CopyOnWriteMap<>();
    private final Map<PlatformPermLike, PrefetchCondition> prefetchConditions = new CopyOnWriteMap<>();

    public PermManagerImpl(@NotNull String address, @NotNull String token) {
        this.baseUrl = address;
        this.token = token;

        MinecraftServer.getGlobalEventHandler()
                .addListener(AsyncPlayerPreLoginEvent.class, event -> prefetchConditions.values().forEach(c -> c.addPlayer(event.getGameProfile().uuid().toString())))
                .addListener(PlayerDisconnectEvent.class, event -> prefetchConditions.values().forEach(c -> c.removePlayer(event.getPlayer())));
    }

    @Override
    public void invalidate(@NotNull PlatformPermLike perm, @NotNull String playerId) {
        var cache = platformPermissions.get(perm.permName());
        if (cache != null) cache.invalidate(playerId);

        prefetchConditions.values().forEach(c -> {
            if (!c.perm.permName().equals(perm.permName())) return;

            FutureUtil.submitVirtual(() -> {
                c.allowedPlayers.remove(playerId);
                if (hasPlatformPermission0(playerId, perm)) {
                    c.allowedPlayers.add(playerId);
                }
            });
        });
    }

    @Override
    public void overwrite(@NotNull PlatformPermLike perm, @NotNull String playerId, boolean value) {
        var cache = platformPermissions.get(perm.permName());
        if (cache != null) cache.put(playerId, value);

        prefetchConditions.values().forEach(c -> {
            if (!c.perm.permName().equals(perm.permName())) return;
            c.allowedPlayers.add(playerId);
        });
    }

    @Override
    public boolean hasPlatformPermission(@NotNull Player player, @NotNull PlatformPermLike perm) {
        var cache = platformPermissions.computeIfAbsent(perm.permName(), k -> Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build());
        return cache.get(player.getUuid().toString(), k -> hasPlatformPermission0(k, perm));
    }

    @Override
    public boolean hasMapPermission(@NotNull Player player, @NotNull String mapId, @NotNull MapPerm perm) {
        return false;
    }

    @Override
    public @NotNull Predicate<String> createPrefetchedCondition(@NotNull PlatformPermLike perm) {
        return prefetchConditions.computeIfAbsent(perm, PrefetchCondition::new);
    }

    private boolean hasPlatformPermission0(@NotNull String playerId, @NotNull PlatformPermLike perm) {
        FutureUtil.assertThreadWarn();

        var body = PLATFORM_QUERY_TEMPLATE.formatted(perm.permName(), playerId, TIME_FORMAT.format(new Date()));
        var req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/permissions/check"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json");
        var res = doRequest("check_permission", req, HttpResponse.BodyHandlers.ofString());
        var result = GSON.fromJson(res.body(), JsonObject.class);

        enum Permissionship {
            PERMISSIONSHIP_UNSPECIFIED,
            PERMISSIONSHIP_NO_PERMISSION,
            PERMISSIONSHIP_HAS_PERMISSION,
            PERMISSIONSHIP_CONDITIONAL_PERMISSION;
        }

        var permissionship = Permissionship.valueOf(result.get("permissionship").getAsString());
        var state = permissionship == Permissionship.PERMISSIONSHIP_HAS_PERMISSION ||
                // Conditional is allowed here because of audit log hack.
                permissionship == Permissionship.PERMISSIONSHIP_CONDITIONAL_PERMISSION;

        if (perm instanceof PlatformPerm) logger.info("platform perm check: {} {} -> {}", playerId, perm, state);
        return state;
    }

    private class PrefetchCondition implements Predicate<String> {

        private final Set<String> allowedPlayers = new CopyOnWriteArraySet<>();
        private final PlatformPermLike perm;

        private PrefetchCondition(PlatformPermLike perm) {
            this.perm = perm;
        }

        @Blocking
        public void addPlayer(@NotNull String playerId) {
            if (hasPlatformPermission0(playerId, perm)) {
                allowedPlayers.add(playerId);
            }
        }

        public void removePlayer(@NotNull Player player) {
            allowedPlayers.remove(player.getUuid().toString());
        }

        @Override
        public boolean test(@NotNull String playerId) {
            return allowedPlayers.contains(playerId);
        }
    }

}
