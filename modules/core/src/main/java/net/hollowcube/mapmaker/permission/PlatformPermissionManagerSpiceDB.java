package net.hollowcube.mapmaker.permission;

import com.authzed.api.v1.Core.ObjectReference;
import com.authzed.api.v1.PermissionsServiceGrpc.PermissionsServiceFutureStub;
import org.jetbrains.annotations.NotNull;

public class PlatformPermissionManagerSpiceDB extends ZPermissionManager {
    private static final ObjectReference PLATFORM_OBJECT = ObjectReference.newBuilder()
            .setObjectType("mapmaker/platform")
            .setObjectId("1")
            .build();


    public PlatformPermissionManagerSpiceDB(@NotNull PermissionsServiceFutureStub permissionService) {
        super(permissionService);
    }


}
