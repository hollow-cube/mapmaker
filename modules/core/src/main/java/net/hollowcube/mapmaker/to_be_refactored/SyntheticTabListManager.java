package net.hollowcube.mapmaker.to_be_refactored;

import net.hollowcube.ipc.player.DisplayName;
import net.hollowcube.mapmaker.api.players.PlayerClient;
import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.session.PlayerSession;
import net.minestom.server.adventure.audience.Audiences;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.PlayerInfoRemovePacket;
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket;
import net.minestom.server.utils.PacketSendingUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SyntheticTabListManager {

    private static final EnumSet<PlayerInfoUpdatePacket.Action> ACTIONS = EnumSet.of(
            PlayerInfoUpdatePacket.Action.ADD_PLAYER,
            PlayerInfoUpdatePacket.Action.UPDATE_LISTED,
            PlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME,
            PlayerInfoUpdatePacket.Action.UPDATE_LIST_ORDER
    );

    private final PlayerClient players;

    private final Map<String, PlayerInfoUpdatePacket.Entry> listedPlayers = new ConcurrentHashMap<>();

    public SyntheticTabListManager(@NotNull PlayerClient players) {
        this.players = players;
    }

    public void addSession(@NotNull PlayerSession session) {
        List<PlayerInfoUpdatePacket.Property> properties = session.skin().texture() == null ? List.of()
                : List.of(new PlayerInfoUpdatePacket.Property("textures", session.skin().texture(), session.skin().signature()));
        var displayName = players.getDisplayName(session.playerId());
        var username = Objects.requireNonNullElse(displayName.username(), "Unknown");
        var playerListEntry = new PlayerInfoUpdatePacket.Entry(
                getListUuid(session.playerId()), username, properties,
                true, 0, null, displayName.render(),
                null, tabListOrder(displayName), true
        );

        listedPlayers.put(session.playerId(), playerListEntry);
        MiscFunctionality.broadcastTabList(Audiences.all(), listedPlayers.size());
        var packet = new PlayerInfoUpdatePacket(ACTIONS, List.of(playerListEntry));
        PacketSendingUtils.broadcastPlayPacket(packet);
    }

    public void removeSession(@NotNull String sessionId) {
        listedPlayers.remove(sessionId);
        MiscFunctionality.broadcastTabList(Audiences.all(), listedPlayers.size());
        var packet = new PlayerInfoRemovePacket(List.of(getListUuid(sessionId)));
        PacketSendingUtils.broadcastPlayPacket(packet);
    }

    public void addLocalPlayer(@NotNull Player player) {
        player.sendPacket(new PlayerInfoUpdatePacket(ACTIONS, List.copyOf(listedPlayers.values())));
        MiscFunctionality.broadcastTabList(player, listedPlayers.size());
    }

    // This method exists as minestom automatically adds its own entries so we need our
    // own ids for our entry
    private @NotNull UUID getListUuid(@NotNull String playerId) {
        var playerUuid = UUID.fromString(playerId);
        return new UUID(playerUuid.getMostSignificantBits(), playerUuid.getLeastSignificantBits() + 1);
    }

    private static int tabListOrder(DisplayName name) {
        return switch (name.badge()) {
            case "dev_3", "mod_3", "ct_3" -> 5;
            case "dev_2", "mod_2", "ct_2" -> 4;
            case "dev_1", "mod_1", "ct_1" -> 3;
            case "media" -> 2;
            case "hypercube/gold" -> 1;
            case null, default -> 0;
        };
    }
}
