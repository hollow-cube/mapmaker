package net.hollowcube.mapmaker.perm;

import com.authzed.api.v1.Core.ObjectReference;
import com.authzed.api.v1.Core.SubjectReference;
import com.authzed.api.v1.PermissionsServiceGrpc;
import com.authzed.grpcutil.BearerToken;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import io.grpc.ManagedChannelBuilder;
import net.hollowcube.common.util.FutureUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerPreLoginEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import org.apache.kafka.common.utils.CopyOnWriteMap;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static com.authzed.api.v1.PermissionService.*;
import static com.authzed.api.v1.PermissionsServiceGrpc.PermissionsServiceBlockingStub;

@Blocking
public class PermManagerImpl implements PermManager {
    private static final Logger logger = LoggerFactory.getLogger(PermManagerImpl.class);

    private static final ObjectReference PLATFORM_OBJECT = ObjectReference.newBuilder().setObjectType("mapmaker/platform").setObjectId("0").build();

    private static final SimpleDateFormat TIME_FORMAT;

    static {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        TIME_FORMAT.setTimeZone(tz);
    }

    private final PermissionsServiceBlockingStub svc;
    private final Map<String, Cache<String, Boolean>> platformPermissions = new CopyOnWriteMap<>();

    private final List<PrefetchCondition> prefetchConditions = new ArrayList<>();

    public PermManagerImpl(@NotNull String address, @NotNull String token) {
        var channel = ManagedChannelBuilder.forTarget(address).usePlaintext().build();
        svc = PermissionsServiceGrpc.newBlockingStub(channel)
                .withCallCredentials(new BearerToken(token));

        MinecraftServer.getGlobalEventHandler()
                .addListener(AsyncPlayerPreLoginEvent.class, event -> prefetchConditions.forEach(c -> c.addPlayer(event.getPlayer())))
                .addListener(PlayerDisconnectEvent.class, event -> prefetchConditions.forEach(c -> c.removePlayer(event.getPlayer())));
    }

    @Override
    public void invalidate(@NotNull PlatformPermLike perm, @NotNull String playerId) {
        var cache = platformPermissions.get(perm.permName());
        if (cache != null) cache.invalidate(playerId);

        prefetchConditions.forEach(c -> {
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

        prefetchConditions.forEach(c -> {
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
        var condition = new PrefetchCondition(perm);
        prefetchConditions.add(condition);
        return condition;
    }

    private boolean hasPlatformPermission0(@NotNull String playerId, @NotNull PlatformPermLike perm) {
        var req = CheckPermissionRequest.newBuilder()
                .setConsistency(Consistency.newBuilder().setMinimizeLatency(true).build())
                .setResource(PLATFORM_OBJECT)
                .setSubject(SubjectReference.newBuilder()
                        .setObject(ObjectReference.newBuilder().setObjectType("mapmaker/player").setObjectId(playerId).build())
                        .build())
                .setPermission(perm.permName())
                //todo my audit log hack is not working
                .setContext(Struct.newBuilder()
                        .putFields("never_set", Value.newBuilder().setBoolValue(true).build())
                        .putFields("current_time", Value.newBuilder().setStringValue(TIME_FORMAT.format(new Date())).build())
                        .build())
                .build();
        var res = svc.checkPermission(req);
        var state = res.getPermissionship() != CheckPermissionResponse.Permissionship.PERMISSIONSHIP_NO_PERMISSION;
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
        public void addPlayer(@NotNull Player player) {
            var playerId = player.getUuid().toString();
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
