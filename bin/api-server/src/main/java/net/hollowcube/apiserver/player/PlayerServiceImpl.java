package net.hollowcube.apiserver.player;

import com.google.gson.JsonObject;
import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.ipc.PaginatedList;
import net.hollowcube.ipc.player.DisplayName;
import net.hollowcube.ipc.player.HypercubeStatus;
import net.hollowcube.ipc.player.PlayerData;
import net.hollowcube.ipc.player.PlayerService;
import net.hollowcube.ipc.player.PlayerStub;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static net.hollowcube.ipc.util.IpcArgs.uuidOrNull;
import static net.hollowcube.ipc.util.IpcException.notFound;

public final class PlayerServiceImpl implements PlayerService {

    private static final int DEFAULT_SEARCH_LIMIT = 25;
    private static final int MAX_SEARCH_LIMIT = 100;

    private final ApiDatabase db;

    public PlayerServiceImpl(ApiDatabase db) {
        this.db = db;
    }

    @Override
    public @Nullable PlayerData get(String idOrUsername) {
        var id = uuidOrNull(idOrUsername);
        var row = id == null
            ? db.players.getPlayerByUsername(idOrUsername)
            : db.players.getPlayerById(id);
        return Players.data(row);
    }

    @Override
    public @Nullable DisplayName displayName(UUID playerId) {
        return displayNames(List.of(playerId)).get(playerId);
    }

    @Override
    public Map<UUID, DisplayName> displayNames(List<UUID> playerIds) {
        if (playerIds.isEmpty()) return Map.of();
        var names = new HashMap<UUID, DisplayName>();
        for (var id : playerIds) {
            var org = DisplayNames.org(id);
            if (org != null) names.put(id, org);
        }
        for (var row : db.players.getPlayerNames(playerIds)) {
            var name = DisplayNames.of(row.id(), row.username(), row.role(), row.hypercubeEnd());
            names.put(row.id(), name);
        }
        return names;
    }

    @Override
    public @Nullable HypercubeStatus hypercube(UUID playerId) {
        var row = db.players.getPlayerById(playerId);
        if (row == null || row.hypercubeStart() == null || row.hypercubeEnd() == null) return null;
        if (!row.hypercubeEnd().isAfter(Instant.now())) return null;
        return new HypercubeStatus(row.hypercubeStart(), row.hypercubeEnd());
    }

    @Override
    public void updateSettings(UUID playerId, JsonObject patch) {
        if (patch.isEmpty()) return;
        if (db.players.updatePlayerSettings(patch.toString(), playerId) == 0) {
            throw notFound("no player " + playerId);
        }
    }

    @Override
    public List<PlayerStub> search(String query, List<UUID> excludeIds, int limit) {
        if (query.isEmpty()) return List.of();
        var clamped = PaginatedList.limit(limit, DEFAULT_SEARCH_LIMIT, MAX_SEARCH_LIMIT);
        var stubs = new ArrayList<PlayerStub>();
        for (var row : db.players.searchPlayers(excludeIds, query, clamped)) {
            stubs.add(Players.stub(row));
        }
        return stubs;
    }

    @Override
    public List<PlayerStub> alts(UUID playerId) {
        var alts = new ArrayList<PlayerStub>();
        for (var row : db.players.getPlayerAlts(playerId)) {
            alts.add(Players.stub(row));
        }
        return alts;
    }
}
