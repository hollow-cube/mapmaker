package net.hollowcube.mapmaker.perm;

import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import static com.authzed.api.v1.PermissionsServiceGrpc.PermissionsServiceBlockingStub;

@Blocking
public class PermManagerImpl implements PermManager {
    private final PermissionsServiceBlockingStub svc;

    public PermManagerImpl(@NotNull String address, @NotNull String token) {
//        var channel = ManagedChannelBuilder.forTarget(address)
//                .usePlaintext()
//                .build();
//        svc = PermissionsServiceGrpc.newBlockingStub(channel)
//                .withCallCredentials(new BearerToken(token));
        this.svc = null;
    }

    @Override
    public boolean hasPlatformPermission(@NotNull Player player, @NotNull PlatformPerm perm) {
        return false;
    }

    @Override
    public boolean hasMapPermission(@NotNull Player player, @NotNull String mapId, @NotNull MapPerm perm) {
        return false;
    }

}
