package net.hollowcube.mapmaker.storage;

import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class MuteStorageMemory implements MuteStorage {

    private final HashSet<UUID> mutedIds = new HashSet<>();

    @Override
    public @NotNull Future<Boolean> isPlayerMuted(@NotNull Player player) {
        return CompletableFuture.completedFuture(mutedIds.contains(player.getUuid()));
    }

    @Override
    public @NotNull Future<Void> mutePlayer(@NotNull Player player) {
        return CompletableFuture.runAsync(() -> mutedIds.add(player.getUuid()));
    }

    @Override
    public @NotNull Future<Void> ummutePlayer(@NotNull Player player) {
        return CompletableFuture.runAsync(() -> mutedIds.remove(player.getUuid()));
    }
}
