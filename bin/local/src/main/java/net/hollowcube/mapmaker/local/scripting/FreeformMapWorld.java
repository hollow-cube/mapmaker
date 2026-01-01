package net.hollowcube.mapmaker.local.scripting;

import net.hollowcube.mapmaker.editor.SubWorld;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.map.util.EventUtil;
import net.hollowcube.mapmaker.runtime.parkour.item.RateMapItem;
import net.hollowcube.mapmaker.scripting.WorldScriptContext;
import net.kyori.adventure.bossbar.BossBar;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

public class FreeformMapWorld extends AbstractMapWorld<FreeformState, FreeformMapWorld> implements SubWorld {

    public static @Nullable FreeformMapWorld forPlayer(Player player) {
        return MapWorld.forPlayer(FreeformMapWorld.class, player);
    }

    private final WorldScriptContext scriptContext;

    public FreeformMapWorld(MapServer server, MapData map, Path dataDirectory) {
        super(server, map, makeMapInstance(map, 'f'), FreeformState.class);

        eventNode()
            .addListener(PlayerTickEvent.class, this::handlePlayerTick)
            .addChild(EventUtil.READ_ONLY_NODE);

        this.scriptContext = new WorldScriptContext(this, dataDirectory.resolve("scripts"));
//        this.scriptContext.initializeWorld();
    }

    @Override
    protected FreeformState configurePlayer(Player player) {
        if (RateMapItem.isMapRatable(this)) {
            RateMapItem.initLastRating(server().mapService(), player, map());
        }

        player.setRespawnPoint(map().settings().getSpawnPoint());

        return new FreeformState.Building();
    }

    @Override
    public final void addPlayerDirect(Player player, Runnable callback) {
        var initialState = configurePlayer(player);
        player.setTag(PLAYER_INITIAL_STATE, initialState);

        // Global scheduler end of tick is the same as a safe point tick so this should be ok.
        MinecraftServer.getSchedulerManager().scheduleEndOfTick(() -> {
            spawnPlayer(player);
            callback.run();
        });
    }

    @Override
    public void spawnPlayer(Player player) {
        super.spawnPlayer(player);

        scriptContext.initializePlayer((MapPlayer) player);
    }

    @Override
    public void removePlayer(Player player) {
        super.removePlayer(player);

        scriptContext.destroyPlayer((MapPlayer) player);
    }

    private void handlePlayerTick(PlayerTickEvent event) {
        final var player = event.getPlayer();

        var minHeight = instance().getCachedDimensionType().minY() - 20;
        if (player.getPosition().y() < minHeight)
            player.teleport(map().settings().getSpawnPoint());
    }

    @Override
    protected @Nullable List<BossBar> createBossBars() {
        // TODO: only makes sense if this is being used as a test world, should split the test part of this off like is done with parkour
        return null; // Inherit from parent
    }
}
