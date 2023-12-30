package net.hollowcube.mapmaker.to_be_refactored;

import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.session.PlayerSession;
import net.hollowcube.mapmaker.session.SessionManager;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.audience.Audiences;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.PlayerInfoRemovePacket;
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket;
import net.minestom.server.timer.ExecutionType;
import net.minestom.server.utils.PacketUtils;
import net.minestom.server.utils.time.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@SuppressWarnings("UnstableApiUsage")
public class SyntheticTabListManager {
    private static final Logger logger = LoggerFactory.getLogger(SyntheticTabListManager.class);

    private static final EnumSet<PlayerInfoUpdatePacket.Action> ACTIONS = EnumSet.of(
            PlayerInfoUpdatePacket.Action.ADD_PLAYER, PlayerInfoUpdatePacket.Action.UPDATE_LISTED,
            PlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME
    );

    private final SessionManager sessionManager;
    private final PlayerService playerService;

    // Contains a player list entry for every player globally.
    private final Map<String, PlayerInfoUpdatePacket.Entry> allPlayers = new ConcurrentHashMap<>();
    private final Set<String> localPlayers = new CopyOnWriteArraySet<>();

    public SyntheticTabListManager(@NotNull SessionManager sessionManager, @NotNull PlayerService playerService) {
        this.sessionManager = sessionManager;
        this.playerService = playerService;
    }

    public void addSession(@NotNull PlayerSession session) {
        List<PlayerInfoUpdatePacket.Property> properties = session.skin().texture() == null ? List.of()
                : List.of(new PlayerInfoUpdatePacket.Property("textures", session.skin().texture(), session.skin().signature()));
        var displayName = playerService.getPlayerDisplayName2(session.playerId()).build();
        var playerListEntry = new PlayerInfoUpdatePacket.Entry(
                UUID.fromString(session.playerId()), session.playerId().substring(0, 5), properties,
                true, 0, null, displayName, null
        );

        allPlayers.put(session.playerId(), playerListEntry);

        // Send the update to all current local players if the player is not currently on the server.
        if (!localPlayers.contains(session.playerId())) {
            var packet = new PlayerInfoUpdatePacket(ACTIONS, List.of(playerListEntry));
            PacketUtils.broadcastPlayPacket(packet);
        }
    }

    public void removeSession(@NotNull String sessionId) {
        var playerListEntry = allPlayers.remove(sessionId);
        if (playerListEntry == null) return;

        // Send the update to all current local players if the player is not currently on the server.
        if (!localPlayers.contains(sessionId)) {
            var packet = new PlayerInfoRemovePacket(List.of(UUID.fromString(sessionId)));
            PacketUtils.broadcastPlayPacket(packet);
        }

        // Always update the player count anyway.
        MiscFunctionality.broadcastTabList(Audiences.all(), sessionManager.networkPlayerCount());
    }

    public void preAddLocalPlayer(@NotNull Player player) {
        localPlayers.add(player.getUuid().toString());

        // Remove the player for all others on the server
        var packet = new PlayerInfoRemovePacket(List.of(player.getUuid()));
        PacketUtils.broadcastPlayPacket(packet);
    }

    public void addLocalPlayer(@NotNull Player player) {

        // Add all remote entries to the player.
        // The local ones will be managed by Minestom anyway.
        var remoteEntries = allPlayers.entrySet().stream()
                .filter(entry -> !localPlayers.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();
        player.sendPacket(new PlayerInfoUpdatePacket(ACTIONS, remoteEntries));

        // Always update the player count.
        MiscFunctionality.broadcastTabList(Audiences.all(), sessionManager.networkPlayerCount());
    }

    public void removeLocalPlayer(@NotNull Player player) {
        localPlayers.remove(player.getUuid().toString());

        MinecraftServer.getSchedulerManager().buildTask(() -> {
            // When a player is removed by Minestom we need to re-add them as a synthetic entry as long as their session exists.
            var entry = allPlayers.get(player.getUuid().toString());
            if (entry != null) {
                var packet = new PlayerInfoUpdatePacket(ACTIONS, List.of(entry));
                PacketUtils.broadcastPlayPacket(packet);
            }

            // Always update the player count anyway.
            MiscFunctionality.broadcastTabList(Audiences.all(), sessionManager.networkPlayerCount());
        }).delay(1, TimeUnit.SERVER_TICK).executionType(ExecutionType.SYNC).schedule();
    }
}
