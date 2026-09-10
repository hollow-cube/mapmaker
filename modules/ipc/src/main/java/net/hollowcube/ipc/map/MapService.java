package net.hollowcube.ipc.map;

import net.hollowcube.ipc.Blob;
import net.hollowcube.ipc.PaginatedList;
import net.hollowcube.ipc.util.Ipc;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/// Maps, ported from Go's `/v4/internal/maps`: the map row, its world object, builders,
/// verification and publishing, plus the listing reads (search, progress, history). Save states
/// and leaderboards are still Go's and stay on core's `MapClient.Http`.
@Ipc
public interface MapService {

    CreateMapResult create(UUID owner, MapSize size, int protocolVersion);

    /// By id, or by the `000-000-000` published id.
    @Nullable
    MapData get(String idOrPublishedId);

    void update(UUID mapId, MapPatch patch);

    /// Listed published maps only.
    PaginatedList<MapData> search(MapSearch search);

    /// One entry per map of `mapIds` the player has a playing or verifying save state on; a map
    /// never started has none.
    List<PlayerMapProgress> progress(UUID playerId, List<UUID> mapIds);

    /// The maps the player has played, most recently played first. A map deleted since is left
    /// out rather than returned as a hole.
    PaginatedList<MapData> history(UUID playerId, int page, int pageSize);

    /// A reason is required unless the owner is deleting their own unpublished map.
    void delete(UUID actorId, UUID mapId, @Nullable String reason);

    Blob getWorld(UUID mapId);

    /// `loadTime` is when the uploading server loaded the world; a world loaded before the map was
    /// published is refused, as is any upload while verification is pending.
    void updateWorld(UUID mapId, long loadTime, Blob world);

    MapStatus getStatus(UUID mapId);

    PublishMapResult publish(UUID mapId);

    /// Drains the editor and marks the map pending; blocks for up to the drain timeout.
    BeginVerificationResult beginVerification(UUID mapId);

    DeleteVerificationResult deleteVerification(UUID mapId);

    List<MapSlot> getPlayerSlots(UUID playerId);

    List<MapBuilder> getBuilders(UUID mapId, boolean onlyActive);

    BuilderResult inviteBuilder(UUID mapId, UUID playerId);

    BuilderResult removeBuilder(UUID mapId, UUID playerId);

    BuilderResult acceptBuilderInvite(UUID mapId, UUID playerId);

    BuilderResult rejectBuilderInvite(UUID mapId, UUID playerId);

    void report(
        UUID mapId,
        UUID reporter,
        List<MapReportCategory> categories,
        @Nullable String comment
    );

    MapRating getPlayerRating(UUID mapId, UUID playerId);

    void setPlayerRating(UUID mapId, UUID playerId, MapRating rating);
}
