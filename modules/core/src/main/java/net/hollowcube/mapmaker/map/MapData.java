package net.hollowcube.mapmaker.map;

import net.hollowcube.mapmaker.object.ObjectData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class MapData {
    public static final String DEFAULT_NAME = "Untitled Map";

    public static final String SPAWN_MAP_ID = System.getenv("MAPMAKER_SPAWN_MAP_ID");
    public static final List<String> SPAWN_MAP_PLAYERS = List.of(
            "e6c3e11c-1166-4dcd-ad59-c12e386b00bd", // Nixotica
            "aceb326f-da15-45bc-bf2f-11940c21780c", // notmattw
            "dbe21059-5b0b-4312-aa12-830dc540ad04", // Seth28
            "a3634428-40a0-45b3-8583-a3b5813d64c5", // SethPRG
            "b6496267-8dfe-485c-982f-85871ae4cbe4", // Tamto
            "3e66e238-ec72-49bb-b9dc-6a8a83d0aae6", // ArcaneWarrior
            "47cc8695-2681-4dcd-b772-7eeb8d69c09b", // Ossipago1
            "194845a1-cd34-43a7-9c35-a70c26bc0d90", // TheSmartFox
            "ed017f08-fd89-46e2-bba0-495686319801"  // Ontal
    );

    private String id;
    private String owner;
    private MapSettings settings;
    private MapVerification verification = MapVerification.UNVERIFIED;

    private long publishedId;
    private Instant publishedAt;

    private int objectLimit = 100;
    private List<ObjectData> objects = new ArrayList<>();
    private transient int objectUsage = -1;

    public MapData() {
    }

    public MapData(
            @NotNull String id,
            @NotNull String owner,
            @NotNull MapSettings settings,
            long publishedId,
            @Nullable Instant publishedAt
    ) {
        this.id = id;
        this.owner = owner;
        this.settings = settings;
        this.publishedId = publishedId;
        this.publishedAt = publishedAt;
//        this.pois = new ArrayList<>();
//        this.maxPois = 100;
        this.objectLimit = 100;
        this.objects = new ArrayList<>();
    }

    public @NotNull String id() {
        return id;
    }

    public @NotNull String name() {
        var name = settings.getName();
        if (name.isEmpty())
            return DEFAULT_NAME;
        return name;
    }

    public @NotNull String owner() {
        return owner;
    }

    public @NotNull MapSettings settings() {
        return settings;
    }

    public boolean needsVerification() {
        return settings().getVariant() != MapVariant.BUILDING;
    }

    public boolean isVerified() {
        return !needsVerification() || verification == MapVerification.VERIFIED;
    }

    public @NotNull MapVerification verification() {
        return verification;
    }

    public boolean isPublished() {
        return publishedId != 0;
    }

    public long publishedId() {
        return publishedId;
    }

    public @UnknownNullability String publishedIdString() {
        return publishedId == 0 ? null : formatPublishedId(publishedId);
    }

    public @UnknownNullability Instant publishedAt() {
        return publishedAt;
    }

    public int objectUsage() {
        if (objectUsage == -1) {
            objectUsage = objects.stream()
                    .mapToInt(o -> o.type().cost())
                    .sum();
        }

        return objectUsage;
    }

    public boolean addObject(@NotNull ObjectData object) {
        settings.updateLock.lock();
        try {
            if (objectUsage() + object.type().cost() > objectLimit)
                return false;

            objects.add(object);
            objectUsage += object.type().cost();

            // Add to update
            settings.updates.newObjects.add(object);
            settings.updates.removedObjects.remove(object.id());

            return true;
        } finally {
            settings.updateLock.unlock();
        }
    }

    public boolean removeObject(@NotNull String id) {
        settings.updateLock.lock();
        try {
            var removed = false;
            var iter = objects.iterator();
            while (iter.hasNext()) {
                var object = iter.next();

                if (object.id().equals(id)) {
                    iter.remove();
                    objectUsage = objectUsage() - object.type().cost();
                    removed = true;

                    settings.updates.removedObjects.add(id);
                }
            }

            if (removed) {
                settings.updates.newObjects.removeIf(p -> p.id().equals(id));
            }

            return removed;
        } finally {
            settings.updateLock.unlock();
        }
    }

    public @NotNull List<ObjectData> objects() {
        return List.copyOf(objects);
    }

    public @Nullable ObjectData getObject(String id) {
        var object = objects.stream().filter(obj -> obj.id().equals(id)).findFirst();
        return object.orElse(null);
    }

    public static @NotNull String formatPublishedId(long number) {
        // Pad zeros if necessary
        var numberString = new StringBuilder(String.valueOf(number));
        while (numberString.length() < 9) {
            numberString.insert(0, "0");
        }

        // Format as xxx-xxx-xxx
        return numberString.substring(0, 3) +
                "-" +
                numberString.substring(3, 6) +
                "-" +
                numberString.substring(6);
    }

    public static long parsePublishedID(@NotNull String publishedId) {
        class Holder {
            static final Pattern ID_PATTERN = Pattern.compile("([0-9]{3})-([0-9]{3})-([0-9]{3})");
        }

        if (!Holder.ID_PATTERN.matcher(publishedId).matches())
            throw new IllegalArgumentException("Invalid published ID format");
        return Long.parseLong(publishedId.replace("-", ""));
    }

    public static class WithSlot extends MapData {
        private int slot;

        public int slot() {
            return slot;
        }
    }
}
