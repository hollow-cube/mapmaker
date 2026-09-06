package net.hollowcube.ipc.map;

import com.google.gson.JsonObject;
import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.ipc.util.Position;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RuntimeGson
public record MapData(
    UUID id,
    UUID owner,
    int protocolVersion,
    Settings settings,
    MapVerification verification,
    @Nullable String publishedId,
    @Nullable Instant publishedAt,
    boolean listed,
    MapQuality quality,
    MapDifficulty difficulty,
    long uniquePlays,
    double clearRate,
    int likes,
    @Nullable UUID contest,
    Instant createdAt,
    Instant lastModified
) {
    public static final String DEFAULT_NAME = "Untitled Map";
    public static final int MAX_NAME_LENGTH = 20;
    public static final int MIN_PLAYS_FOR_DIFFICULTY = 10;
    public static final String SPAWN_MAP_ID = "b210fa07-d64c-4100-a2db-1426c35b7533";

    public static MapData draft(UUID id, UUID owner) {
        return new MapData(
            id,
            owner,
            0,
            Settings.defaults(),
            MapVerification.UNVERIFIED,
            null,
            null,
            true,
            MapQuality.UNRATED,
            MapDifficulty.UNRATED,
            0,
            0,
            0,
            null,
            Instant.EPOCH,
            Instant.EPOCH
        );
    }

    public String name() {
        return settings.name().isEmpty() ? DEFAULT_NAME : settings.name();
    }

    public boolean needsVerification() {
        return settings.variant() != MapVariant.BUILDING;
    }

    public boolean isVerified() {
        return !needsVerification() || verification == MapVerification.VERIFIED;
    }

    public boolean isPublished() {
        return publishedId != null;
    }

    public boolean isCompletable() {
        return settings.variant() == MapVariant.PARKOUR;
    }

    public MapData withSettings(Settings settings) {
        return new MapData(
            id,
            owner,
            protocolVersion,
            settings,
            verification,
            publishedId,
            publishedAt,
            listed,
            quality,
            difficulty,
            uniquePlays,
            clearRate,
            likes,
            contest,
            createdAt,
            lastModified
        );
    }

    @RuntimeGson
    public record Settings(
        String name,
        String icon,
        MapSize size,
        MapVariant variant,
        @Nullable String subvariant,
        Position spawnPoint,
        List<String> tags,
        MapLeaderboard leaderboard,
        JsonObject extra
    ) {
        public Settings {
            tags = List.copyOf(tags);
            extra = extra.deepCopy();
        }

        public static Settings defaults() {
            return new Settings(
                "",
                "",
                MapSize.NORMAL,
                MapVariant.PARKOUR,
                null,
                new Position(0.5, 40, 0.5, 0, 0),
                List.of(),
                MapLeaderboard.DEFAULT,
                new JsonObject()
            );
        }
    }
}
