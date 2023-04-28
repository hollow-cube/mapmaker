package net.hollowcube.mapmaker.storage;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class WhitelistStorageMemory implements WhitelistStorage {

    @Override
    public boolean isWhitelisted(@NotNull String playerId) {
        return true;
    }

    @Override
    public void addToWhitelist(@NotNull String playerId) {
        // memory whitelist always allows everyone
    }

    @Override
    public void removeFromWhitelist(@NotNull String playerId) {
        // memory whitelist always allows everyone
    }
}
