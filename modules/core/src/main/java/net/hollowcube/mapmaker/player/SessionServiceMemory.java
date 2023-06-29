package net.hollowcube.mapmaker.player;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public class SessionServiceMemory implements SessionService {
    private final PlayerServiceMemory playerService;

    public SessionServiceMemory(@NotNull PlayerServiceMemory playerService) {
        this.playerService = playerService;
    }

    @Override
    public @NotNull PlayerDataV2 createSession(@NotNull String id, @NotNull String username, @NotNull String ip) {
        var data = playerService.data.get(id);
        if (data != null) return data;

        data = new PlayerDataV2(id, username, Component.text(username));
        playerService.data.put(id, data);
        return data;
    }

    @Override
    public void deleteSession(@NotNull String id) {
        // does nothing
    }
}
