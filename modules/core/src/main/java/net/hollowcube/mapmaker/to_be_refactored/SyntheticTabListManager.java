package net.hollowcube.mapmaker.to_be_refactored;

import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.session.PlayerSession;
import net.hollowcube.mapmaker.session.SessionManager;
import net.minestom.server.adventure.audience.Audiences;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.PlayerInfoRemovePacket;
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket;
import net.minestom.server.utils.PacketUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("UnstableApiUsage")
public class SyntheticTabListManager {
    private static final Logger logger = LoggerFactory.getLogger(SyntheticTabListManager.class);

    private static final EnumSet<PlayerInfoUpdatePacket.Action> ACTIONS = EnumSet.of(
            PlayerInfoUpdatePacket.Action.ADD_PLAYER, PlayerInfoUpdatePacket.Action.UPDATE_LISTED,
            PlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME
    );

    private final SessionManager sessionManager;
    private final PlayerService playerService;

    private final Map<String, PlayerInfoUpdatePacket.Entry> listedPlayers = new ConcurrentHashMap<>();

    public SyntheticTabListManager(@NotNull SessionManager sessionManager, @NotNull PlayerService playerService) {
        this.sessionManager = sessionManager;
        this.playerService = playerService;
    }

    public void addSession(@NotNull PlayerSession session) {
        List<PlayerInfoUpdatePacket.Property> properties = session.skin().texture() == null ? List.of()
                : List.of(new PlayerInfoUpdatePacket.Property("textures", session.skin().texture(), session.skin().signature()));
        var displayName = playerService.getPlayerDisplayName2(session.playerId());
        var playerListEntry = new PlayerInfoUpdatePacket.Entry(
                getListUuid(session.playerId()), session.username(), properties,
                true, 0, null, displayName.build(), null
        );

        listedPlayers.put(session.playerId(), playerListEntry);
        MiscFunctionality.broadcastTabList(Audiences.all(), listedPlayers.size());
        var packet = new PlayerInfoUpdatePacket(ACTIONS, List.of(playerListEntry));
        PacketUtils.broadcastPlayPacket(packet);
    }

    public void removeSession(@NotNull String sessionId) {
        listedPlayers.remove(sessionId);
        MiscFunctionality.broadcastTabList(Audiences.all(), listedPlayers.size());
        var packet = new PlayerInfoRemovePacket(List.of(getListUuid(sessionId)));
        PacketUtils.broadcastPlayPacket(packet);
    }

    public void addLocalPlayer(@NotNull Player player) {
        player.sendPacket(new PlayerInfoUpdatePacket(ACTIONS, List.copyOf(listedPlayers.values())));
        MiscFunctionality.broadcastTabList(player, listedPlayers.size());
    }

    private @NotNull UUID getListUuid(@NotNull String playerId) {
        var playerUuid = UUID.fromString(playerId);
        return new UUID(playerUuid.getMostSignificantBits(), playerUuid.getLeastSignificantBits() + 1);
    }
}
