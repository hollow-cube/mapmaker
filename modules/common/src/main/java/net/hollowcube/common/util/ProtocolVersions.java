package net.hollowcube.common.util;

import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.stream.Collectors;

public final class ProtocolVersions {
    public static final int CURRENT = MinecraftServer.PROTOCOL_VERSION;

    public static int getProtocolVersion(@NotNull String name) {
        return ID_TO_NAME.getOrDefault(name, -1);
    }
    
    public static @NotNull String getProtocolName(int version) {
        return NAME_TO_ID.getOrDefault(version, "unknown");
    }

    private static final Map<String, Integer> ID_TO_NAME = Map.ofEntries(
            Map.entry("1.20.4", 765),
            Map.entry("1.20.2", 764),
            Map.entry("1.20.1", 763),
            Map.entry("1.19.4", 762),
            Map.entry("1.19.3", 761),
            Map.entry("1.19.2", 760),
            Map.entry("1.19", 759),
            Map.entry("1.18.2", 758),
            Map.entry("1.18.1", 757),
            Map.entry("1.17.1", 756),
            Map.entry("1.17", 755),
            Map.entry("1.16.5", 754)
    );
    private static final Map<Integer, String> NAME_TO_ID = ID_TO_NAME.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

}
