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
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static com.authzed.api.v1.PermissionService.*;
import static com.authzed.api.v1.PermissionsServiceGrpc.PermissionsServiceBlockingStub;

@Blocking
public class PermManagerImpl implements PermManager {
    private static final Logger logger = LoggerFactory.getLogger(PermManagerImpl.class);

    private static final ObjectReference PLATFORM_OBJECT = ObjectReference.newBuilder().setObjectType("mapmaker/platform").setObjectId("0").build();

    private final PermissionsServiceBlockingStub svc;
    private final Cache<String, Boolean>[] platformPermissions;

    public PermManagerImpl(@NotNull String address, @NotNull String token) {
        var channel = ManagedChannelBuilder.forTarget(address).usePlaintext().build();
        svc = PermissionsServiceGrpc.newBlockingStub(channel)
                .withCallCredentials(new BearerToken(token));

        platformPermissions = new Cache[PlatformPerm.values().length];
        for (var i = 0; i < platformPermissions.length; i++) {
            var perm = PlatformPerm.values()[i];
            platformPermissions[i] = Caffeine.newBuilder()
                    .expireAfterWrite(30, TimeUnit.SECONDS)
                    .build();
        }
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

}
