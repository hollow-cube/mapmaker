package net.hollowcube.mapmaker.player;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerServiceMemory implements PlayerService {
    private static final System.Logger logger = System.getLogger(PlayerServiceMemory.class.getSimpleName());

    final Map<String, PlayerDataV2> data = new ConcurrentHashMap<>();

    @Override
    public @NotNull Component getPlayerDisplayName(@NotNull String id) {
        var data = this.data.get(id);
        if (data == null)
            return Component.text("Unknown Player");
        return Component.text(data.username());
    }

    @Override
    public void updatePlayerData(@NotNull String id, @NotNull PlayerDataUpdateRequest update) {
        logger.log(System.Logger.Level.WARNING, "PlayerServiceMemory.updatePlayerData is a noop currently");
    }
}
