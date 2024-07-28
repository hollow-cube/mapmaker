package net.hollowcube.mapmaker.local.svc;

import net.hollowcube.mapmaker.local.LocalServerRunner;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.runtime.NoopServerBridge;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

public class LocalServerBridge extends NoopServerBridge {
    public static final Tag<String> TARGET_SERVER_TAG = Tag.Transient("target-server");

    private final LocalServerRunner server;

    public LocalServerBridge(LocalServerRunner server) {
        this.server = server;
    }

    @Override
    public void joinHub(@NotNull Player player) {
        player.kick("Exit to 'hub' :)");
    }

    @Override
    public void joinMap(@NotNull Player player, @NotNull String mapId, @NotNull JoinMapState joinMapState, @NotNull String source) {
        var world = MapWorld.forPlayerOptional(player);
        if (world != null) {
            world.removePlayer(player);
        }

        player.setTag(TARGET_SERVER_TAG, mapId);
        player.startConfigurationPhase();
    }
}
