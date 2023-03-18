package net.hollowcube.mapmaker.permission;

import com.authzed.api.v1.Core.ObjectReference;
import com.authzed.api.v1.Core.Relationship;
import com.authzed.api.v1.Core.RelationshipUpdate;
import com.authzed.api.v1.Core.SubjectReference;
import com.authzed.api.v1.PermissionService.*;
import com.authzed.api.v1.PermissionsServiceGrpc.PermissionsServiceFutureStub;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.jetbrains.annotations.NotNull;

/**
 * Contains a number of utilities for removing boilerplate and builder hell from the spicedb api.
 */
@SuppressWarnings("UnstableApiUsage")
class ZPermissionManager {
    private final PermissionsServiceFutureStub permissionService;

    ZPermissionManager(PermissionsServiceFutureStub permissionService) {
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

    protected @NotNull ListenableFuture<@NotNull String> createRelationship(@NotNull Relationship relationship) {
        var update = RelationshipUpdate.newBuilder()
                .setOperation(RelationshipUpdate.Operation.OPERATION_CREATE)
                .setRelationship(relationship)
                .build();
        return writeRelationshipUpdate(update);
    }

    protected @NotNull ListenableFuture<String> deleteRelationships(@NotNull RelationshipFilter filter) {
        var req = DeleteRelationshipsRequest.newBuilder()
                .setRelationshipFilter(filter)
                .build();
        return Futures.transform(
                permissionService.deleteRelationships(req),
                res -> res.getDeletedAt().getToken(),
                Runnable::run
        );
    }

    protected @NotNull ListenableFuture<@NotNull String> writeRelationshipUpdate(@NotNull RelationshipUpdate update) {
        var req = WriteRelationshipsRequest.newBuilder()
                .addUpdates(update)
                .build();
        return Futures.transform(
                permissionService.writeRelationships(req),
                res -> res.getWrittenAt().getToken(),
                Runnable::run
        );
    }

    public @NotNull ListenableFuture<Boolean> checkPermission(@NotNull CheckPermissionRequest req) {
        return Futures.transform(
                permissionService.checkPermission(req),
                res -> res.getPermissionship() == CheckPermissionResponse.Permissionship.PERMISSIONSHIP_HAS_PERMISSION,
                Runnable::run
        );
    }

}
