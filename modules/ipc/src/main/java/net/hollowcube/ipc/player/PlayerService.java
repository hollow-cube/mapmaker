package net.hollowcube.ipc.player;

import com.google.gson.JsonObject;
import net.hollowcube.ipc.util.Ipc;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Ipc
public interface PlayerService {

    @Nullable
    PlayerData get(String idOrUsername);

    @Nullable
    DisplayName displayName(UUID playerId);

    Map<UUID, DisplayName> displayNames(List<UUID> playerIds);

    @Nullable
    HypercubeStatus hypercube(UUID playerId);

    /// Merges top-level keys atomically; a JSON null removes its key.
    void updateSettings(UUID playerId, JsonObject patch);

    /// Substring match on the username; `limit` of 0 (or over the cap) means the default page.
    List<PlayerStub> search(String query, List<UUID> excludeIds, int limit);

    List<PlayerStub> alts(UUID playerId);
}
