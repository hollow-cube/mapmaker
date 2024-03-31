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
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerPreLoginEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final PermissionsServiceBlockingStub svc;
    private final Cache<String, Boolean>[] platformPermissions;

    private final List<PrefetchCondition> prefetchConditions = new ArrayList<>();

    public PermManagerImpl(@NotNull String address, @NotNull String token) {
        var channel = ManagedChannelBuilder.forTarget(address).usePlaintext().build();
        svc = PermissionsServiceGrpc.newBlockingStub(channel)
                .withCallCredentials(new BearerToken(token));

        platformPermissions = new Cache[PlatformPerm.values().length];
        for (var i = 0; i < platformPermissions.length; i++) {
            var perm = PlatformPerm.values()[i];
            platformPermissions[i] = Caffeine.newBuilder()
                    .expireAfterWrite(5, TimeUnit.MINUTES)
                    .build();
        }

        MinecraftServer.getGlobalEventHandler()
                .addListener(AsyncPlayerPreLoginEvent.class, event -> prefetchConditions.forEach(c -> c.addPlayer(event.getPlayer())))
                .addListener(PlayerDisconnectEvent.class, event -> prefetchConditions.forEach(c -> c.removePlayer(event.getPlayer())));
    }

    @Override
    public boolean hasPlatformPermission(@NotNull Player player, @NotNull PlatformPerm perm) {
        return platformPermissions[perm.ordinal()].get(
                player.getUuid().toString(),
                k -> hasPlatformPermission0(k, perm)
        );
    }

    @Override
    public boolean hasMapPermission(@NotNull Player player, @NotNull String mapId, @NotNull MapPerm perm) {
        return false;
    }

    @Override
    public @NotNull Predicate<Player> createPrefetchedCondition(@NotNull PlatformPerm perm) {
        var condition = new PrefetchCondition(perm);
        prefetchConditions.add(condition);
        return condition;
    }

    private boolean hasPlatformPermission0(@NotNull String playerId, @NotNull PlatformPerm perm) {
        var req = CheckPermissionRequest.newBuilder()
                .setConsistency(Consistency.newBuilder().setMinimizeLatency(true).build())
                .setResource(PLATFORM_OBJECT)
                .setSubject(SubjectReference.newBuilder()
                        .setObject(ObjectReference.newBuilder().setObjectType("mapmaker/player").setObjectId(playerId).build())
                        .build())
                .setPermission(perm.name().toLowerCase(Locale.ROOT))
                //todo my audit log hack is not working
                .setContext(Struct.newBuilder().putFields("never_set", Value.newBuilder().setBoolValue(true).build()).build())
                .build();
        var res = svc.checkPermission(req);
        var state = res.getPermissionship() != CheckPermissionResponse.Permissionship.PERMISSIONSHIP_NO_PERMISSION;
        logger.info("platform perm check: {} {} -> {}", playerId, perm, state);
        return state;
    }

    private class PrefetchCondition implements Predicate<Player> {

        private final Set<UUID> allowedPlayers = new CopyOnWriteArraySet<>();
        private final PlatformPerm perm;

        private PrefetchCondition(PlatformPerm perm) {
            this.perm = perm;
        }

        @Blocking
        public void addPlayer(@NotNull Player player) {
            var playerId = player.getUuid().toString();
            if (hasPlatformPermission0(playerId, perm)) {
                allowedPlayers.add(player.getUuid());
            }
        }

        public void removePlayer(@NotNull Player player) {
            allowedPlayers.remove(player.getUuid());
        }

        @Override
        public boolean test(@NotNull Player player) {
            return allowedPlayers.contains(player.getUuid());
        }
    }

}
