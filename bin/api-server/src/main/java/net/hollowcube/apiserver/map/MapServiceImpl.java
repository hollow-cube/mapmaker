package net.hollowcube.apiserver.map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.hollowcube.apiserver.common.NatsPublisher;
import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.apiserver.db.Maps;
import net.hollowcube.apiserver.db.MapsQueries;
import net.hollowcube.apiserver.job.IndexMap;
import net.hollowcube.apiserver.job.JobSpec;
import net.hollowcube.apiserver.player.Roles;
import net.hollowcube.apiserver.s3.S3Client;
import net.hollowcube.ipc.Blob;
import net.hollowcube.ipc.Wire;
import net.hollowcube.ipc.map.*;
import net.hollowcube.posthog.PostHogClient;
import net.hollowcube.sqlgen.runtime.Jdbc;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.params.ScanParams;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;

import static java.util.Objects.requireNonNullElse;
import static net.hollowcube.apiserver.map.MapCompat.*;
import static net.hollowcube.ipc.util.IpcException.badRequest;
import static net.hollowcube.ipc.util.IpcException.notFound;
import static net.hollowcube.ipc.util.IpcException.unavailable;

/// Map writes: creation, settings, the world object, builders, verification and publishing.
///
/// The Go api-server still serves the same tables, so every column keeps Go's spelling and every
/// event keeps Go's shape. What changed is that the rules the servers used to enforce for
/// themselves — slots, sizes, what a map needs before it can publish — are enforced here, under
/// the row lock, and answered as a result the caller can show rather than a 400.
public final class MapServiceImpl implements MapService {

    private static final int PUBLISH_ATTEMPTS = 10;

    private final ApiDatabase db;
    private final S3Client worlds;
    private final NatsPublisher nats;
    private final JedisPooled redis;
    private final PostHogClient posthog;
    private final Duration minimumBuildTime;
    private final Duration drainTimeout;
    private final LongSupplier publishedIds;

    public MapServiceImpl(
        ApiDatabase db,
        S3Client worlds,
        NatsPublisher nats,
        JedisPooled redis,
        PostHogClient posthog,
        Duration minimumBuildTime
    ) {
        this(
            db,
            worlds,
            nats,
            redis,
            posthog,
            minimumBuildTime,
            Duration.ofSeconds(30),
            () -> ThreadLocalRandom.current().nextLong(1, 1_000_000_000)
        );
    }

    @TestOnly
    MapServiceImpl(
        ApiDatabase db,
        S3Client worlds,
        NatsPublisher nats,
        JedisPooled redis,
        PostHogClient posthog,
        Duration minimumBuildTime,
        Duration drainTimeout,
        LongSupplier publishedIds
    ) {
        this.db = db;
        this.worlds = worlds;
        this.nats = nats;
        this.redis = redis;
        this.posthog = posthog;
        this.minimumBuildTime = minimumBuildTime;
        this.drainTimeout = drainTimeout;
        this.publishedIds = publishedIds;
    }

    @Override
    public CreateMapResult create(UUID owner, MapSize size, int protocolVersion) {
        if (size == MapSize.UNKNOWN || protocolVersion <= 0)
            throw badRequest("invalid map size or protocol version");
        return db.txResult(tx -> {
            var player = tx.maps.getPlayerForUpdate(owner);
            if (player == null) return new CreateMapResult.NotFound();

            var flags = Roles.flags(player.role(), player.hypercubeEnd());
            if (!Roles.maxMapSize(player.maxMapSize(), flags).unlocks(size))
                return new CreateMapResult.SizeLocked();
            var limit = Roles.mapSlots(player.extraMapSlots(), flags);
            if (tx.maps.countSlots(owner) >= limit) return new CreateMapResult.NoSlots(limit);

            var map = tx.maps.createMap(UUID.randomUUID(), owner, size.id(), protocolVersion);
            tx.maps.insertOwnerSlot(owner, map.id());
            tx.afterCommit(
                () -> posthog.capture(
                    owner.toString(),
                    "map_created",
                    Map.of("size", size.name().toLowerCase(Locale.ROOT))
                )
            );
            return new CreateMapResult.Success(mapData(tx.maps, map));
        });
    }

    @Override
    public @Nullable MapData get(String idOrPublishedId) {
        var map = switch (MapRef.parse(idOrPublishedId)) {
            case MapRef.Uuid(var id) -> db.maps.getMap(id);
            case MapRef.Published(var id) -> db.maps.getMapByPublishedId(id);
            case null -> null;
        };
        if (map == null) return null;
        return mapData(db.maps, map);
    }

    @Override
    public void update(UUID mapId, MapPatch patch) {
        patch.validate();
        db.tx(tx -> {
            var map = tx.maps.getMapForUpdate(mapId);
            if (map == null) throw notFound("map not found");

            // The dedicated boolean columns are presented to callers as keys of `extra`, so a patch
            // to `extra` is merged over the current view and the booleans are split back out.
            var extra = extra(map);
            if (patch.extra() != null)
                for (var entry : patch.extra().entrySet())
                    extra.add(entry.getKey(), entry.getValue().deepCopy());
            var onlySprint = takeBoolean(extra, "only_sprint");
            var noSprint = takeBoolean(extra, "no_sprint");
            var noJump = takeBoolean(extra, "no_jump");
            var noSneak = takeBoolean(extra, "no_sneak");
            var boat = takeBoolean(extra, "boat");

            // A null in a patch is "unchanged", so `MapPatch.Builder` spells clearing the
            // subvariant as "none".
            var subvariant = patch.subvariant() == null ? map.optSubvariant() : patch.subvariant();
            if ("none".equals(subvariant)) subvariant = null;
            var variant = patch.variant() == null
                ? map.optVariant()
                : patch.variant().name().toLowerCase(Locale.ROOT);
            var spawnPoint = patch.spawnPoint() == null
                ? map.optSpawnPoint()
                : Wire.gson().toJson(patch.spawnPoint());
            var leaderboard = patch.leaderboard() == null
                ? map.leaderboard()
                : leaderboardColumn(patch.leaderboard());
            var quality = patch.qualityOverride() == null
                ? requireNonNullElse(map.qualityOverride(), 0L)
                : (long) patch.qualityOverride().ordinal();
            var protocolVersion = requireNonNullElse(
                patch.protocolVersion(),
                requireNonNullElse(map.protocolVersion(), DEFAULT_PROTOCOL_VERSION)
            );
            tx.maps.updateMap(
                new MapsQueries.UpdateMapParams(
                    requireNonNullElse(patch.name(), requireNonNullElse(map.optName(), "")),
                    requireNonNullElse(patch.icon(), requireNonNullElse(map.optIcon(), "")),
                    patch.size() == null ? map.size() : patch.size().id(),
                    variant,
                    subvariant,
                    spawnPoint,
                    leaderboard,
                    extra.toString().getBytes(StandardCharsets.UTF_8),
                    onlySprint,
                    noSprint,
                    noJump,
                    noSneak,
                    boat,
                    requireNonNullElse(patch.listed(), map.listed()),
                    quality,
                    protocolVersion,
                    mapId
                )
            );

            if (patch.tags() != null) {
                var known = new HashSet<>(tx.maps.listKnownTags());
                if (!known.containsAll(patch.tags())) throw badRequest("unknown map tag");
                tx.maps.deleteTags(mapId);
                tx.maps.insertTags(mapId, patch.tags());
            }

            // Runs recorded against a published building map are meaningless once it is a parkour
            // map, but only the ones still going: a completed run is somebody's record.
            var becomesParkour = patch.variant() == MapVariant.PARKOUR
                && variant(map) != MapVariant.PARKOUR;
            if (map.publishedAt() != null && becomesParkour) tx.maps.deleteInProgressStates(mapId);
            tx.afterCommit(this::invalidateSearch);
        });
    }

    @Override
    public void delete(UUID actorId, UUID mapId, @Nullable String reason) {
        db.tx(tx -> {
            var map = tx.maps.getMapForUpdate(mapId);
            if (map == null) {
                if (tx.maps.getMapIncludingDeleted(mapId) == null) throw notFound("map not found");
                return;
            }
            var reasonRequired = map.publishedId() != null || !map.owner().equals(actorId);
            if (reasonRequired && (reason == null || reason.isBlank()))
                throw badRequest("a deletion reason is required");
            tx.maps.deleteAllStates(mapId);
            tx.maps.deleteMap(actorId, reasonRequired ? reason : USER_DELETION, mapId);
            tx.maps.removeSlots(mapId);
            tx.maps.removeLegacyBuilders(mapId);
            deleteInvites(tx, mapId, null);
            tx.afterCommit(
                () -> nats.publish(MAP_DELETE_SUBJECT, mapEvent(MAP_ACTION_DELETE, mapId))
            );
            tx.afterCommit(this::invalidateSearch);
        });
    }

    @Override
    public Blob getWorld(UUID mapId) {
        try {
            return worlds.get(mapId.toString());
        } catch (S3Client.NotFoundError e) {
            throw notFound("map world not found");
        }
    }

    @Override
    public void updateWorld(UUID mapId, long loadTime, Blob world) {
        try (world) {
            // Uploaded under the row lock so that publish and the pending transition, which take
            // the same lock, wait for an in-flight save rather than racing it.
            db.tx(tx -> {
                var map = tx.maps.getMapForUpdate(mapId);
                if (map == null) throw world.refuse(404, "map not found");
                if (verification(map) == MapVerification.PENDING)
                    throw world.refuse(409, "map is being verified");
                if (map.publishedAt() != null && loadTime < map.publishedAt().toEpochMilli())
                    throw world.refuse(409, "map was published after this world was loaded");
                worlds.put(mapId.toString(), world.stream(), world.length());
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public MapStatus getStatus(UUID mapId) {
        var map = db.maps.getMap(mapId);
        if (map == null) return new MapStatus.NotFound();
        return status(map, readiness(db.maps, map));
    }

    @Override
    public PublishMapResult publish(UUID mapId) {
        // A published id is drawn at random and unique, so a collision aborts the transaction and
        // the whole thing is tried again with a fresh draw.
        for (int attempt = 0; attempt < PUBLISH_ATTEMPTS; attempt++) {
            try {
                return db.txResult(tx -> {
                    var map = tx.maps.getMapForUpdate(mapId);
                    if (map == null) return new PublishMapResult.NotFound();
                    if (map.publishedId() != null)
                        return new PublishMapResult.Success(mapData(tx.maps, map));
                    var readiness = readiness(tx.maps, map);
                    if (!readiness.ready()) return new PublishMapResult.Blocked(readiness);

                    var publishedId = publishedIds.getAsLong();
                    var published = tx.maps.publishMap(publishedId, mapId);
                    tx.maps.removeSlots(mapId);
                    deleteInvites(tx, mapId, null);
                    JobSpec.INDEX_MAP.enqueue(tx.jobs, new IndexMap(mapId.toString(), "published"));

                    var properties = new HashMap<String, Object>();
                    properties.put("map_id", mapId.toString());
                    properties.put("published_map_id", publishedId);
                    properties.put("map_name", requireNonNullElse(map.optName(), ""));
                    properties.put("variant", map.optVariant());
                    properties.put("sub_variant", requireNonNullElse(map.optSubvariant(), ""));
                    properties.put("owner_build_time", readiness.buildTime());
                    properties.put("world_data_size", worlds.stat(mapId.toString()));
                    properties.put(
                        "contest",
                        map.contest() == null ? null : map.contest().toString()
                    );
                    tx.afterCommit(
                        () -> posthog.capture(map.owner().toString(), "map_published", properties)
                    );
                    tx.afterCommit(this::invalidateSearch);

                    return new PublishMapResult.Success(mapData(tx.maps, published));
                });
            } catch (Exception e) {
                if (!Jdbc.hasState(e, Jdbc.UNIQUE_VIOLATION)) throw e;
            }
        }
        throw unavailable("could not allocate a published map ID");
    }

    @Override
    public BeginVerificationResult beginVerification(UUID mapId) {
        var answer = answerWithoutDrain(db.maps.getMap(mapId));
        if (answer != null) return answer;

        // The editor is asked to save and unregister first, and the pending flag is only set once
        // it has, so that the verification run plays the world the builder last saw.
        var drain = mapEvent(MAP_ACTION_DRAIN, mapId);
        drain.addProperty("drainReason", "verification");
        nats.publish(MAP_DRAIN_SUBJECT, drain);
        var deadline = System.nanoTime() + drainTimeout.toNanos();
        while (db.maps.countWorlds(mapId) != 0) {
            if (System.nanoTime() >= deadline) return BeginVerificationResult.DRAIN_TIMEOUT;
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw unavailable("verification interrupted");
            }
        }

        return db.txResult(tx -> {
            var decided = answerWithoutDrain(tx.maps.getMapForUpdate(mapId));
            if (decided != null) return decided;
            tx.maps.updateVerification(VERIFICATION_PENDING, mapId);
            return BeginVerificationResult.READY;
        });
    }

    @Override
    public DeleteVerificationResult deleteVerification(UUID mapId) {
        return db.txResult(tx -> {
            var map = tx.maps.getMapForUpdate(mapId);
            if (map == null) return DeleteVerificationResult.MAP_NOT_FOUND;
            if (map.publishedAt() != null) return DeleteVerificationResult.MAP_PUBLISHED;
            tx.maps.deleteVerifyingStates(mapId);
            tx.maps.updateVerification(VERIFICATION_NONE, mapId);
            tx.afterCommit(
                () -> redis.del(
                    VERIFICATION_LEADERBOARD_PREFIX + mapId + VERIFICATION_LEADERBOARD_SUFFIX
                )
            );
            return DeleteVerificationResult.RESET;
        });
    }

    @Override
    public List<MapSlot> getPlayerSlots(UUID playerId) {
        var slots = new ArrayList<MapSlot>();
        for (var slot : db.maps.listPlayerSlots(playerId)) {
            var map = db.maps.getMap(slot.mapId());
            if (map == null) continue;
            slots.add(
                new MapSlot(
                    mapData(db.maps, map),
                    slot.createdAt(),
                    map.owner().equals(playerId),
                    builders(db.maps, map, false)
                )
            );
        }
        for (var map : db.maps.listPlayerPublishedMaps(playerId))
            slots.add(
                new MapSlot(
                    mapData(db.maps, map),
                    Objects.requireNonNull(map.publishedAt(), "publishedAt"),
                    true,
                    List.of()
                )
            );
        return slots;
    }

    /// Owner included, unlike the builders on a [MapSlot] or a [BuilderResult]: a map server
    /// asks this to decide whether anyone who may edit the map is still in it.
    @Override
    public List<MapBuilder> getBuilders(UUID mapId, boolean onlyActive) {
        if (db.maps.getMap(mapId) == null) throw notFound("map not found");
        return db.maps.listBuilders(mapId)
            .stream()
            .filter(slot -> !onlyActive || !slot.isPending())
            .map(MapCompat::builder)
            .toList();
    }

    @Override
    public BuilderResult inviteBuilder(UUID mapId, UUID playerId) {
        var unlocked = db.maps.getMap(mapId);
        if (unlocked == null) return new BuilderResult.MapNotFound();
        return db.txResult(tx -> {
            // Both players' rows are locked in id order before the map, which is the order every
            // other builder operation takes them in.
            var players = tx.maps.getPlayersForUpdate(List.of(unlocked.owner(), playerId));
            var owner = find(players, unlocked.owner());
            var target = find(players, playerId);
            if (owner == null || target == null) return new BuilderResult.PlayerNotFound();

            var map = tx.maps.getMapForUpdate(mapId);
            if (map == null) return new BuilderResult.MapNotFound();
            if (map.publishedAt() != null) return new BuilderResult.MapPublished();
            if (map.owner().equals(playerId)) return new BuilderResult.Owner();
            var slot = tx.maps.getBuilderSlot(mapId, playerId);
            if (slot != null && !slot.isPending()) return new BuilderResult.AlreadyBuilder();
            if (slot != null) return new BuilderResult.AlreadyDone(builders(tx.maps, map, false));
            if (!allowsInvites(target.settings())) return new BuilderResult.InvitesDisabled();
            var limit = Roles.builderSlots(
                owner.mapBuilders(),
                Roles.flags(owner.role(), owner.hypercubeEnd())
            );
            if (builders(tx.maps, map, false).size() >= limit)
                return new BuilderResult.CapacityReached(limit);

            tx.maps.inviteBuilder(mapId, playerId);
            deleteInvites(tx, mapId, playerId);
            var data = new JsonObject();
            data.addProperty("inviterId", map.owner().toString());
            data.addProperty("mapId", mapId.toString());
            notify(tx, playerId, BUILDER_INVITE_NOTIFICATION, inviteKey(mapId), data);
            return new BuilderResult.Success(builders(tx.maps, map, false));
        });
    }

    @Override
    public BuilderResult acceptBuilderInvite(UUID mapId, UUID playerId) {
        return db.txResult(tx -> {
            var player = tx.maps.getPlayerForUpdate(playerId);
            if (player == null) return new BuilderResult.PlayerNotFound();
            var map = tx.maps.getMapForUpdate(mapId);
            if (map == null) return new BuilderResult.MapNotFound();
            if (map.publishedAt() != null) return new BuilderResult.MapPublished();
            if (map.owner().equals(playerId)) return new BuilderResult.Owner();
            var slot = tx.maps.getBuilderSlot(mapId, playerId);
            if (slot == null) return new BuilderResult.InviteGone();
            if (!slot.isPending())
                return new BuilderResult.AlreadyDone(builders(tx.maps, map, false));
            var limit = Roles.mapSlots(
                player.extraMapSlots(),
                Roles.flags(player.role(), player.hypercubeEnd())
            );
            if (tx.maps.countSlots(playerId) >= limit) return new BuilderResult.NoSlots(limit);

            tx.maps.acceptBuilder(mapId, playerId);
            deleteInvites(tx, mapId, playerId);
            return new BuilderResult.Success(builders(tx.maps, map, false));
        });
    }

    @Override
    public BuilderResult removeBuilder(UUID mapId, UUID playerId) {
        return removeBuilder(mapId, playerId, false);
    }

    @Override
    public BuilderResult rejectBuilderInvite(UUID mapId, UUID playerId) {
        return removeBuilder(mapId, playerId, true);
    }

    @Override
    public void report(
        UUID mapId,
        UUID reporter,
        List<MapReportCategory> categories,
        @Nullable String comment
    ) {
        if (categories.isEmpty()
            || categories.contains(MapReportCategory.UNKNOWN)
            || new HashSet<>(categories).size() != categories.size())
            throw badRequest("invalid report categories");
        if (categories.contains(MapReportCategory.UNPLAYABLE)
            && (comment == null || comment.isBlank()))
            throw badRequest("unplayable reports require a comment");

        // Reporting a map for its content is also a dislike of it; reporting the map's runs or its
        // playability says nothing about whether the reporter liked it.
        var dislike = categories.stream().anyMatch(
            c -> c != MapReportCategory.CHEATED && c != MapReportCategory.UNPLAYABLE
        );
        db.tx(tx -> {
            var map = tx.maps.getMap(mapId);
            if (map == null) throw notFound("map not found");
            tx.maps.insertReport(
                mapId,
                reporter,
                categories.stream().map(Enum::ordinal).toList(),
                comment
            );
            if (dislike) tx.maps.setRating(mapId, reporter, MapRating.DISLIKED.ordinal());

            var properties = new HashMap<String, Object>();
            properties.put("map_id", mapId.toString());
            properties.put("map_name", requireNonNullElse(map.optName(), ""));
            properties.put(
                "categories",
                categories.stream().map(c -> c.name().toLowerCase(Locale.ROOT)).toList()
            );
            properties.put("comment", requireNonNullElse(comment, ""));
            tx.afterCommit(() -> posthog.capture(reporter.toString(), "map_reported", properties));
        });
    }

    @Override
    public MapRating getPlayerRating(UUID mapId, UUID playerId) {
        var rating = db.maps.getRating(mapId, playerId);
        if (rating == null || rating < 0 || rating >= MapRating.UNKNOWN.ordinal())
            return MapRating.UNRATED;
        return MapRating.values()[rating];
    }

    @Override
    public void setPlayerRating(UUID mapId, UUID playerId, MapRating rating) {
        if (rating == MapRating.UNKNOWN) throw badRequest("invalid rating");
        if (db.maps.getMap(mapId) == null) throw notFound("map not found");
        db.maps.setRating(mapId, playerId, rating.ordinal());
    }

    /// What [#beginVerification] answers without draining anything, or null for a map that is
    /// verifiable and not yet verified, which is the one case that needs the drain.
    private static @Nullable BeginVerificationResult answerWithoutDrain(@Nullable Maps map) {
        if (map == null) return BeginVerificationResult.MAP_NOT_FOUND;
        if (map.publishedAt() != null) return BeginVerificationResult.MAP_PUBLISHED;
        if (variant(map) == MapVariant.BUILDING) return BeginVerificationResult.NOT_VERIFIABLE;
        if (verification(map) != MapVerification.UNVERIFIED) return BeginVerificationResult.READY;
        return null;
    }

    private BuilderResult removeBuilder(UUID mapId, UUID playerId, boolean reject) {
        return db.txResult(tx -> {
            var map = tx.maps.getMapForUpdate(mapId);
            if (map == null) return new BuilderResult.MapNotFound();
            if (map.publishedAt() != null) return new BuilderResult.MapPublished();
            if (map.owner().equals(playerId)) return new BuilderResult.Owner();
            var slot = tx.maps.getBuilderSlot(mapId, playerId);
            if (reject && slot != null && !slot.isPending())
                return new BuilderResult.AlreadyBuilder();
            deleteInvites(tx, mapId, playerId);
            if (slot != null) {
                tx.maps.removeBuilder(mapId, playerId);
                if (reject) {
                    var data = new JsonObject();
                    data.addProperty("mapId", mapId.toString());
                    data.addProperty("builderId", playerId.toString());
                    notify(tx, map.owner(), BUILDER_REJECTED_NOTIFICATION, "", data);
                }
            }
            if (!reject) {
                // A removed builder may be standing in the map right now.
                var transfer = new JsonObject();
                transfer.addProperty("playerId", playerId.toString());
                transfer.addProperty("from", mapId.toString());
                transfer.addProperty("to", "hub");
                transfer.addProperty("state", "playing");
                tx.afterCommit(() -> nats.publish(PLAYER_TRANSFER_SUBJECT, transfer));
            }
            var builders = builders(tx.maps, map, false);
            return slot == null
                ? new BuilderResult.AlreadyDone(builders)
                : new BuilderResult.Success(builders);
        });
    }

    private static MapData mapData(MapsQueries queries, Maps map) {
        var stats = map.publishedAt() == null ? null : queries.getStats(map.id());
        return MapCompat.mapData(map, queries.getTags(map.id()), stats);
    }

    private PublishReadiness readiness(MapsQueries queries, Maps map) {
        var required = minimumBuildTime.toMillis();
        if (map.publishedAt() != null) return new PublishReadiness(List.of(), 0, required);
        var missing = new ArrayList<PublishRequirement>();
        try {
            worlds.stat(map.id().toString());
        } catch (S3Client.NotFoundError e) {
            missing.add(PublishRequirement.WORLD);
        }
        var buildTime = requireNonNullElse(queries.getLatestEditingTime(map.id(), map.owner()), 0L);
        if (buildTime < required) missing.add(PublishRequirement.BUILD_TIME);
        if (variant(map) != MapVariant.BUILDING && verification(map) != MapVerification.VERIFIED)
            missing.add(PublishRequirement.VERIFICATION);
        if (map.optName() == null || map.optName().isBlank()) missing.add(PublishRequirement.NAME);
        if (map.optIcon() == null || map.optIcon().isBlank()) missing.add(PublishRequirement.ICON);
        if (queries.getTags(map.id()).isEmpty()) missing.add(PublishRequirement.TAGS);
        return new PublishReadiness(missing, buildTime, required);
    }

    private static MapStatus status(Maps map, PublishReadiness readiness) {
        if (map.publishedAt() != null)
            return new MapStatus.Published(
                MapData.formatPublishedId(Objects.requireNonNull(map.publishedId(), "publishedId")),
                map.publishedAt()
            );
        if (readiness.ready()) return new MapStatus.ReadyToPublish();
        if (verification(map) == MapVerification.PENDING) return new MapStatus.Verifying(readiness);
        return new MapStatus.Draft(readiness);
    }

    private static boolean takeBoolean(JsonObject extra, String key) {
        var value = extra.remove(key);
        if (value == null) return false;
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean())
            throw badRequest(key + " must be boolean");
        return value.getAsBoolean();
    }

    private static List<MapBuilder> builders(MapsQueries queries, Maps map, boolean onlyActive) {
        return queries.listBuilders(map.id())
            .stream()
            .filter(slot -> !slot.playerId().equals(map.owner()))
            .filter(slot -> !onlyActive || !slot.isPending())
            .map(MapCompat::builder)
            .toList();
    }

    private static MapsQueries.@Nullable GetPlayersForUpdateRow find(
        List<MapsQueries.GetPlayersForUpdateRow> players,
        UUID id
    ) {
        for (var player : players) {
            if (player.id().equals(id)) return player;
        }
        return null;
    }

    private static boolean allowsInvites(String settings) {
        var setting = JsonParser.parseString(settings)
            .getAsJsonObject()
            .get("allow_builder_invites");
        return setting == null || !setting.isJsonPrimitive() || setting.getAsBoolean();
    }

    private void deleteInvites(ApiDatabase.Tx tx, UUID mapId, @Nullable UUID player) {
        for (var notification : tx.maps.deleteNotifications(
            BUILDER_INVITE_NOTIFICATION,
            inviteKey(mapId),
            player
        )) {
            var event = notificationEvent(
                notification.playerId(),
                "delete",
                notification.type(),
                notification.key(),
                null
            );
            tx.afterCommit(() -> nats.publish(NOTIFICATION_DELETED_SUBJECT, event));
        }
    }

    private void notify(
        ApiDatabase.Tx tx,
        UUID playerId,
        String type,
        String key,
        JsonObject data
    ) {
        tx.maps.insertNotification(UUID.randomUUID(), playerId, type, key, data.toString());
        var event = notificationEvent(playerId, "create", type, key, data);
        tx.afterCommit(() -> nats.publish(NOTIFICATION_CREATED_SUBJECT, event));
    }

    private void invalidateSearch() {
        var cursor = "0";
        var params = new ScanParams().match(SEARCH_CACHE_PATTERN).count(100);
        do {
            var page = redis.scan(cursor, params);
            if (!page.getResult().isEmpty()) redis.del(page.getResult().toArray(String[]::new));
            cursor = page.getCursor();
        } while (!"0".equals(cursor));
    }

}
