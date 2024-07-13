package net.hollowcube.mapmaker.local.svc;

import net.hollowcube.mapmaker.map.runtime.NoopServerBridge;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LocalServerBridge extends NoopServerBridge {

    @Override
    public void joinHub(@NotNull Player player) {
        player.kick("Exit to 'hub' :)");
    }
}
