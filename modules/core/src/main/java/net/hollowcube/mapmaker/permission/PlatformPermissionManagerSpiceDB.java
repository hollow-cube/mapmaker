package net.hollowcube.mapmaker.permission;

import com.authzed.api.v1.Core.ObjectReference;
import com.authzed.api.v1.PermissionService.CheckPermissionRequest;
import com.authzed.api.v1.PermissionsServiceGrpc.PermissionsServiceBlockingStub;
import org.jetbrains.annotations.NotNull;

public class PlatformPermissionManagerSpiceDB extends ZPermissionManager implements PlatformPermissionManager {
    public static final ObjectReference PLATFORM_OBJECT = ObjectReference.newBuilder()
            .setObjectType("mapmaker/platform")
            .setObjectId("1")
            .build();


    public PlatformPermissionManagerSpiceDB(@NotNull PermissionsServiceBlockingStub permissionService) {
        super(permissionService);
    }


    @Override
    public boolean checkPermission(@NotNull String playerId, @NotNull PlatformPermission permission) {
        var req = CheckPermissionRequest.newBuilder()
                .setResource(PLATFORM_OBJECT)
                .setPermission(permission.key())
                .setSubject(createPlayerSubject(playerId))
                .build();
        return checkPermission(req);
    }
}
