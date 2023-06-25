package net.hollowcube.mapmaker.map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.time.Instant;

public class MapData {
    public static final String DEFAULT_NAME = "Untitled Map";

    private String id;
    private String owner;

    private MapSettings settings;

    private long publishedId;
    private Instant publishedAt;

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
