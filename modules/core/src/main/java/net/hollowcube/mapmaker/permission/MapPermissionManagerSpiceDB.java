package net.hollowcube.mapmaker.permission;

import com.authzed.api.v1.Core.Relationship;
import com.authzed.api.v1.PermissionService.CheckPermissionRequest;
import com.authzed.api.v1.PermissionsServiceGrpc.PermissionsServiceFutureStub;
import com.google.common.util.concurrent.ListenableFuture;
import net.hollowcube.mapmaker.model.MapData;
import org.jetbrains.annotations.NotNull;


/**
 * Represents a manager for all map permissions
 */
public class MapPermissionManagerSpiceDB extends ZPermissionManager implements MapPermissionManager {

    public MapPermissionManagerSpiceDB(@NotNull PermissionsServiceFutureStub permissionService) {
        super(permissionService);
    }

    @Override
    public @NotNull ListenableFuture<@NotNull String> addMapOwner(@NotNull String mapId, @NotNull String playerId) {
        var relationship = Relationship.newBuilder()
                .setResource(createMapObject(mapId))
                .setRelation("owner")
                .setSubject(createPlayerSubject(playerId))
                .build();
        return createRelationship(relationship);
    }

    @Override
    public @NotNull ListenableFuture<@NotNull String> makeMapPublic(@NotNull String mapId) {
        var relationship = Relationship.newBuilder()
                .setResource(createMapObject(mapId))
                .setRelation("viewer")
                .setSubject(createPlayerSubject("*"))
                .build();
        return createRelationship(relationship);
    }

    @Override
    public @NotNull ListenableFuture<Void> deleteMap(@NotNull String mapId) {
        var filter = Relationship.newBuilder()
                .setResource(createMapObject(mapId))
                .build();
        return deleteRelationship(filter);
    }

    @Override
    public @NotNull ListenableFuture<Boolean> checkPermission(@NotNull String mapId, @NotNull String playerId, MapData.@NotNull Permission permission) {
        var req = CheckPermissionRequest.newBuilder()
                .setResource(createMapObject(mapId))
                .setPermission(permission.key())
                .setSubject(createPlayerSubject(playerId))
                .build();
        return checkPermission(req);
    }


}
