package net.hollowcube.mapmaker.editor;

import net.hollowcube.compat.api.CompatProvider;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;

public class TestServer {

    public static void main(String[] args) {
        var server = MinecraftServer.init();

        CompatProvider.load(MinecraftServer.getGlobalEventHandler());

        var map = new MapData();
        var world = new EditorMapWorld2(null, map);

        MinecraftServer.getGlobalEventHandler()
                .addListener(AsyncPlayerConfigurationEvent.class, event -> {
                    world.configurePlayer(event);

                    final var player = event.getPlayer();
                    player.setTag(PlayerDataV2.TAG, new PlayerDataV2(player));
                })
                .addListener(PlayerSpawnEvent.class, event -> {
                    if (!event.isFirstSpawn()) return;
                    world.spawnPlayer(event.getPlayer());
                });

        MinecraftServer.getSchedulerManager()
                .scheduleEndOfTick(world::safePointTick);

        server.start("0.0.0.0", 25565);
    }

}
