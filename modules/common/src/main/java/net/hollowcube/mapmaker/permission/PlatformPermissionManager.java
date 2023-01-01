package net.hollowcube.mapmaker.permission;

import com.authzed.api.v1.Core.ObjectReference;
import com.authzed.api.v1.Core.Relationship;
import com.authzed.api.v1.PermissionsServiceGrpc.PermissionsServiceFutureStub;
import net.hollowcube.common.result.FutureResult;
import org.jetbrains.annotations.NotNull;

public class PlatformPermissionManager extends ZPermissionManager {
    private static final ObjectReference PLATFORM_OBJECT = ObjectReference.newBuilder()
            .setObjectType("mapmaker/platform")
            .setObjectId("1")
            .build();


    public PlatformPermissionManager(@NotNull PermissionsServiceFutureStub permissionService) {
        super(permissionService);
    }

    public @NotNull FutureResult<Void> addAdmin(@NotNull String playerId) {
        var relationship = Relationship.newBuilder()
                .setResource(PLATFORM_OBJECT)
                .setRelation("admin")
                .setSubject(createPlayerSubject(playerId))
                .build();
        return createRelationship(relationship);
    }

}
