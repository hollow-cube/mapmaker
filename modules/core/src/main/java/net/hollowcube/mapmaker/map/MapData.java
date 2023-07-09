package net.hollowcube.mapmaker.map;

import net.hollowcube.mapmaker.util.CoordinateUtil;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

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

    private record PointOfInterest(String type, Vec pos) {
    }

    private int maxPois = 100; //todo once map service sets this default it can be unset here
    private List<PointOfInterest> pois;
    private transient final ReentrantLock poiLock = new ReentrantLock();

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
        this.pois = new ArrayList<>();
        this.maxPois = 100;
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

    public boolean addPointOfInterest(@NotNull String type, @NotNull Point pos) {
        poiLock.lock();
        try {
            if (pois == null) pois = new ArrayList<>();
            if (pois.size() >= maxPois) return false;

            pos = CoordinateUtil.floor(pos);
            removePointOfInterest(pos);
            pois.add(new PointOfInterest(type, Vec.fromPoint(pos)));
            return true;
        } finally {
            poiLock.unlock();
        }
    }

    public @Nullable String removePointOfInterest(@NotNull Point pos) {
        poiLock.lock();
        try {
            if (pois == null) return null;

            String removed = null;
            var iter = pois.iterator();
            while (iter.hasNext()) {
                var poi = iter.next();
                if (poi.pos.equals(pos)) {
                    iter.remove();
                    removed = poi.type;
                }
            }
            return removed;
        } finally {
            poiLock.unlock();
        }
    }

    private static @NotNull String formatPublishedId(long number) {
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
}
