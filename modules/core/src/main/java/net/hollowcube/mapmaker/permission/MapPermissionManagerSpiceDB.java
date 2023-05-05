package net.hollowcube.mapmaker.permission;

import com.authzed.api.v1.Core.Relationship;
import com.authzed.api.v1.PermissionService.CheckPermissionRequest;
import com.authzed.api.v1.PermissionService.RelationshipFilter;
import com.authzed.api.v1.PermissionsServiceGrpc.PermissionsServiceBlockingStub;
import net.hollowcube.mapmaker.model.MapData;
import org.jetbrains.annotations.NotNull;


/**
 * Represents a manager for all map permissions
 */
public class MapPermissionManagerSpiceDB extends ZPermissionManager implements MapPermissionManager {

    public MapPermissionManagerSpiceDB(@NotNull PermissionsServiceBlockingStub permissionService) {
        super(permissionService);
    }

    @Override
    public @NotNull String addMapOwner(@NotNull String mapId, @NotNull String playerId) {
        var relationship = Relationship.newBuilder()
                .setResource(createMapObject(mapId))
                .setRelation("owner")
                .setSubject(createPlayerSubject(playerId))
                .build();
        return createRelationship(relationship);
    }

    @Override
    public @NotNull String makeMapPublic(@NotNull String mapId) {
        var relationship = Relationship.newBuilder()
                .setResource(createMapObject(mapId))
                .setRelation("viewer")
                .setSubject(createPlayerSubject("*"))
                .build();
        return createRelationship(relationship);
    }

    @Override
    public @NotNull String deleteMap(@NotNull String mapId) {
        var filter = RelationshipFilter.newBuilder()
                .setResourceType("mapmaker/map")
                .setOptionalResourceId(mapId)
                .build();
        return deleteRelationships(filter);
    }

    @Override
    public boolean checkPermission(@NotNull String mapId, @NotNull String playerId, MapData.@NotNull Permission permission) {
        var req = CheckPermissionRequest.newBuilder()
                .setResource(createMapObject(mapId))
                .setPermission(permission.key())
                .setSubject(createPlayerSubject(playerId))
                .build();
        return checkPermission(req);
    }


}
