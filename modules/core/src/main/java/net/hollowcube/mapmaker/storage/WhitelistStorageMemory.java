package net.hollowcube.mapmaker.storage;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class WhitelistStorageMemory implements WhitelistStorage {

    private final HashSet<UUID> whitelistedIds = new HashSet<>();

    @Override
    public @NotNull Future<Boolean> isUUIDWhitelisted(@NotNull UUID uuid) {
        return CompletableFuture.completedFuture(whitelistedIds.contains(uuid));
    }

    @Override
    public @NotNull Future<Void> addToWhitelist(@NotNull UUID uuid) {
        return CompletableFuture.runAsync(() -> whitelistedIds.add(uuid));
    }

    @Override
    public @NotNull Future<Void> removeFromWhitelist(@NotNull UUID uuid) {
        return CompletableFuture.runAsync(() -> whitelistedIds.remove(uuid));
    }
}
