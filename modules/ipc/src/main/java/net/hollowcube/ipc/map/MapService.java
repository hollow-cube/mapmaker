package net.hollowcube.ipc.map;

import net.hollowcube.ipc.Blob;
import net.hollowcube.ipc.PaginatedList;
import net.hollowcube.ipc.util.Ipc;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/// Maps, ported from Go's `/v4/internal/maps`: the map row, its world object, builders,
/// verification and publishing, plus the listing reads (search, progress, history); then save
/// states, the checkpoint every map server writes for every player; then leaderboards.
///
/// A map's board is a redis sorted set of every player's best completed run, kept by the save
/// state write and derived from `save_states`, which stays the record: [#rebuildLeaderboard]
/// rewrites it from the rows whenever the two disagree.
///
/// The leaderboard deletes are what staff use to strike times. They soft-delete the runs, so each
/// has an undelete that reverses it: a run comes back when its deletion stamp lies in the given
/// range, both ends optional and inclusive. Without a range everything ever soft-deleted in scope
/// comes back, including runs a map deletion or a verification reset put away, so a staff mistake
/// is undone by its time and not by scope alone.
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

    /// The player's newest state of this type on the map, or null when there is none or the newest
    /// is completed, in which case the caller starts a new one under an id of its own.
    @Nullable
    SaveStateData getLatestSaveState(UUID mapId, UUID playerId, SaveStateType type);

    /// The player's best completed run on the map by its leaderboard's direction, or null.
    @Nullable
    SaveStateData getBestSaveState(UUID mapId, UUID playerId);

    /// Inserts on first sight. 404 for an unknown map; 409 for a state already completed, which
    /// never changes again.
    void upsertSaveState(UUID mapId, UUID playerId, UUID stateId, SaveStateUpdate update);

    /// The top ten and, when `playerId` is given, that player's own standing. Empty for a map
    /// nobody has finished.
    LeaderboardData getMapLeaderboard(UUID mapId, @Nullable UUID playerId);

    /// With `playerId`, only that player's score, at rank -1; without, the top ten.
    LeaderboardData getGlobalLeaderboard(GlobalLeaderboard board, @Nullable UUID playerId);

    /// Every published map the player has finished, best placement first.
    PaginatedList<PlayerTopTime> getPlayerTopTimes(UUID playerId, int page, int pageSize);

    /// The player's runs on the map. `notify` tells them.
    void deleteLeaderboardEntry(UUID mapId, UUID playerId, boolean notify);

    /// The player's runs on every map.
    void deletePlayerLeaderboardEntries(UUID playerId);

    /// Everyone's runs on the map.
    void deleteLeaderboard(UUID mapId);

    /// Runs restored.
    int undeleteLeaderboardEntry(
        UUID mapId,
        UUID playerId,
        @Nullable Instant deletedAfter,
        @Nullable Instant deletedBefore
    );

    int undeletePlayerLeaderboardEntries(
        UUID playerId,
        @Nullable Instant deletedAfter,
        @Nullable Instant deletedBefore
    );

    int undeleteLeaderboard(
        UUID mapId,
        @Nullable Instant deletedAfter,
        @Nullable Instant deletedBefore
    );

    /// Rewrites the map's board from its completed runs. 404 for an unknown map; nothing for a map
    /// without a board.
    void rebuildLeaderboard(UUID mapId);
}
