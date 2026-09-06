package net.hollowcube.ipc.map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.ipc.util.Position;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

@RuntimeGson
public record MapPatch(
    @Nullable String name,
    @Nullable String icon,
    @Nullable MapSize size,
    @Nullable MapVariant variant,
    @Nullable String subvariant,
    @Nullable Position spawnPoint,
    @Nullable List<String> tags,
    @Nullable MapLeaderboard leaderboard,
    @Nullable JsonObject extra,
    @Nullable Boolean listed,
    @Nullable MapQuality qualityOverride,
    @Nullable Integer protocolVersion
) {
    public MapPatch {
        tags = tags == null ? null : List.copyOf(tags);
        extra = extra == null ? null : extra.deepCopy();
    }

    public boolean isEmpty() {
        return name == null
            && icon == null
            && size == null
            && variant == null
            && subvariant == null
            && spawnPoint == null
            && tags == null
            && leaderboard == null
            && extra == null
            && listed == null
            && qualityOverride == null
            && protocolVersion == null;
    }

    public MapData apply(MapData map) {
        var s = map.settings();
        var mergedExtra = s.extra().deepCopy();
        if (extra != null)
            extra.entrySet().forEach(e -> mergedExtra.add(e.getKey(), e.getValue().deepCopy()));
        var settings = new MapData.Settings(
            or(name, s.name()),
            or(icon, s.icon()),
            or(size, s.size()),
            or(variant, s.variant()),
            "none".equals(subvariant) ? null : or(subvariant, s.subvariant()),
            or(spawnPoint, s.spawnPoint()),
            or(tags, s.tags()),
            or(leaderboard, s.leaderboard()),
            mergedExtra
        );
        return new MapData(
            map.id(),
            map.owner(),
            or(protocolVersion, map.protocolVersion()),
            settings,
            map.verification(),
            map.publishedId(),
            map.publishedAt(),
            or(listed, map.listed()),
            or(qualityOverride, map.quality()),
            map.difficulty(),
            map.uniquePlays(),
            map.clearRate(),
            map.likes(),
            map.contest(),
            map.createdAt(),
            map.lastModified()
        );
    }

    private static <T> T or(@Nullable T value, T fallback) {
        return value == null ? fallback : value;
    }

    public static final class Builder {
        private final ReentrantLock lock = new ReentrantLock();
        private volatile MapData map;
        private @Nullable String name;
        private @Nullable String icon;
        private @Nullable MapSize size;
        private @Nullable MapVariant variant;
        private @Nullable String subvariant;
        private @Nullable Position spawnPoint;
        private @Nullable List<String> tags;
        private @Nullable MapLeaderboard leaderboard;
        private @Nullable JsonObject extra;
        private @Nullable Boolean listed;
        private @Nullable MapQuality qualityOverride;
        private @Nullable Integer protocolVersion;

        public Builder(MapData map) {
            this.map = Objects.requireNonNull(map);
        }

        public MapData map() {
            return map;
        }

        public MapPatch build() {
            lock.lock();
            try {
                return new MapPatch(
                    name,
                    icon,
                    size,
                    variant,
                    subvariant,
                    spawnPoint,
                    tags,
                    leaderboard,
                    extra,
                    listed,
                    qualityOverride,
                    protocolVersion
                );
            } finally {
                lock.unlock();
            }
        }

        public void reset(MapData map) {
            lock.lock();
            try {
                this.map = Objects.requireNonNull(map);
                name = null;
                icon = null;
                size = null;
                variant = null;
                subvariant = null;
                spawnPoint = null;
                tags = null;
                leaderboard = null;
                extra = null;
                listed = null;
                qualityOverride = null;
                protocolVersion = null;
            } finally {
                lock.unlock();
            }
        }

        /// A null result or an exception retains pending edits. Edits from other threads wait
        /// until the save finishes so resetting to the saved map cannot discard them.
        public void save(Function<MapPatch, @Nullable MapData> write) {
            lock.lock();
            try {
                var patch = build();
                if (patch.isEmpty()) return;
                var saved = write.apply(patch);
                if (saved != null) reset(saved);
            } finally {
                lock.unlock();
            }
        }

        private void edit(Runnable change) {
            lock.lock();
            try {
                change.run();
                map = build().apply(map);
            } finally {
                lock.unlock();
            }
        }

        public void setName(String value) {
            edit(() -> name = Objects.requireNonNull(value));
        }

        public void setIcon(String value) {
            edit(() -> icon = Objects.requireNonNull(value));
        }

        public void setSize(MapSize value) {
            edit(() -> size = Objects.requireNonNull(value));
        }

        public void setVariant(MapVariant value) {
            edit(() -> variant = Objects.requireNonNull(value));
        }

        public void setSubVariant(@Nullable String value) {
            edit(() -> subvariant = value == null ? "none" : value);
        }

        public void setSpawnPoint(Position value) {
            edit(() -> spawnPoint = Objects.requireNonNull(value));
        }

        public void setTags(List<String> value) {
            edit(() -> tags = List.copyOf(value));
        }

        public void setLeaderboard(MapLeaderboard value) {
            edit(() -> leaderboard = Objects.requireNonNull(value));
        }

        public void setListed(boolean value) {
            edit(() -> listed = value);
        }

        public void setQualityOverride(MapQuality value) {
            edit(() -> qualityOverride = Objects.requireNonNull(value));
        }

        public void setProtocolVersion(int value) {
            edit(() -> protocolVersion = value);
        }

        public void setExtra(String key, JsonElement value) {
            var copy = value.deepCopy();
            edit(() -> {
                if (extra == null) extra = new JsonObject();
                extra.add(key, copy);
            });
        }

        public boolean addTag(String tag) {
            lock.lock();
            try {
                var tags = new ArrayList<>(map.settings().tags());
                if (tags.contains(tag)) return false;
                tags.add(tag);
                setTags(tags);
                return true;
            } finally {
                lock.unlock();
            }
        }

        public void replaceTag(String previous, String tag) {
            lock.lock();
            try {
                var tags = new ArrayList<>(map.settings().tags());
                tags.set(tags.indexOf(previous), tag);
                setTags(tags);
            } finally {
                lock.unlock();
            }
        }

        public boolean removeTag(String tag) {
            lock.lock();
            try {
                var tags = new ArrayList<>(map.settings().tags());
                if (!tags.remove(tag)) return false;
                setTags(tags);
                return true;
            } finally {
                lock.unlock();
            }
        }
    }
}
