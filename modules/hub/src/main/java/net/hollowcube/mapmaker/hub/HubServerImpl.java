package net.hollowcube.mapmaker.hub;

import net.hollowcube.canvas.RouterSection;
import net.hollowcube.canvas.Section;
import net.hollowcube.mapmaker.hub.world.HubWorld;
import net.hollowcube.mapmaker.storage.MapStorage;
import net.hollowcube.mapmaker.storage.PlayerStorage;
import net.hollowcube.world.WorldManager;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class HubServerImpl implements HubServer {
    private final PlayerStorage players;
    private final MapStorage maps;

    private final HubWorld world;

    private boolean ready = false;

    public HubServerImpl(@NotNull PlayerStorage players, @NotNull MapStorage maps, @NotNull WorldManager worldManager) {
        this.players = players;
        this.maps = maps;

        this.world = new HubWorld(worldManager);
        world.loadWorld().thenRun(() -> this.ready = true);
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public @NotNull PlayerStorage players() {
        return players;
    }

    @Override
    public @NotNull MapStorage maps() {
        return maps;
    }

    @Override
    public @NotNull HubWorld world() {
        return world;
    }

    @Override
    public void openGUIForPlayer(@NotNull Player player, @NotNull Section gui) {
        new RouterSection(gui, Map.of(HubServer.class, this)).showToPlayer(player);
    }
}
