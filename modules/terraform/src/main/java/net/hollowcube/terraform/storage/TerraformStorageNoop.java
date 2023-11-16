package net.hollowcube.terraform.storage;

import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TerraformStorageNoop implements TerraformStorage {
    @Override
    public byte @Nullable [] loadPlayerSession(@NotNull Player player, @NotNull String playerId) {
        return null;
    }

    @Override
    public void savePlayerSession(byte @NotNull [] session) {

    }

    @Override
    public byte @Nullable [] loadLocalSession(@NotNull Player player, @NotNull String playerId, @NotNull String instanceId) {
        return null;
    }

    @Override
    public void saveLocalSession(byte @NotNull [] session) {

    }
}
