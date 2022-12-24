package net.hollowcube.mapmaker.permission;

import com.authzed.api.v1.Core.ObjectReference;
import com.authzed.api.v1.Core.Relationship;
import com.authzed.api.v1.Core.RelationshipUpdate;
import com.authzed.api.v1.Core.SubjectReference;
import com.authzed.api.v1.PermissionService.CheckPermissionRequest;
import com.authzed.api.v1.PermissionService.CheckPermissionResponse;
import com.authzed.api.v1.PermissionService.WriteRelationshipsRequest;
import com.authzed.api.v1.PermissionsServiceGrpc.PermissionsServiceFutureStub;
import net.hollowcube.common.result.Error;
import net.hollowcube.common.result.FutureResult;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Contains a number of utilities for removing boilerplate and builder hell from the spicedb api.
 */
class ZPermissionManager {
    public static final Error ERR_NO_PERMISSION = Error.of("no permission");

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

    protected @NotNull FutureResult<Void> createRelationship(@NotNull Relationship relationship) {
        var update = RelationshipUpdate.newBuilder()
                .setOperation(RelationshipUpdate.Operation.OPERATION_CREATE)
                .setRelationship(relationship)
                .build();
        var req = WriteRelationshipsRequest.newBuilder()
                .addUpdates(update)
                .build();
        var asyncRes = permissionService.writeRelationships(req);
        var future = new CompletableFuture<Void>();
        asyncRes.addListener(() -> {
            try {
                asyncRes.get();
                //todo check response
                future.complete(null);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        }, Runnable::run);
        return FutureResult.wrap(future);
    }

    public @NotNull FutureResult<Void> checkPermission(@NotNull CheckPermissionRequest req) {
        var asyncRes = permissionService.checkPermission(req);
        var future = new CompletableFuture<CheckPermissionResponse>();
        asyncRes.addListener(() -> {
            try {
                future.complete(asyncRes.get());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        }, Runnable::run);
        return FutureResult.wrap(future).flatMap(res -> {
            if (res.getPermissionship() == CheckPermissionResponse.Permissionship.PERMISSIONSHIP_HAS_PERMISSION)
                return FutureResult.ofNull();
            return FutureResult.error(ERR_NO_PERMISSION);
        });
    }

}
