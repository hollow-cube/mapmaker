package net.hollowcube.terraform.storage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TerraformStorageMemory implements TerraformStorage {
    private final Map<String, byte[]> sessionData = new ConcurrentHashMap<>();
    private final Map<String, byte[]> localSessionData = new ConcurrentHashMap<>();

    @Override
    public byte @Nullable [] loadPlayerSession(@NotNull String playerId) {
        return sessionData.get(playerId);
    }

    @Override
    public void savePlayerSession(@NotNull String playerId, byte @NotNull [] session) {
        sessionData.put(playerId, session);
    }

    @Override
    public byte @Nullable [] loadLocalSession(@NotNull String playerId, @NotNull String instanceId) {
        return localSessionData.get(String.format("%s:%s", playerId, instanceId));
    }

    @Override
    public void saveLocalSession(@NotNull String playerId, @NotNull String instanceId, byte @NotNull [] session) {
        localSessionData.put(String.format("%s:%s", playerId, instanceId), session);
    }
}
