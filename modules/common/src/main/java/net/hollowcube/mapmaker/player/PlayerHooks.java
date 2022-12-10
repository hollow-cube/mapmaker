package net.hollowcube.mapmaker.player;

import net.hollowcube.mapmaker.model.PlayerData;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class PlayerHooks {
    private PlayerHooks() {}

    public static @NotNull String getId(@NotNull Player player) {
        return player.getTag(PlayerData.PLAYER_ID);
    }
}
