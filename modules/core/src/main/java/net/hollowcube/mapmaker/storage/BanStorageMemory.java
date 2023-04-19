package net.hollowcube.mapmaker.storage;

import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class BanStorageMemory implements BanStorage {

    private final HashMap<UUID, String> bannedPlayers = new HashMap<>();
    // Makes removing a uuid by username easier
    private final HashMap<String, UUID> inverseMap = new HashMap<>();

    @Override
    public @NotNull Future<Boolean> isPlayerBanned(@NotNull Player player) {
        return CompletableFuture.completedFuture(bannedPlayers.containsKey(player.getUuid()));
    }

    @Override
    public @NotNull Future<Void> banPlayer(@NotNull Player player) {
        return CompletableFuture.runAsync(() -> {
            bannedPlayers.put(player.getUuid(), player.getUsername());
            inverseMap.put(player.getUsername(), player.getUuid());
        });
    }

    @Override
    public @NotNull Future<Void> unbanPlayer(@NotNull String username) {
        return CompletableFuture.runAsync(() -> {
            UUID id = inverseMap.remove(username);
            if (id != null) {
                bannedPlayers.remove(id);
            }
        });
    }

    @Override
    public @NotNull Future<Collection<String>> getUnbannedUsernames() {
        return CompletableFuture.completedFuture(bannedPlayers.values());
    }
}
