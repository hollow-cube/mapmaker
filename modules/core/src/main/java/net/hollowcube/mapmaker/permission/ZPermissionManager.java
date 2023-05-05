package net.hollowcube.mapmaker.permission;

import com.authzed.api.v1.Core.ObjectReference;
import com.authzed.api.v1.Core.Relationship;
import com.authzed.api.v1.Core.RelationshipUpdate;
import com.authzed.api.v1.Core.SubjectReference;
import com.authzed.api.v1.PermissionService.*;
import com.authzed.api.v1.PermissionsServiceGrpc.PermissionsServiceBlockingStub;
import com.google.common.util.concurrent.Futures;
import org.jetbrains.annotations.NotNull;

/**
 * Contains a number of utilities for removing boilerplate and builder hell from the spicedb api.
 */
@SuppressWarnings("UnstableApiUsage")
class ZPermissionManager {
    private final PermissionsServiceBlockingStub permissionService;

    ZPermissionManager(PermissionsServiceBlockingStub permissionService) {
        this.permissionService = permissionService;
    }

    protected SubjectReference createPlayerSubject(@NotNull String playerId) {
        return SubjectReference.newBuilder()
                .setObject(ObjectReference.newBuilder()
                        .setObjectType("mapmaker/player")
                        .setObjectId(playerId)
                        .build())
                .build();
    }

    protected ObjectReference createMapObject(@NotNull String mapId) {
        return ObjectReference.newBuilder()
                .setObjectType("mapmaker/map")
                .setObjectId(mapId)
                .build();
    }

    protected @NotNull String createRelationship(@NotNull Relationship relationship) {
        var update = RelationshipUpdate.newBuilder()
                .setOperation(RelationshipUpdate.Operation.OPERATION_CREATE)
                .setRelationship(relationship)
                .build();
        return writeRelationshipUpdate(update);
    }

    protected @NotNull String deleteRelationships(@NotNull RelationshipFilter filter) {
        var req = DeleteRelationshipsRequest.newBuilder()
                .setRelationshipFilter(filter)
                .build();
        return permissionService.deleteRelationships(req)
                .getDeletedAt().getToken();
    }

    protected @NotNull String writeRelationshipUpdate(@NotNull RelationshipUpdate update) {
        var req = WriteRelationshipsRequest.newBuilder()
                .addUpdates(update)
                .build();
        return permissionService.writeRelationships(req)
                .getWrittenAt().getToken();
    }

    public boolean checkPermission(@NotNull CheckPermissionRequest req) {
        var expected = CheckPermissionResponse.Permissionship.PERMISSIONSHIP_HAS_PERMISSION;
        var actual = permissionService.checkPermission(req).getPermissionship();
        return expected == actual;
    }

}
