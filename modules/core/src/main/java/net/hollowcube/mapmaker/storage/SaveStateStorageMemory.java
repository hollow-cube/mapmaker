package net.hollowcube.mapmaker.storage;

import net.hollowcube.mapmaker.model.SaveState;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SaveStateStorageMemory implements SaveStateStorage {
    private final Map<String, SaveState> saveStatesById = new ConcurrentHashMap<>();

    @Override
    public @NotNull SaveState createSaveState(@NotNull SaveState saveState) {
        if (saveStatesById.containsKey(saveState.getId()))
            throw new NotFoundError();
        saveStatesById.put(saveState.getId(), saveState);
        return saveState;
    }

    @Override
    public void updateSaveState(@NotNull SaveState saveState) {
        if (!saveStatesById.containsKey(saveState.getId()))
            throw new NotFoundError();
        saveStatesById.put(saveState.getId(), saveState);
    }

    @Override
    public @NotNull SaveState getLatestSaveState(@NotNull String playerId, @NotNull String mapId, @NotNull SaveState.Type type) {
        return saveStatesById.values().stream()
                .filter(saveState -> saveState.getPlayerId().equals(playerId) &&
                        saveState.getMapId().equals(mapId) &&
                        saveState.getType() == type &&
                        !saveState.isCompleted())
                .max(Comparator.comparing(SaveState::getStartTime))
                .orElseThrow(NotFoundError::new);
    }

}
