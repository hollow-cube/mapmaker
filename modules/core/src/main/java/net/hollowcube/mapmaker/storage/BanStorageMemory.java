package net.hollowcube.mapmaker.storage;

import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class BanStorageMemory implements BanStorage {

    private final HashSet<UUID> bannedPlayers = new HashSet<>();

    @Override
    public @NotNull Future<Boolean> isPlayerBanned(@NotNull Player player) {
        return CompletableFuture.completedFuture(bannedPlayers.contains(player.getUuid()));
    }

    @Override
    public @NotNull Future<Void> banPlayer(@NotNull Player player) {
        return CompletableFuture.runAsync(() -> bannedPlayers.add(player.getUuid()));
    }

    @Override
    public @NotNull Future<Void> unbanPlayer(@NotNull Player player) {
        return CompletableFuture.runAsync(() -> bannedPlayers.remove(player.getUuid()));
    }
}
