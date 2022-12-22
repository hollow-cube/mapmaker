package net.hollowcube.mapmaker.storage;

import net.hollowcube.mapmaker.model.SaveState;
import net.hollowcube.mapmaker.result.FutureResult;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SaveStateStorageMemory implements SaveStateStorage {
    private final Map<String, SaveState> saveStatesById = new ConcurrentHashMap<>();

    @Override
    public @NotNull FutureResult<@NotNull SaveState> createSaveState(@NotNull SaveState saveState) {
        if (saveStatesById.containsKey(saveState.getId()))
            return FutureResult.error(ERR_DUPLICATE_ENTRY);
        saveStatesById.put(saveState.getId(), saveState);
        return FutureResult.of(saveState);
    }

    @Override
    public @NotNull FutureResult<Void> updateSaveState(@NotNull SaveState saveState) {
        if (!saveStatesById.containsKey(saveState.getId()))
            return FutureResult.error(ERR_NOT_FOUND);
        saveStatesById.put(saveState.getId(), saveState);
        return FutureResult.ofNull();
    }

    @Override
    public @NotNull FutureResult<@NotNull SaveState> getLatestSaveState(@NotNull String playerId, @NotNull String mapId) {
        return saveStatesById.values().stream()
                .filter(saveState -> saveState.getPlayerId().equals(playerId) && saveState.getMapId().equals(mapId) && !saveState.isCompleted())
                .max(Comparator.comparing(SaveState::getStartTime))
                .map(FutureResult::of)
                .orElse(FutureResult.error(ERR_NOT_FOUND));
    }

}
