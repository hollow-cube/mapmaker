package net.hollowcube.mapmaker.map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.time.Instant;

public class MapData {
    public static final String DEFAULT_NAME = "Untitled Map";

    private String id;
    private String owner;

    private MapSettings settings;

    private long publishedId;
    private Instant publishedAt;

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
    }

    public @NotNull String id() {
        return id;
    }

    public @NotNull String owner() {
        return owner;
    }

    public @NotNull MapSettings settings() {
        return settings;
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
