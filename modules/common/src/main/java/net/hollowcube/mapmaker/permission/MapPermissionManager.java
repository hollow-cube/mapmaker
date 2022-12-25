package net.hollowcube.mapmaker.permission;

import com.authzed.api.v1.Core.Relationship;
import com.authzed.api.v1.PermissionService.CheckPermissionRequest;
import com.authzed.api.v1.PermissionsServiceGrpc.PermissionsServiceFutureStub;
import net.hollowcube.common.result.Error;
import net.hollowcube.common.result.FutureResult;
import net.hollowcube.mapmaker.model.MapData;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;


/**
 * Represents a manager for all map permissions
 */
public class MapPermissionManager extends ZPermissionManager {
    public static final Error ERR_NO_PERMISSION = ZPermissionManager.ERR_NO_PERMISSION;

    public MapPermissionManager(@NotNull PermissionsServiceFutureStub permissionService) {
        super(permissionService);
    }

    public @NotNull FutureResult<Void> addMapOwner(@NotNull String mapId, @NotNull String playerId) {
        var relationship = Relationship.newBuilder()
                .setResource(createMapObject(mapId))
                .setRelation("owner")
                .setSubject(createPlayerSubject(playerId))
                .build();
        return createRelationship(relationship);
    }

    public @NotNull FutureResult<Void> deleteMap(@NotNull String mapId) {
        var filter = Relationship.newBuilder()
                .setResource(createMapObject(mapId))
                .build();
        return deleteRelationship(filter);
    }

    public @NotNull FutureResult<Void> checkPermission(
            @NotNull String mapId, @NotNull String playerId,
            @NotNull @MagicConstant(valuesFromClass = MapData.class) String permission
    ) {
        var req = CheckPermissionRequest.newBuilder()
                .setResource(createMapObject(mapId))
                .setPermission(permission)
                .setSubject(createPlayerSubject(playerId))
                .build();
        return checkPermission(req);
    }

}
