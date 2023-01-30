package net.hollowcube.mapmaker.storage;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import net.hollowcube.mapmaker.model.SaveState;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SaveStateStorageMemory implements SaveStateStorage {
    private final Map<String, SaveState> saveStatesById = new ConcurrentHashMap<>();

    @Override
    public @NotNull ListenableFuture<@NotNull SaveState> createSaveState(@NotNull SaveState saveState) {
        if (saveStatesById.containsKey(saveState.getId()))
            return Futures.immediateFailedFuture(new NotFoundError());
        saveStatesById.put(saveState.getId(), saveState);
        return Futures.immediateFuture(saveState);
    }

    @Override
    public @NotNull ListenableFuture<Void> updateSaveState(@NotNull SaveState saveState) {
        if (!saveStatesById.containsKey(saveState.getId()))
            return Futures.immediateFailedFuture(new NotFoundError());
        saveStatesById.put(saveState.getId(), saveState);
        return Futures.immediateVoidFuture();
    }

    @Override
    public @NotNull ListenableFuture<@NotNull SaveState> getLatestSaveState(@NotNull String playerId, @NotNull String mapId) {
        return saveStatesById.values().stream()
                .filter(saveState -> saveState.getPlayerId().equals(playerId) && saveState.getMapId().equals(mapId) && !saveState.isCompleted())
                .max(Comparator.comparing(SaveState::getStartTime))
                .map(Futures::immediateFuture)
                .orElse(Futures.immediateFailedFuture(new NotFoundError()));
    }

}
