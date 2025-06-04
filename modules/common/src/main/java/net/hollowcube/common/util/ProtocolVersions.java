package net.hollowcube.common.util;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.stream.Collectors;

public final class ProtocolVersions {
    public static final int V1_21_5 = 770;

    public static final int MIN_SUPPORTED = V1_21_5;
    public static final int CURRENT = V1_21_5;

    public static int getProtocolVersion(@NotNull String name) {
        return ID_TO_NAME.getOrDefault(name, -1);
    }

    public static @NotNull String getProtocolName(int version) {
        return NAME_TO_ID.getOrDefault(version, "unknown");
    }

    private static final Map<String, Integer> ID_TO_NAME = Map.ofEntries(
            Map.entry("1.21.5", V1_21_5)
    );
    private static final Map<Integer, String> NAME_TO_ID = ID_TO_NAME.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

}
