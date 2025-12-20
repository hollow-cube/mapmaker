package net.hollowcube.common.util;

import net.hollowcube.posthog.PostHog;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class ProtocolVersions {
    public static final int V1_21_4 = 769;
    public static final int V1_21_5 = 770;
    public static final int V1_21_6 = 771;
    public static final int V1_21_7 = 772;
    public static final int V1_21_9 = 773;
    public static final int V1_21_11 = 774;

    public static final int UNKNOWN = -1;
    public static final int MIN_SUPPORTED = V1_21_4;
    public static final int CURRENT = MinecraftServer.PROTOCOL_VERSION;
    private static final Logger logger = LoggerFactory.getLogger(ProtocolVersions.class);

    public static int getProtocolVersion(@NotNull String name) {
        return ID_TO_NAME.getOrDefault(name, -1);
    }

    public static @NotNull String getProtocolName(int version) {
        return NAME_TO_ID.getOrDefault(version, "unknown");
    }

    public static int getProtocolVersion(@NotNull Player player) {
        return player.getTag(PROTOCOL_VERSION_TAG);
    }

    public static boolean hasProtocolVersion(@NotNull Player player, int version) {
        return getProtocolVersion(player) >= version;
    }

    @Blocking
    public static void requestProtocolVersionFromProxy(@NotNull Player player) {
        var future = new CompletableFuture<Integer>();
        MinecraftServer.getGlobalEventHandler().addListener(EventListener.builder(PlayerPluginMessageEvent.class)
            .filter(event -> event.getPlayer() == player && "mapmaker:pvn".equals(event.getIdentifier()))
            .handler(event -> {
                try {
                    var str = new String(event.getMessage(), StandardCharsets.UTF_8);
                    future.complete(Integer.parseInt(str));
                } catch (NumberFormatException e) {
                    PostHog.captureException(e, player.getUuid().toString());
                    logger.error("Failed to parse protocol version from proxy for player {}: {}", player.getUsername(), e.getMessage());
                    future.complete(UNKNOWN);
                }
            })
            .expireCount(1)
            .build());

        player.sendPluginMessage("mapmaker:pvn", new byte[0]);
        int pvn = Objects.requireNonNullElse(FutureUtil.getUnchecked(future, 5000), UNKNOWN);
        if (pvn == UNKNOWN) {
            // Not a really useful message, just something we can identify in logs :)
            player.kick("Unsupported version. Please update your client.");
            return;
        }

        player.setTag(PROTOCOL_VERSION_TAG, pvn);
    }

    public static void unsafeSetProtocolVersion(@NotNull Player player, int protocolVersion) {
        player.setTag(PROTOCOL_VERSION_TAG, protocolVersion);
    }

    private static final Map<String, Integer> ID_TO_NAME = Map.ofEntries(
        Map.entry("1.21.4", V1_21_4),
        Map.entry("1.21.5", V1_21_5),
        Map.entry("1.21.6", V1_21_6),
        Map.entry("1.21.7", V1_21_7),
        Map.entry("1.21.9", V1_21_9),
        Map.entry("1.21.11", V1_21_11)
    );
    private static final Map<Integer, String> NAME_TO_ID = ID_TO_NAME.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    public static final List<String> SUPPORTED_PROTOCOL_NAMES = ID_TO_NAME.keySet().stream().sorted().toList();

    // We assume the current protocol version unless we are told otherwise.
    private static final Tag<Integer> PROTOCOL_VERSION_TAG = Tag.Integer("mapmaker:pvn").defaultValue(CURRENT);

}
