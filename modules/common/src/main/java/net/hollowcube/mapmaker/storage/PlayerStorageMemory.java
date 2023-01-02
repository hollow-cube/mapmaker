package net.hollowcube.mapmaker.storage;

import net.hollowcube.common.result.FutureResult;
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
    public @NotNull FutureResult<@NotNull PlayerData> createPlayer(@NotNull PlayerData player) {
        logger.info("Creating player {}", player.getId());
        var existing = playersById.putIfAbsent(player.getId(), player);
        if (existing != null)
            return FutureResult.error(ERR_DUPLICATE_ENTRY);
        return FutureResult.of(player);
    }

    @Override
    public @NotNull FutureResult<@NotNull PlayerData> getPlayerById(@NotNull String id) {
        logger.info("Getting player by id {}", id);
        var player = playersById.get(id);
        if (player == null) {
            return FutureResult.error(ERR_NOT_FOUND);
        }
        return FutureResult.of(player);
    }

    @Override
    public @NotNull FutureResult<@NotNull PlayerData> getPlayerByUuid(@NotNull String uuid) {
        logger.info("Getting player by uuid {}", uuid);
        for (var entry : playersById.entrySet()) {
            if (entry.getValue().getId().equals(uuid)) {
                return FutureResult.of(entry.getValue());
            }
        }
        return FutureResult.error(ERR_NOT_FOUND);
    }

    @Override
    public @NotNull FutureResult<@NotNull Void> updatePlayer(@NotNull PlayerData player) {
        logger.info("Updating player {}", player.getId());
        if (!playersById.containsKey(player.getId()))
            return FutureResult.error(ERR_NOT_FOUND);
        playersById.put(player.getId(), player);
        return FutureResult.ofNull();
    }

    @Override
    public @NotNull FutureResult<Void> unlinkMap(@NotNull String mapId) {
        logger.info("Unlinking map {}", mapId);
        playersById.values().forEach(playerData -> {
            for (int i = 0; i < playerData.getUnlockedMapSlots(); i++) {
                if (mapId.equals(playerData.getMapSlot(i))) {
                    playerData.setMapSlot(i, null);
                }
            }
        });
        return FutureResult.ofNull();
    }
}
