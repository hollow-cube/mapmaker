package net.hollowcube.mapmaker.to_be_refactored;

import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.session.PlayerSession;
import net.hollowcube.mapmaker.session.SessionManager;
import net.minestom.server.adventure.audience.Audiences;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.PlayerInfoRemovePacket;
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket;
import net.minestom.server.utils.PacketSendingUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SyntheticTabListManager {

    private static final EnumSet<PlayerInfoUpdatePacket.Action> ACTIONS = EnumSet.of(
            PlayerInfoUpdatePacket.Action.ADD_PLAYER,
            PlayerInfoUpdatePacket.Action.UPDATE_LISTED,
            PlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME,
            PlayerInfoUpdatePacket.Action.UPDATE_LIST_ORDER
    );

    private final SessionManager sessionManager;
    private final PlayerService playerService;

    private final Map<String, PlayerInfoUpdatePacket.Entry> listedPlayers = new ConcurrentHashMap<>();

    public SyntheticTabListManager(SessionManager sessionManager, PlayerService playerService) {
        this.sessionManager = sessionManager;
        this.playerService = playerService;
    }

    public void addSession(PlayerSession session) {
        List<PlayerInfoUpdatePacket.Property> properties = session.skin().texture() == null ? List.of()
                : List.of(new PlayerInfoUpdatePacket.Property("textures", session.skin().texture(), session.skin().signature()));
        var displayName = playerService.getPlayerDisplayName2(session.playerId());
        var username = Objects.requireNonNullElse(displayName.getUsername(), "Unknown");
        var playerListEntry = new PlayerInfoUpdatePacket.Entry(
                getListUuid(session.playerId()), username, properties,
                true, 0, null, displayName.build(),
                null, displayName.getTabListOrder(), true
        );

        listedPlayers.put(session.playerId(), playerListEntry);
        MiscFunctionality.broadcastTabList(Audiences.all(), listedPlayers.size());
        var packet = new PlayerInfoUpdatePacket(ACTIONS, List.of(playerListEntry));
        PacketSendingUtils.broadcastPlayPacket(packet);
    }

    public void removeSession(String sessionId) {
        listedPlayers.remove(sessionId);
        MiscFunctionality.broadcastTabList(Audiences.all(), listedPlayers.size());
        var packet = new PlayerInfoRemovePacket(List.of(getListUuid(sessionId)));
        PacketSendingUtils.broadcastPlayPacket(packet);
    }

    public void addLocalPlayer(Player player) {
        player.sendPacket(new PlayerInfoUpdatePacket(ACTIONS, List.copyOf(listedPlayers.values())));
        MiscFunctionality.broadcastTabList(player, listedPlayers.size());
    }

    // This method exists as minestom automatically adds its own entries so we need our
    // own ids for our entry
    private UUID getListUuid(String playerId) {
        var playerUuid = UUID.fromString(playerId);
        return new UUID(playerUuid.getMostSignificantBits(), playerUuid.getLeastSignificantBits() + 1);
    }
}
