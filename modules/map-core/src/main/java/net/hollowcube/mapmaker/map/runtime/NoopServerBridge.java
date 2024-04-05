package net.hollowcube.mapmaker.map.runtime;

import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class NoopServerBridge implements ServerBridge {
    @Override
    public void joinMap(@NotNull Player player, @NotNull String mapId, @NotNull JoinMapState joinMapState, @NotNull String source) {
        player.sendMessage("joinMap not implemented in noop bridge");
    }

    @Override
    public void joinHub(@NotNull Player player) {
        player.sendMessage("joinHub not implemented in noop bridge");
    }
}
