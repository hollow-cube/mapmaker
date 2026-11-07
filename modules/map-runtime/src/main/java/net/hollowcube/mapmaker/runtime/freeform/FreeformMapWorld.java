package net.hollowcube.mapmaker.runtime.freeform;

import net.hollowcube.common.hud.PlayerHud;
import net.hollowcube.ipc.map.MapData;
import net.hollowcube.mapmaker.map.AbstractMapWorld;
import net.hollowcube.mapmaker.map.MapServer;
import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.runtime.MapWorlds;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/// A played map that isn't a parkour course: no save state, no runs, nothing to complete.
public class FreeformMapWorld extends AbstractMapWorld<FreeformState, FreeformMapWorld> {

    public static @Nullable FreeformMapWorld forPlayer(Player player) {
        return MapWorld.forPlayer(FreeformMapWorld.class, player);
    }

    private final FreeformRuntime runtime;

    public FreeformMapWorld(
        MapServer server,
        MapData map,
        Function<FreeformMapWorld, FreeformRuntime> runtime
    ) {
        super(server, map, makeMapInstance(map, 'f'), FreeformState.class);
        MapWorlds.checkInitialized();

        eventNode().addListener(PlayerTickEvent.class, this::handlePlayerTick);

        this.runtime = runtime.apply(this);
    }

    public FreeformRuntime runtime() {
        return runtime;
    }

    @Override
    protected FreeformState configurePlayer(Player player) {
        runtime.configurePlayer(this, player);
        return new FreeformState.Playing();
    }

    @Override
    protected void loadWorldData() {
        super.loadWorldData();
        runtime.load(this);
    }

    private void handlePlayerTick(PlayerTickEvent event) {
        final var player = event.getPlayer();

        var minHeight = instance().getCachedDimensionType().minY() - 20;
        if (player.getPosition().y() < minHeight)
            player.teleport(MapSettings.getSpawnPoint(map().settings()));
    }

    @Override
    protected PlayerHud.@Nullable Module createTitleHud() {
        return runtime.titleHud(this);
    }
}
