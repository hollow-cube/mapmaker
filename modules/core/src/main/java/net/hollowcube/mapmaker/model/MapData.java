package net.hollowcube.mapmaker.model;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.item.ItemStack;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MapData {
    // Permission keys

    public enum Permission {
        READ("read"),
        WRITE("write"),
        ADMIN("admin");

        private final String key;

        Permission(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }
    public static final String READ = "read";
    public static final String WRITE = "write";
    public static final String ADMIN = "admin";

    public static final @Language("regexp") String NAME_REGEX = "[a-zA-Z0-9_ ]{1,32}";

    // Conditions:
    //  Allowed characters: A-Z, 0-9, _
    //  Must contain at least one: A-Z
    private static final @Language("regexp") String ALIAS_REGEX = "^[A-Z0-9_]*[A-Z]+[A-Z0-9_]*$";

    private String id;
    private String owner;
    private String name = "Untitled Map";

    // Published state
    private Instant publishedAt = null;
    // Published id is a short ID for users to reference a map.
    // This ID is assigned when a map is published and not guaranteed to be consistent. It should never be stored
    // internally as a reference to a map. Use the ID instead.
    private String publishedId;

    // Alias id is a 3 to 16 character unique ID allowing [0-9A-Z] all caps no spaces no symbols
    // that can be referenced in place of the publishedId. This is optional for maps and will only
    // be acquired by purchase or special circumstance.
    private String aliasId = null;

    // The following is unqueryable data, may be stored in serialized form.

    // ID of the file in storage, or null if the map does not yet exist (it is lazily created)
    private String mapFileId;
    // ItemStack Icon to display the map as
    private ItemStack icon;
    private Pos spawnPoint = new Pos(0, 40, 0);
    private final List<POI> pois = new ArrayList<>();

    private final int MAX_COMPLETION_TIMES = 10;
    private final List<CompletionTime> completionTimes = new ArrayList<>(MAX_COMPLETION_TIMES + 1);

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ItemStack getIcon() {
        return icon;
    }

    public void setIcon(ItemStack icon) {
        this.icon = icon;
    }

    public boolean isPublished() {
        return publishedAt != null;
    }

    public boolean hasAlias() {
        return aliasId != null;
    }

    public @UnknownNullability Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public String getDisplayedId() {
        return (hasAlias()) ? getAliasId() : getPublishedId();
    }

    public String getPublishedId() {
        return publishedId;
    }

    public void setPublishedId(String publishedId) {
        this.publishedId = publishedId;
    }

    public String getAliasId() {
        return aliasId;
    }

    public static boolean isValidAlias(String aliasId) {
        if (aliasId.length() < 3 || aliasId.length() > 16)
            return false;
        return aliasId.matches(ALIAS_REGEX);
    }

    public boolean setAliasId(String aliasId) {
        if (isValidAlias(aliasId)) {
            this.aliasId = aliasId;
            return true;
        } else {
            return false;
        }
    }

    public String getMapFileId() {
        return mapFileId;
    }

    public void setMapFileId(String mapFileId) {
        this.mapFileId = mapFileId;
    }

    public @NotNull Pos getSpawnPoint() {
        return spawnPoint;
    }

    public void setSpawnPoint(@NotNull Pos spawnPoint) {
        this.spawnPoint = spawnPoint;
    }

    public List<POI> getPois() {
        return pois;
    }

    public POI getPoi(@NotNull Point pos) {
        for (POI poi : pois) {
            if (poi.pos.sameBlock(pos)) {
                return poi;
            }
        }
        return null;
    }

    public POI getPoi(@NotNull String id) {
        for (POI poi : pois) {
            if (poi.id.equals(id)) {
                return poi;
            }
        }
        return null;
    }

    public void addPOI(@NotNull POI poi) {
        pois.add(poi);
    }

    public void removePOI(@NotNull Point pos) {
        pois.removeIf(poi -> poi.pos.sameBlock(pos));
    }

    public void tryAddTime(UUID id, long time) {
        int index = -1;
        for (int i = 0; i < completionTimes.size(); i++) {
            // Find index
            if (time < completionTimes.get(i).timeInMills) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            // We were not able to find an index, could be in a couple of states, array is empty, it's the last possible time, etc
            // Add onto end, we will correct it later
            completionTimes.add(new CompletionTime(id, time));
        } else {
            // found insertion index
            completionTimes.add(index, new CompletionTime(id, time));
        }
        // Remove times until we are at the max to correct data
        while (completionTimes.size() > MAX_COMPLETION_TIMES) {
            completionTimes.remove(completionTimes.size() - 1);
        }
    }

    public List<CompletionTime> getCompletionTimes() {
        return completionTimes;
    }

    @Override
    public String toString() {
        return "MapData{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    public static class POI {
        private final String type;
        private final String id;
        private final Point pos;
        private final Map<String, Object> data;

        public POI(String type, String id, Point pos) {
            this(type, id, pos, new HashMap<>());
        }

        public POI(String type, String id, Point pos, Map<String, Object> data) {
            this.type = type;
            this.id = id;
            this.pos = pos;
            this.data = new HashMap<>(data);
        }

        public String getType() {
            return type;
        }

        public String getId() {
            return id;
        }

        public Point getPos() {
            return pos;
        }

        public Map<String, Object> getData() {
            return data;
        }

        public <T> @NotNull  T getOrDefault(@NotNull String key, T def) {
            //noinspection unchecked
            return (T) data.getOrDefault(key, def);
        }

        public void set(@NotNull String key, Object value) {
            data.put(key, value);
        }
    }

    public record CompletionTime(UUID playerUUID, long timeInMills) {}

}
