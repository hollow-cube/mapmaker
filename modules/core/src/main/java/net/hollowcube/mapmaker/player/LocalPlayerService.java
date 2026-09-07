package net.hollowcube.mapmaker.player;

import com.google.gson.JsonObject;
import net.hollowcube.ipc.player.DisplayName;
import net.hollowcube.ipc.player.HypercubeStatus;
import net.hollowcube.ipc.player.PlayerData;
import net.hollowcube.ipc.player.PlayerService;
import net.hollowcube.ipc.player.PlayerStub;
import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static net.hollowcube.mapmaker.player.LocalPlayer.localPlayer;

/// The api's player service as this server sees it: a player who is online here answers for their
/// own display name (it is the hottest lookup and their session already holds it), and a player the
/// api does not know at all renders as [DisplayName#UNKNOWN] rather than as a null every caller
/// would have to fence.
public final class LocalPlayerService implements PlayerService {
    private final PlayerService api;

    public LocalPlayerService(PlayerService api) {
        this.api = api;
    }

    @Override
    public DisplayName displayName(UUID playerId) {
        var local = localDisplayName(playerId);
        if (local != null) return local;
        return Objects.requireNonNullElse(api.displayName(playerId), DisplayName.UNKNOWN);
    }

    @Override
    public Map<UUID, DisplayName> displayNames(List<UUID> playerIds) {
        var names = new HashMap<UUID, DisplayName>();
        var remote = new ArrayList<UUID>();
        for (var id : playerIds) {
            var local = localDisplayName(id);
            if (local != null) names.put(id, local);
            else remote.add(id);
        }
        if (!remote.isEmpty()) names.putAll(api.displayNames(remote));
        for (var id : remote) names.putIfAbsent(id, DisplayName.UNKNOWN);
        return names;
    }

    @Override
    public @Nullable PlayerData get(String idOrUsername) {
        return api.get(idOrUsername);
    }

    @Override
    public @Nullable HypercubeStatus hypercube(UUID playerId) {
        return api.hypercube(playerId);
    }

    @Override
    public void updateSettings(UUID playerId, JsonObject patch) {
        api.updateSettings(playerId, patch);
    }

    @Override
    public List<PlayerStub> search(String query, List<UUID> excludeIds, int limit) {
        return api.search(query, excludeIds, limit);
    }

    @Override
    public List<PlayerStub> alts(UUID playerId) {
        return api.alts(playerId);
    }

    private static @Nullable DisplayName localDisplayName(UUID playerId) {
        var player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(playerId);
        return player == null ? null : localPlayer(player).info().displayName();
    }
}
