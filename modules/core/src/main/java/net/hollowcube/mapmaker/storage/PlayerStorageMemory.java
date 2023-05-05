package net.hollowcube.mapmaker.storage;

import net.hollowcube.mapmaker.model.PlayerData;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

class PlayerStorageMemory implements PlayerStorage {
    public static final Logger logger = LoggerFactory.getLogger(PlayerStorageMemory.class);

    private final Map<String, PlayerData> playersById = new HashMap<>();

    @Override
    public @NotNull PlayerData createPlayer(@NotNull PlayerData player) {
        logger.info("Creating player {}", player.getId());
        var existing = playersById.putIfAbsent(player.getId(), player);
        if (existing != null)
            throw new DuplicateEntryError();
        return player;
    }

    @Override
    public @NotNull PlayerData getPlayerByUuid(@NotNull String uuid) {
        logger.info("Getting player by uuid {}", uuid);
        for (var entry : playersById.entrySet()) {
            if (entry.getValue().getId().equals(uuid)) {
                return entry.getValue();
            }
        }
        throw new NotFoundError(uuid);
    }

    @Override
    public void updatePlayer(@NotNull PlayerData player) {
        logger.info("Updating player {}", player.getId());
        if (!playersById.containsKey(player.getId()))
            throw new NotFoundError(player.getId());
        playersById.put(player.getId(), player);
    }

}
